package bdv.cache.revised;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SoftRefCache< K, V > implements Cache< K, V >
{
	private final EntryCache< K, Entry > entryCache = new EntryCache<>();

	final class Entry
	{
		private V value;

		public Entry()
		{
			this.value = null;
		}

		public V getValue()
		{
			return value;
		}

		public void setValue( final V value )
		{
			this.value = value;
		}
	}

	@Override
	public V getIfPresent( final Object key )
	{
		final Entry entry = entryCache.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final Callable< ? extends V > loader ) throws ExecutionException
	{
		final Entry entry = entryCache.computeIfAbsent( key, () -> new Entry() );
		if ( entry.getValue() == null )
		{
			synchronized ( entry )
			{
				if ( entry.getValue() == null )
				{
					try
					{
						entry.setValue( loader.call() );
					}
					catch ( final InterruptedException e )
					{
						Thread.currentThread().interrupt();
						throw new ExecutionException( e );
					}
					catch ( final RuntimeException e )
					{
						throw new UncheckedExecutionException( e );
					}
					catch ( final Exception e )
					{
						throw new ExecutionException( e );
					}
					catch ( final Error e )
					{
						throw new ExecutionError( e );
					}
				}
			}
		}

		return entry.getValue();
	}
}

class EntryCache< K, V >
{
	final ConcurrentHashMap< K, Reference< V > > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	public V get( final Object key )
	{
		final Reference< V > ref = map.get( key );
		return ref == null ? null : ref.get();
	}

	public V computeIfAbsent( final K key, final Supplier<? extends V> supplier )
	{
		V value = get( key );
		if ( value == null )
		{
			synchronized ( this )
			{
				value = get( key );
				if ( value == null )
				{
					value = supplier.get();
					map.put( key, new CacheSoftReference( key, value ) );
				}
			}
			cleanUp( 10 );
		}
		return value;
	}

	/**
	 * Remove references from the cache that have been garbage-collected. To
	 * avoid long run-times, per call to {@code cleanUp()}, at most
	 * {@code maxElements} are processed.
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
			( ( CacheSoftReference ) poll ).clean();
		}
		return i;
	}

	class CacheSoftReference extends SoftReference< V >
	{
		private final K key;

		public CacheSoftReference( final K key, final V referent )
		{
			super( referent, queue );
			this.key = key;
		}

		public void clean()
		{
			map.remove( key, this );
		}
	}

//  TODO: remove if not needed:
//	public void invalidateAll()
//	{
//		for ( final Reference< ? > ref : map.values() )
//			ref.clear();
//		map.clear();
//	}
//
//	/**
//	 * Returns the approximate number of entries in this cache.
//	 */
//	public long size()
//	{
//		return map.size();
//	}
//
//	public V computeIfAbsent( final K key, final Function<? super K,? extends V> mappingFunction )
//	{
//		V value = get( key );
//		if ( value == null )
//		{
//			synchronized ( this )
//			{
//				value = get( key );
//				if ( value == null )
//				{
//					value = mappingFunction.apply( key );
//					map.put( key, new CacheSoftReference( key, value ) );
//				}
//			}
//		}
//		return value;
//	}
//
//	public void put( final K key, final V value )
//	{
//		map.put( key, new CacheSoftReference( key, value ) );
//		cleanUp( 10 );
//	}
}
