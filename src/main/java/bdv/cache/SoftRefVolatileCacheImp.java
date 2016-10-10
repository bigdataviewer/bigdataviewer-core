package bdv.cache;

import bdv.img.cache.VolatileGlobalCellCache;

public class SoftRefVolatileCacheImp implements VolatileCache
{
	// TODO: this should be a singleton, but for now we create new instances, because bdv cache keys don't identify the viewer (yet).
	public static VolatileCache getInstance()
	{
		return new SoftRefVolatileCacheImp();
	}

	@Override
	public < K, V extends VolatileCacheValue >
		VolatileCacheEntry< K, V > put( final K key, final V value, final VolatileCacheValueLoader< K, V > loader )
	{
		final Entry< K, V > entry = new Entry<>( key, value, loader );
		if ( value.isValid() )
			cache.putSoft( key, entry );
		else
			cache.putWeak( key, entry );
		return entry;
	}

	@Override
	public < K, V extends VolatileCacheValue >
		VolatileCacheEntry< K, V > get( final K key )
	{
		return cache.get( key );
	}

	@Override
	public void clearCache()
	{
		cache.clearCache();
	}


	@Override
	public void finalizeRemovedCacheEntries()
	{
		cache.finalizeRemovedCacheEntries();
	}

	class Entry< K, V extends VolatileCacheValue > implements VolatileCacheEntry< K, V >
	{
		private final K key;

		private V value;

		private final VolatileCacheValueLoader< K, V > loader;

		/**
		 * When was this entry last enqueued for loading (see
		 * {@link VolatileGlobalCellCache#currentQueueFrame}). This is initialized
		 * to -1. When the entry's data becomes valid, it is set to
		 * {@link Long#MAX_VALUE}.
		 */
		private long enqueueFrame;

		public Entry( final K key, final V data, final VolatileCacheValueLoader< K, V > loader )
		{
			this.key = key;
			this.value = data;
			this.loader = loader;
			enqueueFrame = -1;
		}

		@Override
		public void loadIfNotValid() throws InterruptedException
		{
			/*
			 * TODO: the assumption for following synchronisation pattern is
			 * that isValid() will never go from true to false. When
			 * invalidation API is added, that might change.
			 */
			if ( !value.isValid() )
			{
				synchronized ( this )
				{
					if ( !value.isValid() )
					{
						value = loader.load( key );
						enqueueFrame = Long.MAX_VALUE;
						cache.putSoft( key, this );
						notifyAll();
					}
				}
			}
		}

		@Override
		public K getKey()
		{
			return key;
		}

		@Override
		public V getValue()
		{
			return value;
		}

		@Override
		public long getEnqueueFrame()
		{
			return enqueueFrame;
		}

		@Override
		public void setEnqueueFrame( final long f )
		{
			enqueueFrame = f;
		}
	}

	private final WeakSoftCache cache = WeakSoftCacheImp.getInstance();

	private SoftRefVolatileCacheImp()
	{}

//	private static SoftRefVolatileCacheImp instance = new SoftRefVolatileCacheImp();
}
