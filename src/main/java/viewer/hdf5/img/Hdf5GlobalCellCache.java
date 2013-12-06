package viewer.hdf5.img;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import viewer.hdf5.img.Hdf5ImgCells.CellCache;

public class Hdf5GlobalCellCache< A extends VolatileAccess >
{
	final int numTimepoints;

	final int numSetups;

	final int maxNumLevels;

	final int[] maxLevels;

	class Key
	{
		final int timepoint;

		final int setup;

		final int level;

		final int index;

		public Key( final int timepoint, final int setup, final int level, final int index )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.index = index;

			final long value = ( ( index * maxNumLevels + level ) * numSetups + setup ) * numTimepoints + timepoint;
			hashcode = ( int ) ( value ^ ( value >>> 32 ) );
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof Hdf5GlobalCellCache.Key ) )
				return false;
			@SuppressWarnings( "unchecked" )
			final Key that = ( Key ) other;
			return ( this.timepoint == that.timepoint ) && ( this.setup == that.setup ) && ( this.level == that.level ) && ( this.index == that.index );
		}

		final int hashcode;

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	class Entry
	{
		final protected Key key;

		protected Hdf5Cell< A > data;

		protected long enqueueFrame;

		public Entry( final Key key, final Hdf5Cell< A > data )
		{
			this.key = key;
			this.data = data;
			enqueueFrame = -1;
		}

		@Override
		public void finalize()
		{
			synchronized ( softReferenceCache )
			{
				// System.out.println( "finalizing..." );
				softReferenceCache.remove( key );
				// System.out.println( softReferenceCache.size() +
				// " tiles chached." );
			}
		}
	}

	final protected ConcurrentHashMap< Key, SoftReference< Entry > > softReferenceCache = new ConcurrentHashMap< Key, SoftReference< Entry > >();

	final protected BlockingFetchQueues< Key > queue;

	protected long currentQueueFrame = 0;

	/**
	 * locking adapted from {@link ArrayBlockingQueue}
	 */
	static class BlockingFetchQueues< E >
	{
		private final ArrayDeque< E >[] queues;

		private final int prefetchCapacity;

		private final ArrayDeque< E > prefetch;

	    /** Number of elements in the queue */
	    private int count;

	    /** Main lock guarding all access */
	    private final ReentrantLock lock;

	    /** Condition for waiting takes */
	    private final Condition notEmpty;

		public BlockingFetchQueues( final int numPriorities )
		{
			this( numPriorities, 16384 );
		}

		@SuppressWarnings( "unchecked" )
		public BlockingFetchQueues( final int numPriorities, final int prefetchCapacity )
		{
			queues = new ArrayDeque[ numPriorities ];
			for ( int i = 0; i < numPriorities; ++i )
				queues[ i ] = new ArrayDeque< E >();
			this.prefetchCapacity = prefetchCapacity;
			prefetch = new ArrayDeque< E >( prefetchCapacity );
			lock = new ReentrantLock();
			notEmpty = lock.newCondition();
		}

		public void put( final E element, final int priority )
		{
			final ReentrantLock lock = this.lock;
			lock.lock();
			try
			{
				queues[ priority ].add( element );
				++count;
				notEmpty.signal();
			}
			finally
			{
				lock.unlock();
			}
		}

		public E take() throws InterruptedException
		{
			final ReentrantLock lock = this.lock;
			lock.lockInterruptibly();
			try
			{
				while ( count == 0 )
					notEmpty.await();
				--count;
				for ( final ArrayDeque< E > q : queues )
					if ( !q.isEmpty() )
						return q.remove();
				return prefetch.poll();
			}
			finally
			{
				lock.unlock();
			}
		}

		/**
		 * Atomically removes all of the elements from this queue. The queue
		 * will be empty after this call returns.
		 */
		public void clear()
		{
			final ReentrantLock lock = this.lock;
			lock.lock();
			try
			{
//				System.out.println( "prefetch size before clear = " + prefetch.size() );

				// make room in the prefetch deque
				final int toRemoveFromPrefetch = Math.max( 0, Math.min( prefetch.size(), count - prefetchCapacity ) );
//				System.out.println( "toRemoveFromPrefetch = " + toRemoveFromPrefetch );
				if ( toRemoveFromPrefetch == prefetch.size() )
					prefetch.clear();
				else
					for ( int i = 0; i < toRemoveFromPrefetch; ++i )
						prefetch.remove();

				// move queue contents to the prefetch
				int c = prefetchCapacity; // prefetch capacity left
				// add elements of first queue to the front of the prefetch
				final ArrayDeque< E > q0 = queues[ 0 ];
				final int q0n = Math.min( q0.size(), c );
				for ( int i = 0; i < q0n; ++i )
					prefetch.addFirst( q0.removeLast() );
				q0.clear();
				c -= q0n;
				// add elements of remaining queues to the end of the prefetch
				for ( int j = 1; j < queues.length; ++j )
				{
					final ArrayDeque< E > q = queues[ j ];
					final int qn = Math.min( q.size(), c );
					for ( int i = 0; i < qn; ++i )
						prefetch.addLast( q.removeFirst() );
					q.clear();
					c -= qn;
				}

				// update count: only prefetch is non-empty now
				count = prefetch.size();

//				System.out.println( "prefetch size after clear = " + prefetch.size() );
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	/**
	 * Load the data for the {@link Hdf5Cell} referenced by k, if
	 * <ul>
	 * <li>the {@link Hdf5Cell} is in the cache, and
	 * <li>the data is not yet loaded (valid).
	 * </ul>
	 *
	 * @param k
	 */
	protected void loadIfNotValid( final Key k )
	{
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
				loadEntryIfNotValid( entry );
		}
	}

	class Fetcher extends Thread
	{
		@Override
		final public void run()
		{
			while ( !isInterrupted() )
			{
				try
				{
					loadIfNotValid( queue.take() );
//					Thread.sleep(1);
				}
				catch ( final InterruptedException e )
				{
					break;
				}
			}
		}
	}

	final protected ArrayList< Fetcher > fetchers;

	final protected Hdf5ArrayLoader< A > loader;

	public Hdf5GlobalCellCache( final Hdf5ArrayLoader< A > loader, final int numTimepoints, final int numSetups, final int maxNumLevels, final int[] maxLevels )
	{
		this.loader = loader;
		this.numTimepoints = numTimepoints;
		this.numSetups = numSetups;
		this.maxNumLevels = maxNumLevels;
		this.maxLevels = maxLevels;

		queue = new BlockingFetchQueues< Key >( maxNumLevels );
		fetchers = new ArrayList< Fetcher >();
		for ( int i = 0; i < 2; ++i ) // TODO: add numFetcherThreads parameter
		{
			final Fetcher f = new Fetcher();
			fetchers.add( f );
			f.start();
		}
	}

	/**
	 * Get a cell if it is in the cache or null. Note, that a cell being in the
	 * cache only means that here is a data array, but not necessarily that the
	 * data has already been loaded. If the cell's cache entry has not been
	 * enqueued for loading in the current frame yet, it is enqueued.
	 *
	 * @return a cell with the specified coordinates or null.
	 */
	public Hdf5Cell< A > getGlobalIfCached( final int timepoint, final int setup, final int level, final int index )
	{
		final Key k = new Key( timepoint, setup, level, index );
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
			{
				if ( entry.enqueueFrame < currentQueueFrame )
				{
					entry.enqueueFrame = currentQueueFrame;
					queue.put( k, maxLevels[ setup ] - level );
				}
				return entry.data;
			}
		}
		return null;
	}

	/**
	 * Get a cell if it is in the cache or null. If a cell is returned, it is
	 * guaranteed to have valid data. If necessary this call will block until
	 * the data is loaded.
	 *
	 * @return a valid cell with the specified coordinates or null.
	 */
	public Hdf5Cell< A > getGlobalIfCachedAndLoadBlocking( final int timepoint, final int setup, final int level, final int index )
	{
		final Key k = new Key( timepoint, setup, level, index );
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
			{
				loadEntryIfNotValid( entry );
				return entry.data;
			}
		}
		return null;
	}

	/**
	 * Create a new cell with the specified coordinates, if it isn't in the
	 * cache already. Enqueue the cell for loading.
	 *
	 * @return a cell with the specified coordinates.
	 */
	public synchronized Hdf5Cell< A > createGlobal( final int[] cellDims, final long[] cellMin, final int timepoint, final int setup, final int level, final int index )
	{
		final Key k = new Key( timepoint, setup, level, index );
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
				return entry.data;
		}

		final Hdf5Cell< A > cell = new Hdf5Cell< A >( cellDims, cellMin, loader.emptyArray( cellDims ) );
		softReferenceCache.put( k, new SoftReference< Entry >( new Entry( k, cell ) ) );
		queue.put( k, maxLevels[ setup ] - level );

		return cell;
	}

	/**
	 * Create a new cell with the specified coordinates, if it isn't in the
	 * cache already. Block until the data for the cell has been loaded.
	 *
	 * @return a valid cell with the specified coordinates.
	 */
	public Hdf5Cell< A > createGlobalAndLoadBlocking( final int[] cellDims, final long[] cellMin, final int timepoint, final int setup, final int level, final int index )
	{
		final Key k = new Key( timepoint, setup, level, index );
		Entry entry = null;

		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
			entry = ref.get();
		if ( entry == null )
		{
			final Hdf5Cell< A > cell = new Hdf5Cell< A >( cellDims, cellMin, loader.emptyArray( cellDims ) );
			entry = new Entry( k, cell );
			softReferenceCache.put( k, new SoftReference< Entry >( entry ) );
		}

		loadEntryIfNotValid( entry );
		return entry.data;
	}

	/**
	 * Load the data for the {@link Entry}, if it is not yet loaded (valid).
	 */
	protected void loadEntryIfNotValid( final Entry entry )
	{
		final Hdf5Cell< A > c = entry.data;
		if ( !c.getData().isValid() )
		{
			final int[] cellDims = c.getDimensions();
			final long[] cellMin = c.getMin();
			final Key k = entry.key;
			final int timepoint = k.timepoint;
			final int setup = k.setup;
			final int level = k.level;
			synchronized( loader )
			{
				if ( !entry.data.getData().isValid() )
				{
					final Hdf5Cell< A > cell = new Hdf5Cell< A >( cellDims, cellMin, loader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
					entry.data = cell; // TODO: need to synchronize or make entry.data volatile?
				}
			}
		}
	}

	public class Hdf5CellCache implements CellCache< A >
	{
		final int timepoint;

		final int setup;

		final int level;

		public Hdf5CellCache( final int timepoint, final int setup, final int level )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
		}

		@Override
		public Hdf5Cell< A > get( final int index )
		{
			return getGlobalIfCached( timepoint, setup, level, index );
		}

		@Override
		public Hdf5Cell< A > load( final int index, final int[] cellDims, final long[] cellMin )
		{
			return createGlobal( cellDims, cellMin, timepoint, setup, level, index );
		}
	}

	public class Hdf5BlockingCellCache implements CellCache< A >
	{
		final int timepoint;

		final int setup;

		final int level;

		public Hdf5BlockingCellCache( final int timepoint, final int setup, final int level )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
		}

		@Override
		public Hdf5Cell< A > get( final int index )
		{
			return getGlobalIfCachedAndLoadBlocking( timepoint, setup, level, index );
		}

		@Override
		public Hdf5Cell< A > load( final int index, final int[] cellDims, final long[] cellMin )
		{
			return createGlobalAndLoadBlocking( cellDims, cellMin, timepoint, setup, level, index );
		}
	}

	public void clearQueue()
	{
		queue.clear();
		++currentQueueFrame;
	}
}
