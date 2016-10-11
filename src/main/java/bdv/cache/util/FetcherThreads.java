package bdv.cache.util;

import java.util.ArrayList;
import java.util.function.IntFunction;

import bdv.cache.LoadingVolatileCache;
import bdv.cache.VolatileCacheValue;
import bdv.cache.WeakSoftCache;

/**
 * A set of threads that load {@link VolatileCacheValue}s. Each thread does the
 * following in a loop:
 * <ol>
 * <li>Take the next {@code key} from a queue.</li>
 * <li>Get the {@link Loadable} with that {@code key} from a cache (if
 * it exists).</li>
 * <li>{@link Loadable#loadIfNotValid() load} the entry's data (unless
 * it is already loaded).</li>
 * </ol>
 * {@link FetcherThreads} are employed by {@link LoadingVolatileCache} to
 * asynchronously load data.
 *
 * <p>
 * TODO Add shutdown() method.
 *
 * <p>
 * TODO This uses {@code WeakSoftCache<?,? extends VolatileCacheEntry>} only for {@code get()}, could be replaced with something less restrictive?
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FetcherThreads
{
	/**
	 * Something that can be loaded.
	 * The assumption is that this {@link #loadIfNotValid()},
	 * the value can be made valid.
	 *
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	public interface Loadable
	{
		/**
		 * If this entry's value is not currently valid, then load it. After the
		 * method returns, the value is guaranteed to be valid.
		 * <p>
		 * This must be implemented in a thread-safe manner. Multiple threads
		 * are allowed to call this method at the same time. The expected
		 * behaviour is that the value is loaded only once and the result is
		 * visible on all threads.
		 * <p>
		 * Note, that loading may be implemented either as
		 * <ol>
		 * <li>modify the existing value and change its state to valid, or</li>
		 * <li>replace the existing value by a valid one (this is done in
		 * {@link LoadingVolatileCache}).</li>
		 * </ol>
		 *
		 * @throws InterruptedException
		 *             if the loading operation was interrupted.
		 */
		public void loadIfNotValid() throws InterruptedException;
	}

	private final ArrayList< Fetcher > fetchers;

	public FetcherThreads(
			final WeakSoftCache< ?, ? extends Loadable > cache,
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
			final WeakSoftCache< ?, ? extends Loadable > cache,
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
		private final WeakSoftCache< ?, ? extends Loadable > cache;

		private final BlockingFetchQueues< ? > queue;

		private final Object lock = new Object();

		private volatile long pauseUntilTimeMillis = 0;

		public Fetcher(
				final WeakSoftCache< ?, ? extends Loadable > cache,
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
					final Loadable entry = cache.get( key );
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
