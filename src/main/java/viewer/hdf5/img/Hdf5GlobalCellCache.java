package viewer.hdf5.img;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.imglib2.display.nativevolatile.VolatileAccess;
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
	 * adapted from {@link LinkedBlockingQueue}
	 */
	static class BlockingFetchQueues< E >
	{
		private final ArrayDeque< E >[] queues;

		@SuppressWarnings( "unchecked" )
		public BlockingFetchQueues( final int numPriorities )
		{
			 queues = new ArrayDeque[ numPriorities ];
			 for ( int i = 0; i < numPriorities; ++i )
				 queues[ i ] = new ArrayDeque< E >();
		}

		public void put( final E element, final int priority )
		{
			int c = -1;
			final ReentrantLock putLock = this.putLock;
			putLock.lock();
			try
			{
				queues[ priority ].add( element );
				c = count.getAndIncrement();
			}
			finally
			{
				putLock.unlock();
			}
			if ( c == 0 )
				signalNotEmpty();
		}

		public E take() throws InterruptedException
		{
			E x;
			int c = -1;
			final AtomicInteger count = this.count;
			final ReentrantLock takeLock = this.takeLock;
			takeLock.lockInterruptibly();
			try
			{
				while ( count.get() == 0 )
				{
					notEmpty.await();
				}
				x = dequeue();
				c = count.getAndDecrement();
				if ( c > 1 )
					notEmpty.signal();
			}
			finally
			{
				takeLock.unlock();
			}
			return x;
		}

	    /**
	     * Atomically removes all of the elements from this queue.
	     * The queue will be empty after this call returns.
	     */
	    public void clear() {
	        fullyLock();
	        try {
				for ( final ArrayDeque< E > q : queues )
					q.clear();
				count.set( 0 );
	        } finally {
	            fullyUnlock();
	        }
	    }

	    /** Current number of elements */
	    private final AtomicInteger count = new AtomicInteger(0);

	    /** Lock held by take, poll, etc */
	    private final ReentrantLock takeLock = new ReentrantLock();

	    /** Wait queue for waiting takes */
	    private final Condition notEmpty = takeLock.newCondition();

	    /** Lock held by put, offer, etc */
	    private final ReentrantLock putLock = new ReentrantLock();

	    /**
	     * Signals a waiting take. Called only from put/offer (which do not
	     * otherwise ordinarily lock takeLock.)
	     */
		private void signalNotEmpty()
		{
			final ReentrantLock takeLock = this.takeLock;
			takeLock.lock();
			try
			{
				notEmpty.signal();
			}
			finally
			{
				takeLock.unlock();
			}
		}

	    /**
	     * Removes a node from head of queue.
	     *
	     * @return the node
	     */
		private E dequeue()
		{
			for ( final ArrayDeque< E > q : queues )
				if ( !q.isEmpty() )
					return q.remove();
			return null;
		}

	    /**
	     * Lock to prevent both puts and takes.
	     */
	    private void fullyLock() {
	        putLock.lock();
	        takeLock.lock();
	    }

	    /**
	     * Unlock to allow both puts and takes.
	     */
	    private void fullyUnlock() {
	        takeLock.unlock();
	        putLock.unlock();
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
		for ( int i = 0; i < 4; ++i )
		{
			final Fetcher f = new Fetcher();
			fetchers.add( f );
			f.start();
		}
	}

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
	 * Load the data for the {@link Hdf5Cell} referenced by k, if
	 * <ul>
	 * <li>the {@link Hdf5Cell} is in the cache, and
	 * <li>the data is not yet loaded (valid).
	 * </ul>
	 *
	 * @param k
	 */
	public void loadIfNotValid( final Key k )
	{
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
			{
				final Hdf5Cell< A > c = entry.data;
				if ( ! c.getData().isValid() )
				{
					final int[] cellDims = c.getDimensions();
					final long[] cellMin = c.getMin();
					final int timepoint = k.timepoint;
					final int setup = k.setup;
					final int level = k.level;
					final Hdf5Cell< A > cell;
					synchronized( loader )
					{
						cell = new Hdf5Cell< A >( cellDims, cellMin, loader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
					}
					entry.data = cell; // TODO: need to synchronize or make entry.data volatile?
				}
			}
		}
	}

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

	public void clearQueue()
	{
		queue.clear();
		++currentQueueFrame;
	}
}
