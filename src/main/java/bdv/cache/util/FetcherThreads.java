package bdv.cache.util;

import java.util.ArrayList;
import java.util.function.IntFunction;

import bdv.cache.LoadingVolatileCache;
import bdv.cache.VolatileCacheEntry;
import bdv.cache.VolatileCacheValue;
import bdv.cache.WeakSoftCache;

/**
 * A set of threads that load {@link VolatileCacheValue}s. Each thread does the
 * following in a loop:
 * <ol>
 * <li>Take the next {@code key} from a queue.</li>
 * <li>Get the {@link VolatileCacheEntry} with that {@code key} from a cache (if
 * it exists).</li>
 * <li>{@link VolatileCacheEntry#loadIfNotValid() load} the entry's data (unless
 * it is already loaded).</li>
 * </ol>
 * {@link FetcherThreads} are employed by {@link LoadingVolatileCache} to
 * asynchronously load data.
 *
 * TODO add shutdown() method
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FetcherThreads
{
	private final ArrayList< Fetcher > fetchers;

	public FetcherThreads(
			final WeakSoftCache< ?, ? extends VolatileCacheEntry > cache,
			final BlockingFetchQueues< ? > queue,
			final int numFetcherThreads )
	{
		this( cache, queue, numFetcherThreads, i -> String.format( "Fetcher-%d", i ) );
	}

	/**
	 *
	 * @param cache the cache that contains entries to load.
	 * @param queue the queue from which request keys are taken.
	 * @param numFetcherThreads how many parallel fetcher threads to start.
	 * @param threadIndexToName a function for naming fetcher threads (takes an index and returns a name).
	 */
	public FetcherThreads(
			final WeakSoftCache< ?, ? extends VolatileCacheEntry > cache,
			final BlockingFetchQueues< ? > queue,
			final int numFetcherThreads,
			final IntFunction< String > threadIndexToName )
	{
		fetchers = new ArrayList<>( numFetcherThreads );
		for ( int i = 0; i < numFetcherThreads; ++i )
		{
			final Fetcher f = new Fetcher( cache, queue );
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

	static class Fetcher extends Thread
	{
		private final WeakSoftCache< ?, ? extends VolatileCacheEntry > cache;

		private final BlockingFetchQueues< ? > queue;

		private final Object lock = new Object();

		private volatile long pauseUntilTimeMillis = 0;

		public Fetcher(
				final WeakSoftCache< ?, ? extends VolatileCacheEntry > cache,
				final BlockingFetchQueues< ? > queue )
		{
			this.cache = cache;
			this.queue = queue;
		}

		@Override
		public final void run()
		{
			Object key = null;
			while ( true )
			{
				while ( key == null )
					try
					{
						key = queue.take();
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
					final VolatileCacheEntry entry = cache.get( key );
					if ( entry != null )
						entry.loadIfNotValid();
					key = null;
				}
				catch ( final InterruptedException e )
				{}
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
