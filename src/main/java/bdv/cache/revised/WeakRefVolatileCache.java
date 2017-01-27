package bdv.cache.revised;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.cache.VolatileCacheValue;
import bdv.cache.iotiming.CacheIoTiming;
import bdv.cache.iotiming.IoStatistics;
import bdv.cache.iotiming.IoTimeBudget;
import bdv.cache.util.BlockingFetchQueues;

public class WeakRefVolatileCache< K, V extends VolatileCacheValue > implements VolatileCache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final Cache< K, V > backingCache;

	private final BlockingFetchQueues< Callable< ? > > fetchQueue;

	public WeakRefVolatileCache(
			final Cache< K, V > backingCache,
			final BlockingFetchQueues< Callable< ? > > fetchQueue )
	{
		this.fetchQueue = fetchQueue;
		this.backingCache = backingCache;
	}

	final class CacheWeakReference extends WeakReference< V >
	{
		private final Entry entry;

		public CacheWeakReference( final V referent, final Entry entry )
		{
			super( referent, queue );
			this.entry = entry;
		}

		public void clean()
		{
			map.remove( entry.getKey(), entry );
		}
	}

	final class Entry
	{
		private final K key;

		private WeakReference< V > ref;

		private long enqueueFrame;

		private VolatileLoader< ? extends V > loader;

		public Entry( final K key, final V value )
		{
			assert( value.isValid() );
			this.key = key;
			this.loader = null;
			ref = new CacheWeakReference( value, this );
			enqueueFrame = -1;
		}

		public Entry( final K key, final VolatileLoader< ? extends V > loader )
		{
			this.key = key;
			this.loader = loader;
			ref = new CacheWeakReference( loader.createInvalid(), this );
			enqueueFrame = -1;
		}

		public K getKey()
		{
			return key;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void load() throws ExecutionException
		{
			/*
			 * TODO: the assumption for following synchronization pattern is
			 * that value.isValid() will never go from true to false. When
			 * invalidation API is added, that might change.
			 */
			final V oldValue = ref.get();
			if ( oldValue != null && !oldValue.isValid() )
			{
				synchronized ( this )
				{
					V value = ref.get();
					if ( value == oldValue )
					{
						value = backingCache.get( key, loader );
						ref = new CacheWeakReference( value, this );
						loader = null;
						enqueueFrame = Long.MAX_VALUE;
						notifyAll();
					}
				}
			}
		}

		public long getEnqueueFrame()
		{
			return enqueueFrame;
		}

		public void setEnqueueFrame( final long f )
		{
			enqueueFrame = f;
		}
	}

	@Override
	public V getIfPresent( final Object key, final CacheHints cacheHints ) throws ExecutionException
	{
		final Entry entry = map.get( key );
		if ( entry == null )
			return null;

		final V value = entry.getValue();
		if ( value == null )
			return null;

		if ( !value.isValid() )
			loadEntryWithCacheHints( entry, cacheHints );

		return entry.getValue();
	}

	@Override
	public V get( final K key, final VolatileLoader< ? extends V > loader, final CacheHints cacheHints ) throws ExecutionException
	{
		/*
		 * Get existing entry for key or create it with an empty (invalid)
		 * value.
		 */
		final Entry entry = map.computeIfAbsent( key,
				( k ) -> {
					final V value = backingCache.getIfPresent( k );
					if ( value == null )
						return new Entry( k, value );
					else
						return new Entry( k, loader );
				} );

		V value = entry.getValue();
		if ( value == null )
		{
			/*
			 * The value has been garbage collected. We need to create a new
			 * entry.
			 */
			map.remove( key, entry );
			return get( key, loader, cacheHints );
		}

		/*
		 * Entry and value exist. If the value is invalid, try to load it. While
		 * we do that, the value cannot be garbage collected because we hold a
		 * strong reference and consequently the entry cannot be removed from the
		 * map.
		 */
		if ( !value.isValid() )
		{
			loadEntryWithCacheHints( entry, cacheHints );
			value = entry.getValue();
			cleanUp( 50 );
		}

		return value;
	}

	/**
	 * Remove references from the cache that have been garbage-collected. To
	 * avoid long run-times at most {@code maxElements} are processed.
	 *
	 * @param maxElements
	 *            how many references to clean up at most.
	 * @return how many references were actually cleaned up.
	 */
	@SuppressWarnings( "unchecked" )
	public int cleanUp( final int maxElements )
	{
		int i = 0;
		for ( ; i < maxElements; ++i )
		{
			final Reference< ? > poll = queue.poll();
			if ( poll == null )
				break;
			( ( CacheWeakReference ) poll ).clean();
		}
		return i;
	}

	@Override
	public void invalidateAll()
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}





	// ================ private methods =====================

	/**
	 * Load or enqueue the specified {@link Entry}, depending on the
	 * {@link LoadingStrategy} given in {@code cacheHints}.
	 *
	 * @param entry
	 * @param currentValue
	 * @param cacheHints
	 * @throws ExecutionException
	 */
	private void loadEntryWithCacheHints( final Entry entry, final CacheHints cacheHints ) throws ExecutionException
	{
		switch ( cacheHints.getLoadingStrategy() )
		{
		case VOLATILE:
		default:
			enqueueEntry( entry, cacheHints );
			break;
		case BLOCKING:
			entry.load();
			break;
		case BUDGETED:
			loadOrEnqueue( entry, cacheHints );
			break;
		case DONTLOAD:
			break;
		}
	}

	/**
	 * Load the data for the {@link Entry} if it is not yet loaded (valid) and
	 * there is enough {@link IoTimeBudget} left. Otherwise, enqueue the
	 * {@link Entry} if it hasn't been enqueued for this frame already.
	 */
	private void loadOrEnqueue( final Entry entry, final CacheHints cacheHints )
	{
		final int priority = cacheHints.getQueuePriority();
		final IoStatistics stats = CacheIoTiming.getIoStatistics();
		final IoTimeBudget budget = stats.getIoTimeBudget();
		final long timeLeft = budget.timeLeft( priority );
		if ( timeLeft > 0 )
		{
			synchronized ( entry )
			{
				if ( entry.getValue().isValid() )
					return;
				enqueueEntry( entry, cacheHints );
				final long t0 = stats.getIoNanoTime();
				stats.start();
				try
				{
					entry.wait( timeLeft  / 1000000l, 1 );
				}
				catch ( final InterruptedException e )
				{}
				stats.stop();
				final long t = stats.getIoNanoTime() - t0;
				budget.use( t, priority );
			}
		}
		else
			enqueueEntry( entry, cacheHints );
	}

	/**
	 * Enqueue the {@link Entry} if it hasn't been enqueued for this frame
	 * already.
	 */
	private void enqueueEntry( final Entry entry, final CacheHints cacheHints )
	{
		final long currentQueueFrame = fetchQueue.getCurrentFrame();
		if ( entry.getEnqueueFrame() < currentQueueFrame )
		{
			entry.setEnqueueFrame( currentQueueFrame );
			fetchQueue.put( new FetchEntry( entry.getKey() ), cacheHints.getQueuePriority(), cacheHints.isEnqueuToFront() );
		}
	}

	/**
	 * {@link Callable} to put into the fetch queue. Loads data for a specific key.
	 */
	final class FetchEntry implements Callable< Void >
	{
		private final K key;

		public FetchEntry( final K key )
		{
			this.key = key;
		}

		/**
		 * If this key's data is not yet valid, then load it. After the method
		 * returns, the data is guaranteed to be valid.
		 *
		 * @throws InterruptedException
		 *             if the loading operation was interrupted.
		 */
		@Override
		public Void call() throws Exception
		{
			final Entry entry = map.get( key );
			if ( entry != null )
				entry.load();
			return null;
		}
	}
}
