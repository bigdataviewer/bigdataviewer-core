package bdv.cache.revised;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import bdv.cache.CacheHints;
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
			entry.clean( this );
		}
	}

	static final int NOTLOADED = 0;
	static final int INVALID = 1;
	static final int VALID = 2;

	final class Entry
	{
		private WeakReference< V > ref;

		final K key;

		int loaded;

		long enqueueFrame;

		VolatileLoader< ? extends V > loader;

		public Entry( final K key, final VolatileLoader< ? extends V > loader )
		{
			this.key = key;
			this.loader = loader;
			this.ref = new WeakReference<>( null );
			this.loaded = NOTLOADED;
			this.enqueueFrame = -1;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setInvalid( final V value )
		{
			loaded = INVALID;
			ref = new CacheWeakReference( value, this );
		}

		public void setValid( final V value )
		{
			loaded = VALID;
			ref = new CacheWeakReference( value, this );
			loader = null;
			enqueueFrame = Long.MAX_VALUE;
		}

		public void clean( final CacheWeakReference ref )
		{
			if ( ref == this.ref )
				map.remove( key, this );
		}
	}

	@Override
	public V getIfPresent( final Object key, final CacheHints hints ) throws ExecutionException
	{
		final Entry entry = map.get( key );
		if ( entry == null )
			return null;

		final V v = entry.getValue();
		if ( v != null && v.isValid() )
			return v;

		switch ( hints.getLoadingStrategy() )
		{
		case BLOCKING:
			return getBlocking( entry );
		case BUDGETED:
			final int priority = hints.getQueuePriority();
			final IoStatistics stats = CacheIoTiming.getIoStatistics();
			final IoTimeBudget budget = stats.getIoTimeBudget();
			final long timeLeft = budget.timeLeft( priority );
			if ( timeLeft > 0 )
				return getBudgeted( entry, hints );
		case VOLATILE:
			enqueue( entry, hints );
		case DONTLOAD:
		default:
			return v;
		}
	}

	@Override
	public V get( final K key, final VolatileLoader< ? extends V > loader, final CacheHints hints ) throws ExecutionException
	{
		/*
		 * Get existing entry for key or create it.
		 */
		final Entry entry = map.computeIfAbsent( key, k -> new Entry( key, loader ) );

		V v = entry.getValue();
		if ( v != null && v.isValid() )
			return v;

		switch ( hints.getLoadingStrategy() )
		{
		case BLOCKING:
			v = getBlocking( entry );
			break;
		case BUDGETED:
			v = getBudgeted( entry, hints );
			break;
		case VOLATILE:
			v = getVolatile( entry, hints );
			break;
		case DONTLOAD:
			v = getDontLoad( entry );
			break;
		}

		cleanUp( 10 );

		if ( v == null )
			return get( key, loader, hints );
		else
			return v;
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
			load( key );
			return null;
		}
	}

	void load( final K key ) throws ExecutionException
	{
		final Entry entry = map.get( key );
		if ( entry != null )
			getBlocking( entry );
	}

	// ================ private methods =====================

	private V getDontLoad( final Entry entry )
	{
		synchronized( entry )
		{
			V v = entry.getValue();
			if ( v != null )
				return v;

			if ( entry.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			final V vl = backingCache.getIfPresent( entry.key );
			if ( vl != null )
			{
				entry.setValid( vl );
				return vl;
			}

			v = entry.loader.createInvalid();
			entry.setInvalid( v );
			return v;
		}
	}

	private V getVolatile( final Entry entry, final CacheHints hints )
	{
		synchronized( entry )
		{
			V v = entry.getValue();
			if ( v == null && entry.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			if ( entry.loaded == VALID ) // v.isValid()
				return v;

			final V vl = backingCache.getIfPresent( entry.key );
			if ( vl != null )
			{
				entry.setValid( vl );
				return vl;
			}

			if ( entry.loaded == NOTLOADED )
			{
				v = entry.loader.createInvalid();
				entry.setInvalid( v );
			}

			enqueue( entry, hints );
			return v;
		}
	}

	private V getBudgeted( final Entry entry, final CacheHints hints )
	{
		synchronized( entry )
		{
			V v = entry.getValue();
			if ( v == null && entry.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			if ( entry.loaded == VALID ) // v.isValid()
				return v;

			enqueue( entry, hints );

			final int priority = hints.getQueuePriority();
			final IoStatistics stats = CacheIoTiming.getIoStatistics();
			final IoTimeBudget budget = stats.getIoTimeBudget();
			final long timeLeft = budget.timeLeft( priority );
			if ( timeLeft > 0 )
			{
				final long t0 = stats.getIoNanoTime();
				stats.start();
				try
				{
					entry.wait( timeLeft  / 1000000l, 1 );
					// releases and re-acquires entry lock
				}
				catch ( final InterruptedException e )
				{}
				stats.stop();
				final long t = stats.getIoNanoTime() - t0;
				budget.use( t, priority );
			}

			v = entry.getValue();
			if ( v == null )
			{
				if ( entry.loaded == NOTLOADED )
				{
					v = entry.loader.createInvalid();
					entry.setInvalid( v );
					return v;
				}
				else
				{
					map.remove( entry.key, entry );
					return null;
				}
			}
			return v;
		}
	}

	private V getBlocking( final Entry entry ) throws ExecutionException
	{
		VolatileLoader< ? extends V > loader;
		synchronized( entry )
		{
			final V v = entry.getValue();
			if ( v == null && entry.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			if ( entry.loaded == VALID ) // v.isValid()
				return v;

			loader = entry.loader;
		}
		final V vl = backingCache.get( entry.key, loader );
		synchronized( entry )
		{
			final V v = entry.getValue();
			if ( v == null && entry.loaded != NOTLOADED )
			{
				map.remove( entry.key, entry );
				return null;
			}

			if ( entry.loaded == VALID ) // v.isValid()
				return v;

			// entry.loaded == INVALID
			entry.setValid( vl );
			return vl;
		}
	}

	/**
	 * Enqueue the {@link Entry} if it hasn't been enqueued for this frame
	 * already.
	 */
	private void enqueue( final Entry entry, final CacheHints hints )
	{
		final long currentQueueFrame = fetchQueue.getCurrentFrame();
		if ( entry.enqueueFrame < currentQueueFrame )
		{
			entry.enqueueFrame = currentQueueFrame;
			fetchQueue.put( new FetchEntry( entry.key ), hints.getQueuePriority(), hints.isEnqueuToFront() );
		}
	}
}
