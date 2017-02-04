package bdv.cache.revised;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SoftRefCache< K, V > implements Cache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final class CacheSoftReference extends SoftReference< V >
	{
		private final Entry entry;

		public CacheSoftReference( final V referent, final Entry entry )
		{
			super( referent, queue );
			this.entry = entry;
		}

		public void clean()
		{
			map.remove( entry.key, entry );
		}
	}

	final class Entry
	{
		final K key;

		private SoftReference< V > ref;

		boolean loaded;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new SoftReference<>( null );
			this.loaded = false;
		}

		public V getValue()
		{
			return ref.get();
		}

		public void setValue( final V value )
		{
			this.loaded = true;
			this.ref = new CacheSoftReference( value, this );
		}
	}

	@Override
	public V getIfPresent( final Object key )
	{
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final Callable< ? extends V > loader ) throws ExecutionException
	{
		final Entry entry = map.computeIfAbsent( key, ( k ) -> new Entry( k ) );
		V value = entry.getValue();
		if ( value == null )
		{
			synchronized ( entry )
			{
				if ( entry.loaded )
				{
					value = entry.getValue();
					if ( value == null )
					{
						/*
						 * The entry was already loaded, but its value has been
						 * garbage collected. We need to create a new entry
						 */
						map.remove( key, entry );
						value = get( key, loader );
					}
				}
				else
				{
					try
					{
						value = loader.call();
						entry.setValue( value );
					}
					catch ( final InterruptedException e )
					{
						Thread.currentThread().interrupt();
						throw new ExecutionException( e );
					}
					catch ( final Exception e )
					{
						throw new ExecutionException( e );
					}
				}
			}
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
			( ( CacheSoftReference ) poll ).clean();
		}
		return i;
	}

	@Override
	public void invalidateAll()
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}
}
