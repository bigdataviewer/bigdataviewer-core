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

import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.CacheIoTiming;
import bdv.cache.LoadingVolatileCache;
import bdv.cache.VolatileCacheValueLoader;
import bdv.cache.WeakSoftCache;
import bdv.cache.util.BlockingFetchQueues;
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

	protected final LoadingVolatileCache< Key, VolatileCell< ? > > volatileCache; // TODO rename

	@Deprecated
	public VolatileGlobalCellCache( final int maxNumTimepoints, final int maxNumSetups, final int maxNumLevels, final int numFetcherThreads )
	{
		this( maxNumLevels, numFetcherThreads );
	}

	/**
	 * @param maxNumLevels
	 *            the highest occurring mipmap level plus 1.
	 * @param numFetcherThreads
	 */
	public VolatileGlobalCellCache( final int maxNumLevels, final int numFetcherThreads )
	{
		volatileCache = new LoadingVolatileCache<>( maxNumLevels, numFetcherThreads );
	}

	/**
	 * pause all fetcher threads for the specified number of milliseconds.
	 */
	public void pauseFetcherThreadsFor( final long ms )
	{
		volatileCache.getFetcherThreads().pauseFetcherThreadsFor( ms );
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
		volatileCache.prepareNextFrame();
	}

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO per frame/
	 *
	 * @param partialBudget
	 *            Initial budget (in nanoseconds) for priority levels 0 through
	 *            <em>n</em>. The budget for level <em>i&gt;j</em> must always be
	 *            smaller-equal the budget for level <em>j</em>. If <em>n</em>
	 *            is smaller than the maximum number of mipmap levels, the
	 *            remaining priority levels are filled up with budget[n].
	 */
	@Override
	public void initIoTimeBudget( final long[] partialBudget )
	{
		volatileCache.initIoTimeBudget( partialBudget );
	}

	/**
	 * Get the {@link CacheIoTiming} that provides per thread-group IO
	 * statistics and budget.
	 */
	@Override
	public CacheIoTiming getCacheIoTiming()
	{
		return volatileCache.getCacheIoTiming();
	}

	/**
	 * Remove all references to loaded data as well as all enqueued requests
	 * from the cache.
	 */
	public void clearCache()
	{
		volatileCache.invalidateAll();
	}

	/**
	 * A {@link VolatileCacheValueLoader} for one specific {@link VolatileCell}.
	 */
	public static class VolatileCellLoader< A extends VolatileAccess > implements VolatileCacheValueLoader< VolatileCell< A > >
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
		public VolatileCell< A > createEmptyValue()
		{
			return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.emptyArray( cellDims ) );
		}

		@Override
		public VolatileCell< A > load() throws InterruptedException
		{
			return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
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
			return ( VolatileCell< A > ) volatileCache.getIfPresent( key, cacheHints );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public VolatileCell< A > load( final long index, final int[] cellDims, final long[] cellMin )
		{
			final Key key = new Key( timepoint, setup, level, index );
			final VolatileCellLoader< A > loader = new VolatileCellLoader<>( cacheArrayLoader, timepoint, setup, level, cellDims, cellMin );
			return ( VolatileCell< A > ) volatileCache.get( key, cacheHints, loader );
		}

		@Override
		public void setCacheHints( final CacheHints cacheHints )
		{
			this.cacheHints = cacheHints;
		}
	}
}
