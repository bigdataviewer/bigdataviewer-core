/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.img.cache;

import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.ui.util.StopWatch;

/**
 * Utilities for per {@link ThreadGroup} measuring and budgeting of time spent
 * in (blocking) IO.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class CacheIoTiming
{
	/**
	 * Budget of time that can be spent in blocking IO. The budget is grouped by
	 * priority levels, where level 0 is the highest priority. The budget for
	 * level <em>i&gt;j</em> must always be smaller-equal the budget for level
	 * <em>j</em>.
	 */
	public static class IoTimeBudget
	{
		private final long[] budget;

		public IoTimeBudget( final int numLevels )
		{
			budget = new long[ numLevels ];
		}

		public synchronized void reset( final long[] partialBudget )
		{
			if ( partialBudget == null )
				clear();
			else
			{
				for ( int i = 0; i < budget.length; ++i )
					budget[ i ] = partialBudget.length > i ? partialBudget[ i ] : partialBudget[ partialBudget.length - 1 ];
				for ( int i = 1; i < budget.length; ++i )
					if ( budget[ i ] > budget[ i - 1 ] )
						budget[ i ] = budget[ i - 1 ];
			}
		}

		public synchronized void clear()
		{
			for ( int i = 0; i < budget.length; ++i )
				budget[ i ] = 0;
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

	private final ConcurrentHashMap< ThreadGroup, IoStatistics > perThreadGroupIoStatistics = new ConcurrentHashMap< ThreadGroup, IoStatistics >();

	public IoStatistics getThreadGroupIoStatistics()
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

	public long getThreadGroupIoNanoTime()
	{
		return getThreadGroupIoStatistics().getIoNanoTime();
	}

	public long getThreadGroupIoBytes()
	{
		return getThreadGroupIoStatistics().getIoBytes();
	}
}
