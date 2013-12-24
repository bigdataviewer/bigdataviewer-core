package bdv.img.cache;

import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.ui.util.StopWatch;

/**
 * Utilities for per {@link ThreadGroup} measuring and budgeting of time spend
 * in (blocking) IO.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class CacheIoTiming
{
	/**
	 * Budget of time that can be spent in blocking IO. The budget is grouped by
	 * priority levels, where level 0 is the highest priority. The budget for
	 * level <em>i>j</em> must always be smaller-equal the budget for level
	 * <em>j</em>.
	 */
	public static class IoTimeBudget
	{
		private final long[] initialBudget;

		private final long[] budget;

		public IoTimeBudget( final long[] initBudget )
		{
			initialBudget = initBudget.clone();
			for ( int l = 1; l < initialBudget.length; ++l )
				if ( initialBudget[ l ] > initialBudget[ l - 1 ] )
					initialBudget[ l ] = initialBudget[ l - 1 ];
			budget = initialBudget.clone();
		}

		public void reset()
		{
			System.arraycopy( initialBudget, 0, budget, 0, budget.length );
			System.out.println( net.imglib2.util.Util.printCoordinates( budget ) );
		}

		public synchronized long timeLeft( final int level )
		{
			return budget[ level ];
		}

		public synchronized void use( final long t, final int level )
		{
			int l = 0;
			for ( ; l <= level; ++l )
				budget[ l ] -= t;
			for ( ; l < budget.length && budget[ l ] > budget[ l - 1 ]; ++l )
				budget[ l ] = budget[ l - 1 ];
		}
	}

	public static class IoStatistics
	{
		private final ConcurrentHashMap< Thread, StopWatch > perThreadStopWatches = new ConcurrentHashMap< Thread, StopWatch >();

		private final StopWatch stopWatch;

		private int numRunningThreads;

		private long ioBytes;

		private IoTimeBudget ioTimeBudget;

		public IoStatistics()
		{
			stopWatch = new StopWatch();
			ioBytes = 0;
			numRunningThreads = 0;
			ioTimeBudget = null;
		}

		public synchronized void start()
		{
			getThreadStopWatch().start();
			if( numRunningThreads++ == 0 )
				stopWatch.start();
		}

		public synchronized void stop()
		{
			getThreadStopWatch().stop();
			if( --numRunningThreads == 0 )
				stopWatch.stop();
		}

		public void incIoBytes( final long n )
		{
			ioBytes += n;
		}

		public long getIoBytes()
		{
			return ioBytes;
		}

		public long getIoNanoTime()
		{
			return stopWatch.nanoTime();
		}

		public long getCumulativeIoNanoTime()
		{
			long sum = 0;
			for ( final StopWatch w : perThreadStopWatches.values() )
				sum += w.nanoTime();
			return sum;
		}

		public IoTimeBudget getIoTimeBudget()
		{
			return ioTimeBudget;
		}

		public void setIoTimeBudget( final IoTimeBudget budget )
		{
			ioTimeBudget = budget;
		}

		private StopWatch getThreadStopWatch()
		{
			final Thread thread = Thread.currentThread();
			StopWatch w = perThreadStopWatches.get( thread );
			if ( w == null )
			{
				w = new StopWatch();
				perThreadStopWatches.put( thread, w );
			}
			return w;
		}
	}

	private final static ConcurrentHashMap< ThreadGroup, IoStatistics > perThreadGroupIoStatistics = new ConcurrentHashMap< ThreadGroup, IoStatistics >();

	public static IoStatistics getThreadGroupIoStatistics()
	{
		final ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		IoStatistics statistics = perThreadGroupIoStatistics.get( threadGroup );
		if ( statistics == null )
		{
			statistics = new IoStatistics();
			perThreadGroupIoStatistics.put( threadGroup, statistics );
		}
		return statistics;
	}

	public static long getThreadGroupIoNanoTime()
	{
		return getThreadGroupIoStatistics().getIoNanoTime();
	}

	public static long getThreadGroupIoBytes()
	{
		return getThreadGroupIoStatistics().getIoBytes();
	}
}
