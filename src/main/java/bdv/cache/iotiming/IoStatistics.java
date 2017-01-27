package bdv.cache.iotiming;

import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.ui.util.StopWatch;

public class IoStatistics
{
	private final ConcurrentHashMap< Thread, StopWatch > perThreadStopWatches = new ConcurrentHashMap<>();

	private final StopWatch stopWatch;

	private int numRunningThreads;

	private long ioBytes;

	private final IoTimeBudget ioTimeBudget;

	public IoStatistics()
	{
		stopWatch = new StopWatch();
		ioBytes = 0;
		numRunningThreads = 0;
		ioTimeBudget = new IoTimeBudget();
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
