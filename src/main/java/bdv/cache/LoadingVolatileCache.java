/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.cache;

import bdv.cache.CacheIoTiming.IoStatistics;
import bdv.cache.CacheIoTiming.IoTimeBudget;
import bdv.cache.util.BlockingFetchQueues;
import bdv.cache.util.FetcherThreads;
import bdv.cache.util.FetcherThreads.Loader;
import bdv.img.cache.VolatileGlobalCellCache;


/**
 * A loading cache mapping keys to {@link VolatileCacheValue}s. The cache spawns
 * a set of {@link FetcherThreads} that asynchronously load data for cached
 * values.
 * <p>
 * Using {@link #get(Object, CacheHints, VolatileCacheValueLoader)}, a key is
 * added to the cache, specifying a {@link VolatileCacheValueLoader} to provide
 * the value for the key. After adding the key to the cache, it is immediately
 * associated with a value. However, that value may be initially
 * {@link VolatileCacheValue#isValid() invalid}. When the value is made valid
 * (loaded) depends on the provided {@link CacheHints}, specifically the
 * {@link CacheHints#getLoadingStrategy() loading strategy}. The strategy may be
 * to load the value immediately, to load it immediately if there is enough IO
 * budget left, to enqueue it for asynchronous loading, or to not load it at
 * all.
 * <p>
 * Using {@link #getIfPresent(Object, CacheHints)} a value for the specified key
 * is returned if the key is in the cache (otherwise {@code null} is returned).
 * Again, the returned value may be invalid, and when the value is loaded
 * depends on the provided {@link CacheHints}.
 *
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public final class LoadingVolatileCache< K, V extends VolatileCacheValue > implements CacheControl
{
	private final WeakSoftCache< K, Entry > cache = WeakSoftCache.newInstance();

	private final Object cacheLock = new Object();

	private final CacheIoTiming cacheIoTiming = new CacheIoTiming();

	private final int numPriorityLevels;

	private final BlockingFetchQueues< K > queue;

	private final FetcherThreads< K > fetchers;

	private volatile long currentQueueFrame = 0;

	/**
	 * Create a new {@link LoadingVolatileCache} with the specified number of
	 * priority levels and number of {@link FetcherThreads} for asynchronous
	 * loading of cache entries.
	 *
	 * @param numPriorityLevels
	 *            the number of priority levels (see {@link CacheHints}).
	 * @param numFetcherThreads
	 *            the number of threads to create for asynchronous loading of
	 *            cache entries.
	 */
	public LoadingVolatileCache( final int numPriorityLevels, final int numFetcherThreads )
	{
		this.numPriorityLevels = numPriorityLevels;
		queue = new BlockingFetchQueues<>( numPriorityLevels );
		fetchers = new FetcherThreads<>( queue, new EntryLoader(), numFetcherThreads );
	}

	/**
	 * Get the value for the specified key if the key is in the cache (otherwise
	 * return {@code null}). Note, that a value being in the cache only means
	 * that there is data, but not necessarily that the data is
	 * {@link VolatileCacheValue#isValid() valid}.
	 * <p>
	 * If the value is present but not valid, do the following, depending on the
	 * {@link LoadingStrategy}:
	 * <ul>
	 * <li>{@link LoadingStrategy#VOLATILE}: Enqueue the key for asynchronous
	 * loading by a fetcher thread, if it has not been enqueued in the current
	 * frame already.
	 * <li>{@link LoadingStrategy#BLOCKING}: Load the data immediately.
	 * <li>{@link LoadingStrategy#BUDGETED}: Load the data immediately if there
	 * is enough {@link IoTimeBudget} left for the current thread group.
	 * Otherwise enqueue for asynchronous loading, if it has not been enqueued
	 * in the current frame already.
	 * <li>{@link LoadingStrategy#DONTLOAD}: Do nothing.
	 * </ul>
	 *
	 * @param key
	 *            the key to query.
	 * @param cacheHints
	 *            {@link LoadingStrategy}, queue priority, and queue order.
	 * @return the value with the specified key in the cache or {@code null}.
	 */
	public V getIfPresent( final K key, final CacheHints cacheHints )
	{
		final Entry entry = cache.get( key );
		if ( entry != null )
		{
			if ( !entry.getValue().isValid() )
				loadEntryWithCacheHints( entry, cacheHints );

			return entry.getValue();
		}
		return null;
	}

	/**
	 * Add a new key to the cache, unless it is already present. If the key is
	 * new, after adding the key to the cache, it is immediately associated with
	 * a value. However, that value may be initially
	 * {@link VolatileCacheValue#isValid() invalid}. When the value is made
	 * valid (loaded) depends on the provided {@link CacheHints}, specifically
	 * the {@link CacheHints#getLoadingStrategy() loading strategy}. The
	 * strategy may be to load the value immediately, to load it immediately if
	 * there is enough IO budget left, to enqueue it for asynchronous loading,
	 * or to not load it at all.
	 * <p>
	 * The {@code cacheLoader} will be used both to provide the initial
	 * (invalid) value and to load the (valid) value.
	 * <p>
	 * Whether the key was already present in the cache or not, if the value is
	 * not valid do the following, depending on the {@link LoadingStrategy}:
	 * <ul>
	 * <li>{@link LoadingStrategy#VOLATILE}: Enqueue the key for asynchronous
	 * loading by a fetcher thread, if it has not been enqueued in the current
	 * frame already.
	 * <li>{@link LoadingStrategy#BLOCKING}: Load the data immediately.
	 * <li>{@link LoadingStrategy#BUDGETED}: Load the data immediately if there
	 * is enough {@link IoTimeBudget} left for the current thread group.
	 * Otherwise enqueue for asynchronous loading, if it has not been enqueued
	 * in the current frame already.
	 * <li>{@link LoadingStrategy#DONTLOAD}: Do nothing.
	 * </ul>
	 *
	 * @param key
	 *            the key to query.
	 * @param cacheHints
	 *            {@link LoadingStrategy}, queue priority, and queue order.
	 * @param cacheLoader
	 * @return the value with the specified key in the cache.
	 */
	public V get( final K key, final CacheHints cacheHints, final VolatileCacheValueLoader< ? extends V > cacheLoader )
	{
		Entry entry = null;

		synchronized ( cacheLock )
		{
			entry = cache.get( key );
			if ( entry == null )
			{
				entry = new Entry( key, cacheLoader );
				cache.putWeak( key, entry );
			}
		}

		if ( !entry.getValue().isValid() )
			loadEntryWithCacheHints( entry, cacheHints );

		return entry.getValue();
	}

	/**
	 * Prepare the cache for providing data for the "next frame":
	 * <ul>
	 * <li>Move pending cell request to the prefetch queue (
	 * {@link BlockingFetchQueues#clearToPrefetch()}).
	 * <li>Perform pending cache maintenance operations (
	 * {@link WeakSoftCache#cleanUp()}).
	 * <li>Increment the internal frame counter, which will enable previously
	 * enqueued requests to be enqueued again for the new frame.
	 * </ul>
	 */
	@Override
	public void prepareNextFrame()
	{
		queue.clearToPrefetch();
		cache.cleanUp();
		++currentQueueFrame;
	}

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO per frame/
	 *
	 * @param partialBudget
	 *            Initial budget (in nanoseconds) for priority levels 0 through
	 *            <em>n</em>. The budget for level <em>i &gt; j</em> must always
	 *            be smaller-equal the budget for level <em>j</em>. If
	 *            <em>n</em> is smaller than the number of priority levels, the
	 *            remaining priority levels are filled up with @code{budget[n]}.
	 */
	@Override
	public void initIoTimeBudget( final long[] partialBudget )
	{
		final IoStatistics stats = cacheIoTiming.getThreadGroupIoStatistics();
		if ( stats.getIoTimeBudget() == null )
			stats.setIoTimeBudget( new IoTimeBudget( numPriorityLevels ) );
		stats.getIoTimeBudget().reset( partialBudget );
	}

	/**
	 * Get the {@link CacheIoTiming} that provides per thread-group IO
	 * statistics and budget.
	 */
	@Override
	public CacheIoTiming getCacheIoTiming()
	{
		return cacheIoTiming;
	}

	/**
	 * Remove all references to loaded data as well as all enqueued requests
	 * from the cache.
	 */
	public void invalidateAll()
	{
		queue.clear();
		cache.invalidateAll();
		prepareNextFrame();
	}

	public FetcherThreads< K > getFetcherThreads()
	{
		return fetchers;
	}

	// ================ private methods =====================

	/**
	 * Load or enqueue the specified {@link Entry}, depending on the
	 * {@link LoadingStrategy} given in {@code cacheHints}.
	 */
	private void loadEntryWithCacheHints( final Entry entry, final CacheHints cacheHints )
	{
		switch ( cacheHints.getLoadingStrategy() )
		{
		case VOLATILE:
		default:
			enqueueEntry( entry, cacheHints.getQueuePriority(), cacheHints.isEnqueuToFront() );
			break;
		case BLOCKING:
			while ( true )
				try
				{
					entry.loadIfNotValid();
					break;
				}
				catch ( final InterruptedException e )
				{}
			break;
		case BUDGETED:
			loadOrEnqueue( entry, cacheHints.getQueuePriority(), cacheHints.isEnqueuToFront() );
			break;
		case DONTLOAD:
			break;
		}
	}

	/**
	 * Enqueue the {@link Entry} if it hasn't been enqueued for this frame
	 * already.
	 */
	private void enqueueEntry( final Entry entry, final int priority, final boolean enqueuToFront )
	{
		if ( entry.getEnqueueFrame() < currentQueueFrame )
		{
			entry.setEnqueueFrame( currentQueueFrame );
			queue.put( entry.getKey(), priority, enqueuToFront );
		}
	}

	/**
	 * Load the data for the {@link Entry} if it is not yet loaded (valid) and
	 * there is enough {@link IoTimeBudget} left. Otherwise, enqueue the
	 * {@link Entry} if it hasn't been enqueued for this frame already.
	 */
	private void loadOrEnqueue( final Entry entry, final int priority, final boolean enqueuToFront )
	{
		final IoStatistics stats = cacheIoTiming.getThreadGroupIoStatistics();
		final IoTimeBudget budget = stats.getIoTimeBudget();
		final long timeLeft = budget.timeLeft( priority );
		if ( timeLeft > 0 )
		{
			synchronized ( entry )
			{
				if ( entry.getValue().isValid() )
					return;
				enqueueEntry( entry, priority, enqueuToFront );
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
			enqueueEntry( entry, priority, enqueuToFront );
	}

	/**
	 * A {@link Loader} that is used by fetcher threads to load values. Loading
	 * value associated with a specific key is achieved by trying to get the
	 * corresponding {@link Entry} from the {@link WeakSoftCache}, and and
	 * forwarding to {@link Entry#loadIfNotValid()}.
	 */
	final class EntryLoader implements Loader< K >
	{
		/**
		 * If this key's data is not yet valid, then load it. After the method
		 * returns, the data is guaranteed to be valid.
		 *
		 * @throws InterruptedException
		 *             if the loading operation was interrupted.
		 */
		@Override
		public void load( final K key ) throws InterruptedException
		{
			final Entry entry = cache.get( key );
			if ( entry != null )
				entry.loadIfNotValid();
		}
	}

	/**
	 * This is the value type of the underlying {@link WeakSoftCache}.
	 * <p>
	 * Besides the current value (of type {@code V extends}
	 * {@link VolatileCacheValue}), the {@link Entry} keeps information related
	 * to queuing and loading the value if it is invalid.
	 * <p>
	 * The {@code key} and a {@code loader} are both required to implement
	 * {@link #loadIfNotValid}.
	 * <p>
	 * {@code enqueueFrame} keeps track of when the entry was last added to
	 * fetch queue. This is used to prevent entries from being enqueued more
	 * than once per frame.
	 */
	final class Entry
	{
		private final K key;

		private V value;

		private final VolatileCacheValueLoader< ? extends V > loader;

		/**
		 * When was this entry last enqueued for loading (see
		 * {@link VolatileGlobalCellCache#currentQueueFrame}). This is initialized
		 * to -1. When the entry's data becomes valid, it is set to
		 * {@link Long#MAX_VALUE}.
		 */
		private long enqueueFrame;

		/**
		 * Create a new cache entry with the given key. The value is initialized
		 * using {@link VolatileCacheValueLoader#createEmptyValue(Object)}. The
		 * {@link #getEnqueueFrame() enqueue frame} is initialized to {@code -1}
		 * indicating that the entry has never been enqueued for fetching.
		 *
		 * @param key
		 *            the key (needed for loading the data if it is not valid).
		 * @param loader
		 *            for {@link VolatileCacheValueLoader#load(Object) loading}
		 *            valid data given the key.
		 */
		public Entry( final K key, final VolatileCacheValueLoader< ? extends V > loader )
		{
			this.key = key;
			this.value = loader.createEmptyValue();
			this.loader = loader;
			enqueueFrame = -1;
		}

		/**
		 * If the {@code value} is not currently
		 * {@link VolatileCacheValue#isValid() valid}, then load it. After the
		 * method returns, the value is guaranteed to be valid.
		 * <p>
		 * This must be implemented in a thread-safe manner. Multiple threads
		 * are allowed to call this method at the same time. The intended
		 * behaviour is that the value is loaded only once and the result is
		 * visible on all threads.
		 *
		 * @throws InterruptedException
		 *             if the loading operation was interrupted.
		 */
		public void loadIfNotValid() throws InterruptedException
		{
			/*
			 * TODO: the assumption for following synchronisation pattern is
			 * that value.isValid() will never go from true to false. When
			 * invalidation API is added, that might change.
			 */
			if ( !value.isValid() )
			{
				synchronized ( this )
				{
					if ( !value.isValid() )
					{
						value = loader.load();
						enqueueFrame = Long.MAX_VALUE;
						cache.putSoft( key, this );
						notifyAll();
					}
				}
			}
		}

		public K getKey()
		{
			return key;
		}

		public V getValue()
		{
			return value;
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
}
