package bdv.img.cache;

import java.util.ArrayList;

import bdv.cache.VolatileCacheEntry;
import bdv.cache.WeakSoftCache;

/**
 * TODO javadoc
 * TODO add constructor with format string for thread names
 * TODO add start() method
 * TODO add shutdown() method
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class FetcherThreads
{
	private final ArrayList< Fetcher > fetchers;

	public FetcherThreads(
			final WeakSoftCache< ?, ? extends VolatileCacheEntry< ?, ? > > cache,
			final BlockingFetchQueues< ? > queue,
			final int numFetcherThreads )
	{
		fetchers = new ArrayList<>( numFetcherThreads );
		for ( int i = 0; i < numFetcherThreads; ++i )
		{
			final Fetcher f = new Fetcher( cache, queue );
			f.setDaemon( true );
			f.setName( "Fetcher-" + i );
			fetchers.add( f );
			f.start();
		}
	}

	/**
	 * pause all {@link Fetcher} threads for the specified number of milliseconds.
	 */
	public void pauseFetcherThreadsFor( final long ms )
	{
		pauseFetcherThreadsUntil( System.currentTimeMillis() + ms );
	}

	/**
	 * pause all {@link Fetcher} threads until the given time (see
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
		private final WeakSoftCache< ?, ? extends VolatileCacheEntry< ?, ? > > cache;

		private final BlockingFetchQueues< ? > queue;

		private final Object lock = new Object();

		private volatile long pauseUntilTimeMillis = 0;

		public Fetcher(
				final WeakSoftCache< ?, ? extends VolatileCacheEntry< ?, ? > > cache,
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
					final VolatileCacheEntry< ?, ? > entry = cache.get( key );
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
