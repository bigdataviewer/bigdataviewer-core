package viewer.hdf5.img;

import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.ui.util.StopWatch;

public class CacheIoTiming
{
	public static class IoStatistics
	{
		private final ConcurrentHashMap< Thread, StopWatch > perThreadStopWatches = new ConcurrentHashMap< Thread, StopWatch >();

		private final StopWatch stopWatch;

		private int numRunningThreads;

		private long ioBytes;

		private boolean timeoutSet;

		private long timeout;

		private Runnable timeoutCallback;

		public IoStatistics()
		{
			stopWatch = new StopWatch();
			ioBytes = 0;
			timeout = -1;
			timeoutCallback = null;
			numRunningThreads = 0;
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

		public void setIoNanoTimeout( final long t, final Runnable callback )
		{
			timeoutSet = true;
			timeout = t;
			timeoutCallback = callback;
		}

		public void clearIoNanoTimeout()
		{
			timeoutSet = false;
		}

		public boolean timeoutReached()
		{
			return timeoutSet && ( timeout < getIoNanoTime() );
		}

		public void timeoutCallback()
		{
			if ( timeoutCallback != null )
			{
				timeoutCallback.run();
				timeoutCallback = null;
			}
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

	public static void setThreadGroupIoNanoTimeout( final long t, final Runnable callback )
	{
		getThreadGroupIoStatistics().setIoNanoTimeout( t, callback );
	}

	public static void clearThreadGroupIoNanoTimeout()
	{
		getThreadGroupIoStatistics().clearIoNanoTimeout();
	}

	public static long getThreadGroupIoNanoTime()
	{
		return getThreadGroupIoStatistics().getIoNanoTime();
	}

	public static long getThreadGroupIoBytes()
	{
		return getThreadGroupIoStatistics().getIoBytes();
	}

	public static long getThreadGroupCumulativeIoNanoTime()
	{
		return getThreadGroupIoStatistics().getCumulativeIoNanoTime();
	}
}
