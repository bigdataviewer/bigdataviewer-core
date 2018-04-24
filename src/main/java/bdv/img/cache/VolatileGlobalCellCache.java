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

import bdv.cache.CacheControl;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.cache.ref.WeakRefVolatileCache;
import net.imglib2.cache.util.KeyBimap;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.UncheckedVolatileCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;

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
			return ( this.index == that.index ) && ( this.timepoint == that.timepoint ) && ( this.setup == that.setup ) && ( this.level == that.level );
		}

		final int hashcode;

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	private final BlockingFetchQueues< Callable< ? > > queue;

	protected final LoaderCache< Key, Cell< ? > > backingCache;

	/**
	 * Create a new global cache with a new fetch queue served by the specified
	 * number of fetcher threads.
	 *
	 * @param maxNumLevels
	 *            the highest occurring mipmap level plus 1.
	 * @param numFetcherThreads
	 *            how many threads should be created to load data.
	 */
	public VolatileGlobalCellCache( final int maxNumLevels, final int numFetcherThreads )
	{
		queue = new BlockingFetchQueues<>( maxNumLevels );
		new FetcherThreads( queue, numFetcherThreads );
		backingCache = new SoftRefLoaderCache<>();
	}

	/**
	 * Create a new global cache with the specified fetch queue. (It is the
	 * callers responsibility to create fetcher threads that serve the queue.)
	 *
	 * @param queue
	 *            queue to which asynchronous data loading jobs are submitted
	 */
	public VolatileGlobalCellCache( final BlockingFetchQueues< Callable< ? > > queue )
	{
		this.queue = queue;
		backingCache = new SoftRefLoaderCache<>();
	}

	/**
	 * Prepare the cache for providing data for the "next frame",
	 * by moving pending cell request to the prefetch queue
	 * ({@link BlockingFetchQueues#clearToPrefetch()}).
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
		backingCache.invalidateAll();
		queue.clear();
		backingCache.invalidateAll();
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
//	public LoadingVolatileCache< Key, Cell< ? > > getLoadingVolatileCache()
//	{
//		return volatileCache;
//	}

	/**
	 * Create a {@link VolatileCachedCellImg} backed by this {@link VolatileGlobalCellCache},
	 * using the provided {@link CacheArrayLoader} to load data.
	 *
	 * @param grid
	 * @param timepoint
	 * @param setup
	 * @param level
	 * @param cacheHints
	 * @param cacheArrayLoader
	 * @param type
	 * @return
	 */
	public < T extends NativeType< T >, A > VolatileCachedCellImg< T, A > createImg(
			final CellGrid grid,
			final int timepoint,
			final int setup,
			final int level,
			final CacheHints cacheHints,
			final CacheArrayLoader< A > cacheArrayLoader,
			final T type )
	{
		final CacheLoader< Long, Cell< ? > > loader = new CacheLoader< Long, Cell< ? > >()
		{
			@Override
			public Cell< A > get( final Long key ) throws Exception
			{
				final int n = grid.numDimensions();
				final long[] cellMin = new long[ n ];
				final int[] cellDims = new int[ n ];
				grid.getCellDimensions( key, cellMin, cellDims );
				return new Cell<>(
						cellDims,
						cellMin,
						cacheArrayLoader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
			}
		};

		final KeyBimap< Long, Key > bimap = KeyBimap.< Long, Key >build(
				index -> new Key( timepoint, setup, level, index ),
				key -> key.index );

		final Cache< Long, Cell< ? > > cache = backingCache
				.mapKeys( bimap )
				.withLoader( loader );

		final EmptyArrayCreator< A > emptyArrayCreator = cacheArrayLoader.getEmptyArrayCreator();
		final CreateInvalidVolatileCell< ? > createInvalid = emptyArrayCreator == null
				? CreateInvalidVolatileCell.get( grid, type, AccessFlags.setOf() )
				: new CreateInvalidVolatileCell<>( grid, type.getEntitiesPerPixel(), emptyArrayCreator );

		final UncheckedVolatileCache< Long, Cell< ? > > vcache = new WeakRefVolatileCache<>(
				cache, queue, createInvalid )
						.unchecked();

		@SuppressWarnings( "unchecked" )
		final VolatileCachedCellImg< T, A > img = new VolatileCachedCellImg<>( grid, type, cacheHints,
				( i, h ) -> ( Cell< A > ) vcache.get( i, h ) );

		return img;
	}
}
