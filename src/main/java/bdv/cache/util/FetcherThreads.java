/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.cache.util;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.IntFunction;

import bdv.cache.VolatileCacheValue;

/**
 * TODO revise javadoc
 *
 * A set of threads that load data. Each thread does the following in a loop:
 * <ol>
 * <li>Take the next {@code key} from a queue.</li>
 * <li>Try {@link Callable< ? >#load(Object) loading} the key's data (retry until that
 * succeeds).</li>
 * </ol>
 * {@link FetcherThreads} are employed by {@link LoadingVolatileCache} to
 * asynchronously load {@link VolatileCacheValue}s.
 *
 * <p>
 * TODO Add shutdown() method.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FetcherThreads
{
	private final ArrayList< Fetcher > fetchers;

	/**
	 * Create (and start) a set of fetcher threads.
	 * <p>
	 * Fetcher threads are named {@code Fetcher-0} ... {@code Fetcher-n}.
	 *
	 * @param queue the queue from which request keys are taken.
	 * @param loader loads data associated with keys.
	 * @param numFetcherThreads how many parallel fetcher threads to start.
	 */
	public FetcherThreads(
			final BlockingFetchQueues< Callable< ? > > queue,
			final int numFetcherThreads )
	{
		this( queue, numFetcherThreads, i -> String.format( "Fetcher-%d", i ) );
	}

	/**
	 * Create (and start) a set of fetcher threads.
	 *
	 * @param queue the queue from which request keys are taken.
	 * @param loader loads data associated with keys.
	 * @param numFetcherThreads how many parallel fetcher threads to start.
	 * @param threadIndexToName a function for naming fetcher threads (takes an index and returns a name).
	 */
	public FetcherThreads(
			final BlockingFetchQueues< Callable< ? > > queue,
			final int numFetcherThreads,
			final IntFunction< String > threadIndexToName )
	{
		fetchers = new ArrayList<>( numFetcherThreads );
		for ( int i = 0; i < numFetcherThreads; ++i )
		{
			final Fetcher f = new Fetcher( queue );
			f.setDaemon( true );
			f.setName( threadIndexToName.apply( i ) );
			fetchers.add( f );
			f.start();
		}
	}

	/**
	 * Pause all Fetcher threads for the specified number of milliseconds.
	 */
	public void pauseFetcherThreadsFor( final long ms )
	{
		pauseFetcherThreadsUntil( System.currentTimeMillis() + ms );
	}

	/**
	 * pause all Fetcher threads until the given time (see
	 * {@link System#currentTimeMillis()}).
	 */
	public void pauseFetcherThreadsUntil( final long timeMillis )
	{
		for ( final Fetcher f : fetchers )
			f.pauseUntil( timeMillis );
	}

	/**
	 * Wake up all Fetcher threads immediately. This ends any
	 * {@link #pauseFetcherThreadsFor(long)} and
	 * {@link #pauseFetcherThreadsUntil(long)} set earlier.
	 */
	public void wakeFetcherThreads()
	{
		for ( final Fetcher f : fetchers )
			f.wakeUp();
	}

	static final class Fetcher extends Thread
	{
		private final BlockingFetchQueues< Callable< ? > > queue;

		private final Object lock = new Object();

		private volatile long pauseUntilTimeMillis = 0;

		public Fetcher( final BlockingFetchQueues< Callable< ? > > queue )
		{
			this.queue = queue;
		}

		@Override
		public final void run()
		{
			Callable< ? > loader = null;
			while ( true )
			{
				while ( loader == null )
					try
					{
						loader = queue.take();
					}
					catch ( final InterruptedException e )
					{}
				long waitMillis = pauseUntilTimeMillis - System.currentTimeMillis();
				while ( waitMillis > 0 )
				{
					try
					{
						synchronized ( lock )
						{
							lock.wait( waitMillis );
						}
					}
					catch ( final InterruptedException e )
					{}
					waitMillis = pauseUntilTimeMillis - System.currentTimeMillis();
				}
				try
				{
					loader.call();
					loader = null;
				}
				catch ( final InterruptedException e )
				{}
				catch ( final ExecutionException e )
				{
					if ( ! ( e.getCause() instanceof InterruptedException ) )
						e.printStackTrace();
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}

		public void pauseUntil( final long timeMillis )
		{
			pauseUntilTimeMillis = timeMillis;
			interrupt();
		}

		public void wakeUp()
		{
			pauseUntilTimeMillis = 0;
			synchronized ( lock )
			{
				lock.notify();
			}
		}
	}
}
