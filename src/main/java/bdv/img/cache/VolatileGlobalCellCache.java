/*
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
package bdv.img.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.revised.Cache;
import bdv.cache.revised.SoftRefCache;
import bdv.cache.revised.VolatileCache;
import bdv.cache.revised.VolatileLoader;
import bdv.cache.revised.WeakRefVolatileCache;
import bdv.cache.util.BlockingFetchQueues;
import bdv.cache.util.FetcherThreads;
import bdv.img.cache.VolatileImgCells.CellCache;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;

public class VolatileGlobalCellCache implements CacheControl
{
	/**
	 * Key for a cell identified by timepoint, setup, level, and index
	 * (flattened spatial coordinate).
	 */
	public static class Key
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private final long index;

		/**
		 * Create a Key for the specified cell. Note that {@code cellDims} and
		 * {@code cellMin} are not used for {@code hashcode()/equals()}.
		 *
		 * @param timepoint
		 *            timepoint coordinate of the cell
		 * @param setup
		 *            setup coordinate of the cell
		 * @param level
		 *            level coordinate of the cell
		 * @param index
		 *            index of the cell (flattened spatial coordinate of the
		 *            cell)
		 */
		public Key( final int timepoint, final int setup, final int level, final long index )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.index = index;

			int value = Long.hashCode( index );
			value = 31 * value + level;
			value = 31 * value + setup;
			value = 31 * value + timepoint;
			hashcode = value;
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof VolatileGlobalCellCache.Key ) )
				return false;
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

	private final BlockingFetchQueues< Callable< ? > > queue;

	private final FetcherThreads fetchers;

	protected final Cache< Key, VolatileCell< ? > > backingCache;

	protected final VolatileCache< Key, VolatileCell< ? > > volatileCache; // TODO rename

	/**
	 * @param maxNumLevels
	 *            the highest occurring mipmap level plus 1.
	 * @param numFetcherThreads
	 */
	public VolatileGlobalCellCache( final int maxNumLevels, final int numFetcherThreads )
	{
		queue = new BlockingFetchQueues<>( maxNumLevels );
		fetchers = new FetcherThreads( queue, numFetcherThreads );
		backingCache = new SoftRefCache<>();
		volatileCache = new WeakRefVolatileCache<>( backingCache, queue );
	}

	/**
	 * pause all fetcher threads for the specified number of milliseconds.
	 */
	// TODO remove on next opportunity (when API is broken anyways...)
	public void pauseFetcherThreadsFor( final long ms )
	{
		fetchers.pauseFor( ms );
	}

	/**
	 * Prepare the cache for providing data for the "next frame":
	 * <ul>
	 * <li>Move pending cell request to the prefetch queue (
	 * {@link BlockingFetchQueues#clearToPrefetch()}).
	 * <li>Perform pending cache maintenance operations (
	 * {@link WeakSoftCache#cleanUp()}).
	 * <li>Increment the internal frame counter, which will enable previously
	 * enqueued requests to be enqueued again for the new frame.
	 * </ul>
	 */
	@Override
	public void prepareNextFrame()
	{
		queue.clearToPrefetch();
	}

	/**
	 * Remove all references to loaded data as well as all enqueued requests
	 * from the cache.
	 */
	public void clearCache()
	{
		volatileCache.invalidateAll();
		backingCache.invalidateAll();
		queue.clear();
	}

	/**
	 * <em>For internal use.</em>
	 * <p>
	 * Get the {@link LoadingVolatileCache} that handles cell loading. This is
	 * used by bigdataviewer-server to directly issue Cell requests without
	 * having {@link CellImg}s and associated {@link VolatileCellCache}s.
	 *
	 * @return the cache that handles cell loading
	 */
	// TODO
//	public LoadingVolatileCache< Key, VolatileCell< ? > > getLoadingVolatileCache()
//	{
//		return volatileCache;
//	}

	/**
	 * A {@link VolatileCacheValueLoader} for one specific {@link VolatileCell}.
	 */
	public static class VolatileCellLoader< A extends VolatileAccess > implements VolatileLoader< VolatileCell< A > >
	{
		private final CacheArrayLoader< A > cacheArrayLoader;

		private final int timepoint;

		private final int setup;

		private final int level;

		private final int[] cellDims;

		private final long[] cellMin;

		/**
		 * Create a loader for a specific cell.
		 *
		 * @param cacheArrayLoader
		 *            loads cell data
		 * @param timepoint
		 *            timepoint coordinate of the cell
		 * @param setup
		 *            setup coordinate of the cell
		 * @param level
		 *            level coordinate of the cell
		 * @param cellDims
		 *            dimensions of the cell in pixels
		 * @param cellMin
		 *            minimum spatial coordinates of the cell in pixels
		 */
		public VolatileCellLoader(
				final CacheArrayLoader< A > cacheArrayLoader,
				final int timepoint,
				final int setup,
				final int level,
				final int[] cellDims,
				final long[] cellMin
				)
		{
			this.cacheArrayLoader = cacheArrayLoader;
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.cellDims = cellDims;
			this.cellMin = cellMin;
		}

		@Override
		public VolatileCell< A > call() throws Exception
		{
			return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
		}

		@Override
		public VolatileCell< A > createInvalid()
		{
			return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.emptyArray( cellDims ) );
		}
	}

	/**
	 * A {@link CellCache} that forwards to the {@link VolatileGlobalCellCache}.
	 *
	 * @param <A>
	 *
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	public class VolatileCellCache< A extends VolatileAccess > implements CellCache< A >
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private CacheHints cacheHints;

		private final CacheArrayLoader< A > cacheArrayLoader;

		public VolatileCellCache( final int timepoint, final int setup, final int level, final CacheHints cacheHints, final CacheArrayLoader< A > cacheArrayLoader )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.cacheHints = cacheHints;
			this.cacheArrayLoader = cacheArrayLoader;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public VolatileCell< A > get( final long index )
		{
			final Key key = new Key( timepoint, setup, level, index );
			try
			{
				return ( VolatileCell< A > ) volatileCache.getIfPresent( key, cacheHints );
			}
			catch ( final ExecutionException e )
			{
				throw new RuntimeException( e );
			}
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public VolatileCell< A > load( final long index, final int[] cellDims, final long[] cellMin )
		{
			final Key key = new Key( timepoint, setup, level, index );
			final VolatileCellLoader< A > loader = new VolatileCellLoader<>( cacheArrayLoader, timepoint, setup, level, cellDims, cellMin );
			try
			{
				return ( VolatileCell< A > ) volatileCache.get( key, loader, cacheHints );
			}
			catch ( final ExecutionException e )
			{
				throw new RuntimeException( e );
			}
		}

		@Override
		public void setCacheHints( final CacheHints cacheHints )
		{
			this.cacheHints = cacheHints;
		}
	}
}
