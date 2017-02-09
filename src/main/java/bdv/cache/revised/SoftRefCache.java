package bdv.cache.revised;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SoftRefCache< K, V > implements Cache< K, V >
{
	final ConcurrentHashMap< K, Entry > map = new ConcurrentHashMap<>();

	final ReferenceQueue< V > queue = new ReferenceQueue<>();

	final ReferenceQueue< V > phantomQueue = new ReferenceQueue<>();

	final class CacheSoftReference extends SoftReference< V >
	{
		private final Entry entry;

		public CacheSoftReference( final V referent )
		{
			super( referent );
			this.entry = null;
		}

		public CacheSoftReference( final V referent, final Entry entry )
		{
			super( referent, queue );
			this.entry = entry;
		}

		public void clean()
		{
			entry.remove();
		}
	}

	static final class CachePhantomReference< V > extends PhantomReference< V >
	{
		static Field referent = null;
		{
			try
			{
				referent = Reference.class.getDeclaredField( "referent" );
			}
			catch ( NoSuchFieldException | SecurityException e )
			{
				e.printStackTrace();
			}
			referent.setAccessible( true );
		}

		SoftRefCache< ?, V >.Entry entry;

		public CachePhantomReference( final V referent, final ReferenceQueue< V > remove, final SoftRefCache< ?, V >.Entry entry )
		{
			super( referent, remove );
			this.entry = entry;
		}

		@SuppressWarnings( "unchecked" )
		public V resurrect()
		{
			try
			{
				return ( V ) referent.get( this );
			}
			catch ( IllegalArgumentException | IllegalAccessException e )
			{
				e.printStackTrace();
				return null;
			}
		}
	}

	final class Entry
	{
		final K key;

		private CacheSoftReference ref;

		private CachePhantomReference< V > phantomRef;

		private Remover< ? super K, ? super V > remover;

		boolean loaded;

		public Entry( final K key )
		{
			this.key = key;
			this.ref = new CacheSoftReference( null );
			this.phantomRef = null;
			this.remover = null;
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

		public void setValue( final V value, final Remover< ? super K, ? super V > remover )
		{
			this.loaded = true;
			this.ref = new CacheSoftReference( value, this );
			this.phantomRef = new CachePhantomReference<>( value, phantomQueue, this );
			this.remover = remover;
		}

		public synchronized void remove()
		{
			if ( remover != null )
			{
				final V value = phantomRef.resurrect();
				phantomRef.clear();
				phantomRef = null;
				remover.remove( key, value );
				remover = null;
//				map.remove( key, this ); // TODO: this should be in here when non-removal Cache is split out
			}
			map.remove( key, this );
		}
	}

	@Override
	public V getIfPresent( final Object key )
	{
		processRemovalQueue();
		final Entry entry = map.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final Callable< ? extends V > loader ) throws ExecutionException
	{
		processRemovalQueue();
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
						entry.remove();
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

		if ( value == null )
			value = get( key, loader );

		return value;
	}


	@Override
	public V get( final K key, final Callable< ? extends V > loader, final Remover< ? super K, ? super V > remover ) throws ExecutionException
	{
		processRemovalQueue();
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
						entry.remove();
					}
				}
				else
				{
					try
					{
						value = loader.call();
						entry.setValue( value, remover );
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

		if ( value == null )
			value = get( key, loader, remover );

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

	public void processRemovalQueue()
	{
		int j = 0;
		while ( true )
		{
			@SuppressWarnings( "unchecked" )
			final CachePhantomReference< V > pr = ( CachePhantomReference< V > ) phantomQueue.poll();
			if ( pr == null )
				break;
			pr.entry.remove();
			pr.clear();
			++j;
		}
		if ( j != 0 )
			System.out.println( "processed " + j + " references" );
	}

	@Override
	public void invalidateAll()
	{
		// TODO
		throw new UnsupportedOperationException( "not implemented yet" );
	}
}
