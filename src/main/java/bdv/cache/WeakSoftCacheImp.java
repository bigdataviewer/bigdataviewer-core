package bdv.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

//TODO: rename, refactor, document
public class WeakSoftCacheImp< K, V > implements WeakSoftCache< K, V >
{
	private static final int MAX_PER_FRAME_FINALIZE_ENTRIES = 500; // TODO rename

	private final ConcurrentHashMap< K, Reference< V > > softReferenceCache = new ConcurrentHashMap<>();

	private final ReferenceQueue< V > finalizeQueue = new ReferenceQueue<>();

	WeakSoftCacheImp()
	{}

	@Override
	public void putWeak( final K key, final V value )
	{
		softReferenceCache.put( key, new MyWeakReference<>( key, value, finalizeQueue ) );
	}

	@Override
	public void putSoft( final K key, final V value )
	{
		softReferenceCache.put( key, new MySoftReference<>( key, value, finalizeQueue ) );
	}

	@Override
	public V get( final Object key )
	{
		final Reference< V > ref = softReferenceCache.get( key );
		return ref == null ? null : ref.get();
	}

	@Override
	public void clearCache()
	{
		for ( final Reference< ? > ref : softReferenceCache.values() )
			ref.clear();
		softReferenceCache.clear();
	}

	@Override
	public void finalizeRemovedCacheEntries()
	{
		synchronized ( softReferenceCache )
		{
			for ( int i = 0; i < MAX_PER_FRAME_FINALIZE_ENTRIES; ++i )
			{
				final Reference< ? > poll = finalizeQueue.poll();
				if ( poll == null )
					break;
				final Object key = ( ( GetKey< ? > ) poll ).getKey();
				final Reference< ? > ref = softReferenceCache.get( key );
				if ( ref == poll )
					softReferenceCache.remove( key );
			}
		}
	}

	private static interface GetKey< K >
	{
		public K getKey();
	}

	private static class MySoftReference< K, V > extends SoftReference< V > implements GetKey< K >
	{
		private final K key;

		public MySoftReference( final K key, final V referent, final ReferenceQueue< ? super V > q )
		{
			super( referent, q );
			this.key = key;
		}

		@Override
		public K getKey()
		{
			return key;
		}
	}

	private static class MyWeakReference< K, V > extends WeakReference< V > implements GetKey< K >
	{
		private final K key;

		public MyWeakReference( final K key, final V referent, final ReferenceQueue< ? super V > q )
		{
			super( referent, q );
			this.key = key;
		}

		@Override
		public K getKey()
		{
			return key;
		}
	}
}
