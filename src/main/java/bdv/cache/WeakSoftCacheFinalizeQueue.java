package bdv.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;

public class WeakSoftCacheFinalizeQueue
{
	public static final int MAX_PER_FRAME_FINALIZE_ENTRIES = 500;

	private final ReferenceQueue< Object > finalizeQueue = new ReferenceQueue<>();

	/**
	 * Remove references from the cache that have been garbage-collected.
	 * To avoid long run-times, per call to {@code cleanUp()}, at most
	 * {@link #MAX_PER_FRAME_FINALIZE_ENTRIES} are processed.
	 */
	public void cleanUp()
	{
		for ( int i = 0; i < MAX_PER_FRAME_FINALIZE_ENTRIES; ++i )
		{
			final Reference< ? > poll = finalizeQueue.poll();
			if ( poll == null )
				break;
			final GetKey< ?, ? > x = ( GetKey< ?, ? > ) poll;
			final Object key = x.getKey();
			final Map< ?, ? extends Reference< ? > > cache = x.getCache();
			final Reference< ? > ref = cache.get( key );
			if ( ref == poll )
				cache.remove( key );
		}
	}

	public < K, V > Reference< V > createSoftReference( final K key, final V referent, final Map< K, Reference< V > > cache )
	{
		return new CacheSoftReference< >( key, referent, cache, finalizeQueue );
	}

	public < K, V > Reference< V > createWeakReference( final K key, final V referent, final Map< K, Reference< V > > cache )
	{
		return new CacheWeakReference<>( key, referent, cache, finalizeQueue );
	}

	private static interface GetKey< K, V >
	{
		public K getKey();

		public Map< K, Reference< V > > getCache();
	}

	private static class CacheSoftReference< K, V > extends SoftReference< V > implements GetKey< K, V >
	{
		private final K key;

		private final Map< K, Reference< V > > cache;

		public CacheSoftReference( final K key, final V referent, final Map< K, Reference< V > > cache, final ReferenceQueue< ? super V > q )
		{
			super( referent, q );
			this.key = key;
			this.cache = cache;
		}

		@Override
		public K getKey()
		{
			return key;
		}

		@Override
		public Map< K, Reference< V > > getCache()
		{
			return cache;
		}
	}

	private static class CacheWeakReference< K, V > extends WeakReference< V > implements GetKey< K, V >
	{
		private final K key;

		private final Map< K, Reference< V > > cache;

		public CacheWeakReference( final K key, final V referent, final Map< K, Reference< V > > cache, final ReferenceQueue< ? super V > q )
		{
			super( referent, q );
			this.key = key;
			this.cache = cache;
		}

		@Override
		public K getKey()
		{
			return key;
		}

		@Override
		public Map< K, Reference< V > > getCache()
		{
			return cache;
		}
	}
}
