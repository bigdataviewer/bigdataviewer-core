package bdv.img.cache;

import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import bdv.img.cache.CacheIoTiming.IoStatistics;
import bdv.img.cache.CacheIoTiming.IoTimeBudget;
import bdv.img.cache.VolatileImgCells.CellCache;

public class VolatileGlobalCellCache< A extends VolatileAccess > implements Cache
{
	private final int numTimepoints;

	private final int numSetups;

	private final int maxNumLevels;

	private final int[] maxLevels;

	class Key
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private final int index;

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
			if ( !( other instanceof VolatileGlobalCellCache.Key ) )
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
		private final Key key;

		private VolatileCell< A > data;

		/**
		 * When was this entry last enqueued for loading (see
		 * {@link VolatileGlobalCellCache#currentQueueFrame}). This is initialized
		 * to -1. When the entry's data becomes valid, it is set to
		 * {@link Long#MAX_VALUE}.
		 */
		private long enqueueFrame;

		public Entry( final Key key, final VolatileCell< A > data )
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

	protected final ConcurrentHashMap< Key, Reference< Entry > > softReferenceCache = new ConcurrentHashMap< Key, Reference< Entry > >();

	protected final BlockingFetchQueues< Key > queue;

	protected long currentQueueFrame = 0;

	class Fetcher extends Thread
	{
		@Override
		public final void run()
		{
			while ( !isInterrupted() )
			{
				try
				{
					while ( pause )
						synchronized ( this )
						{
//							System.out.println( "fetcher wait" );
							pause = false;
							wait( 5 );
						}
//					System.out.println( "fetcher load" );
					loadIfNotValid( queue.take() );
//					Thread.sleep(1);
				}
				catch ( final InterruptedException e )
				{
					break;
				}
			}
		}

		private volatile boolean pause = false;

		public void pause()
		{
			pause = true;
		}
	}

	public void pauseFetcherThreads()
	{
		for ( final Fetcher f : fetchers )
			f.pause();
	}

	private final ArrayList< Fetcher > fetchers;

	private final CacheArrayLoader< A > loader;

	public VolatileGlobalCellCache( final CacheArrayLoader< A > loader, final int numTimepoints, final int numSetups, final int maxNumLevels, final int[] maxLevels )
	{
		this.loader = loader;
		this.numTimepoints = numTimepoints;
		this.numSetups = numSetups;
		this.maxNumLevels = maxNumLevels;
		this.maxLevels = maxLevels;

		queue = new BlockingFetchQueues< Key >( maxNumLevels );
		fetchers = new ArrayList< Fetcher >();
		for ( int i = 0; i < 1; ++i ) // TODO: add numFetcherThreads parameter
		{
			final Fetcher f = new Fetcher();
			f.setDaemon( true );
			fetchers.add( f );
			f.start();
		}
	}

	/**
	 * Load the data for the {@link VolatileCell} referenced by k, if
	 * <ul>
	 * <li>the {@link VolatileCell} is in the cache, and
	 * <li>the data is not yet loaded (valid).
	 * </ul>
	 *
	 * @param k
	 */
	protected void loadIfNotValid( final Key k )
	{
		final Reference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
				loadEntryIfNotValid( entry );
		}
	}

	/**
	 * Load the data for the {@link Entry}, if it is not yet loaded (valid).
	 */
	protected void loadEntryIfNotValid( final Entry entry )
	{
		final VolatileCell< A > c = entry.data;
		if ( !c.getData().isValid() )
		{
			final int[] cellDims = c.getDimensions();
			final long[] cellMin = c.getMin();
			final Key k = entry.key;
			final int timepoint = k.timepoint;
			final int setup = k.setup;
			final int level = k.level;
			final long t0 = System.currentTimeMillis();
			long t1;
			synchronized ( entry )
			{
				t1 = System.currentTimeMillis() - t0;
				if ( !entry.data.getData().isValid() )
				{
					final VolatileCell< A > cell = new VolatileCell< A >( cellDims, cellMin, loader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
					entry.data = cell; // TODO: need to synchronize or make entry.data volatile?
					softReferenceCache.put( entry.key, new SoftReference< Entry >( entry ) );
				}
			}
			if ( t1 > 200 )
			{
				final StringWriter sw = new StringWriter();
				sw.write( "waited " + t1 + " ms for entry lock\n" );
				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
				boolean found = false;
				for ( final StackTraceElement elem : trace )
				{
					if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "get" ) )
					{
						found = true;
						sw.write( "Hdf5CellCache.get\n" );
						break;
					}
					else if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "load" ) )
					{
						found = true;
						sw.write( "Hdf5CellCache.load\n" );
						break;
					}
					else if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Fetcher" ) && elem.getMethodName().equals( "run" ) )
					{
						found = true;
						sw.write( "Fetcher.run\n" );
						break;
					}
				}
				if ( !found )
				{
					for ( final StackTraceElement elem : trace )
						sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
				}
				System.out.println( sw.toString() );
			}
		}
	}

	/**
	 * Enqueue the {@link Entry} if it hasn't been enqueued for this frame
	 * already.
	 */
	protected void enqueueEntry( final Entry entry )
	{
		if ( entry.enqueueFrame < currentQueueFrame )
		{
			entry.enqueueFrame = currentQueueFrame;
			final Key k = entry.key;
			final int priority = maxLevels[ k.setup ] - k.level;
			queue.put( k, priority );
		}
	}

	/**
	 * Load the data for the {@link Entry} if it is not yet loaded (valid) and
	 * there is enough {@link IoTimeBudget} left. Otherwise, enqueue the
	 * {@link Entry} if it hasn't been enqueued for this frame already.
	 */
	protected void loadOrEnqueue( final Entry entry )
	{
		final IoStatistics stats = CacheIoTiming.getThreadGroupIoStatistics();
		final IoTimeBudget budget = stats.getIoTimeBudget();
		final Key k = entry.key;
		final int priority = maxLevels[ k.setup ] - k.level;
		if ( budget.timeLeft( priority ) > 0 )
		{
			pauseFetcherThreads();
			final long t0 = stats.getIoNanoTime();
			stats.start();
			loadEntryIfNotValid( entry );
			stats.stop();
			final long t = stats.getIoNanoTime() - t0;
			budget.use( t, priority );
			pauseFetcherThreads();
		}
		else
			enqueueEntry( entry );
	}

	public static enum LoadingStrategy
	{
		VOLATILE,
		BLOCKING,
		BUDGETED
	};

	/**
	 * Get a cell if it is in the cache or null. Note, that a cell being in the
	 * cache only means that here is a data array, but not necessarily that the
	 * data has already been loaded.
	 *
	 * If the cell data has not been loaded, do the following, depending on the
	 * {@link LoadingStrategy}:
	 *
	 * <ul>
	 *   <li> {@link LoadingStrategy#VOLATILE}:
	 *        Enqueue the cell for asynchronous loading by a fetcher thread, if
	 *        it has not been enqueued in the current frame already.
	 *   <li> {@link LoadingStrategy#BLOCKING}:
	 *        Load the cell data immediately.
	 *   <li> {@link LoadingStrategy#BUDGETED}:
	 *        Load the cell data immediately if there is enough
	 *        {@link IoTimeBudget} left for the current thread group.
	 *        Otherwise enqueue for asynchronous loading, if it has not been
	 *        enqueued in the current frame already.
	 * </ul>
	 *
	 * @return a cell with the specified coordinates or null.
	 */
	public VolatileCell< A > getGlobalIfCached( final int timepoint, final int setup, final int level, final int index, final LoadingStrategy loadingStrategy )
	{
		final Key k = new Key( timepoint, setup, level, index );
		final Reference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
			{
				switch ( loadingStrategy )
				{
				case VOLATILE:
				default:
					enqueueEntry( entry );
					break;
				case BLOCKING:
					loadEntryIfNotValid( entry );
					break;
				case BUDGETED:
					if ( !entry.data.getData().isValid() )
						loadOrEnqueue( entry );
					break;
				}
				return entry.data;
			}
		}
		return null;
	}

	/**
	 * Create a new cell with the specified coordinates, if it isn't in the
	 * cache already. Depending on the {@link LoadingStrategy}, do the
	 * following:
	 * <ul>
	 *   <li> {@link LoadingStrategy#VOLATILE}:
	 *        Enqueue the cell for asynchronous loading by a fetcher thread.
	 *   <li> {@link LoadingStrategy#BLOCKING}:
	 *        Load the cell data immediately.
	 *   <li> {@link LoadingStrategy#BUDGETED}:
	 *        Load the cell data immediately if there is enough
	 *        {@link IoTimeBudget} left for the current thread group.
	 *        Otherwise enqueue for asynchronous loading.
	 * </ul>
	 *
	 * @return a cell with the specified coordinates.
	 */
	public synchronized VolatileCell< A > createGlobal( final int[] cellDims, final long[] cellMin, final int timepoint, final int setup, final int level, final int index, final LoadingStrategy loadingStrategy )
	{
		final Key k = new Key( timepoint, setup, level, index );
		Entry entry = null;

		final Reference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
			entry = ref.get();

		if ( entry == null )
		{
			final VolatileCell< A > cell = new VolatileCell< A >( cellDims, cellMin, loader.emptyArray( cellDims ) );
			entry = new Entry( k, cell );
			softReferenceCache.put( k, new WeakReference< Entry >( entry ) );
		}

		switch ( loadingStrategy )
		{
		case VOLATILE:
		default:
			enqueueEntry( entry );
			break;
		case BLOCKING:
			loadEntryIfNotValid( entry );
			break;
		case BUDGETED:
			if ( !entry.data.getData().isValid() )
				loadOrEnqueue( entry );
			break;
		}
		return entry.data;
	}

	/**
	 * Prepare the cache for providing data for the "next frame":
	 * <ul>
	 * <li>the contents of fetch queues is moved to the prefetch.
	 * <li>the internal frame counter is incremented, which will enable
	 * previously enqueued requests to be enqueued again for the new frame.
	 * </ul>
	 */
	@Override
	public void prepareNextFrame()
	{
		queue.clear();
		++currentQueueFrame;
	}

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO per frame/
	 *
	 * @param partialBudget
	 *            Initial budget (in nanoseconds) for priority levels 0 through
	 *            <em>n</em>. The budget for level <em>i>j</em> must always be
	 *            smaller-equal the budget for level <em>j</em>. If <em>n</em>
	 *            is smaller than the maximum number of mipmap levels, the
	 *            remaining priority levels are filled up with budget[n].
	 * @param reinitialize
	 *            If true, the IO time budget is initialized to the given
	 *            partial budget. If false, the IO time budget is reset to the
	 *            previous initial values.
	 */
	@Override
	public void initIoTimeBudget( final long[] partialBudget, final boolean reinitialize )
	{
		final IoStatistics stats = CacheIoTiming.getThreadGroupIoStatistics();
		if ( reinitialize || stats.getIoTimeBudget() == null )
		{
			final long[] budget = new long[ maxNumLevels ];
			for ( int i = 0; i < budget.length; ++i )
				budget[ i ] = partialBudget.length > i ? partialBudget[ i ] : partialBudget[ partialBudget.length - 1 ];
			stats.setIoTimeBudget( new IoTimeBudget( budget ) );
		}
		stats.getIoTimeBudget().reset();
	}

	public class Hdf5CellCache implements CellCache< A >
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private final LoadingStrategy loadingStrategy;

		public Hdf5CellCache( final int timepoint, final int setup, final int level, final LoadingStrategy strategy )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.loadingStrategy = strategy;
		}

		@Override
		public VolatileCell< A > get( final int index )
		{
			return getGlobalIfCached( timepoint, setup, level, index, loadingStrategy );
		}

		@Override
		public VolatileCell< A > load( final int index, final int[] cellDims, final long[] cellMin )
		{
			return createGlobal( cellDims, cellMin, timepoint, setup, level, index, loadingStrategy );
		}
	}
}
