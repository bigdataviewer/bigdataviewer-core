/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import java.util.function.Function;

import bdv.cache.CacheControl;
import bdv.img.cache.VolatileCachedCellImg.VolatileCachedCells;
import net.imglib2.Volatile;
import net.imglib2.cache.iotiming.IoTimeBudget;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.cache.volatiles.VolatileCache;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.img.list.AbstractLongListImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;

/**
 * A {@link LazyCellImg} for {@link Volatile} accesses. The only difference to
 * {@link LazyCellImg} is that is has {@link CacheHints}.
 *
 * @param <T>
 *            the pixel type
 * @param <A>
 *            the underlying native access type
 *
 * @author Tobias Pietzsch
 * @author Stephan Saalfeld
 * @author Philipp Hanslovsky
 */
public class VolatileCachedCellImg< T extends NativeType< T >, A extends DataAccess >
		extends AbstractCellImg< T, A, Cell< A >, VolatileCachedCells< Cell< A > > >
{
	private final VolatileCache< Long, Cell< A > > cache;

	public VolatileCachedCellImg(
			final CellGrid grid,
			final T type,
			final CacheHints cacheHints,
			final VolatileCache< Long, Cell< A > > cache )
	{
		super( grid, new VolatileCachedCells<>( grid.getGridDimensions(), cache.unchecked()::get, cacheHints ), type.getEntitiesPerPixel() );

		this.cache = cache;

		@SuppressWarnings( "unchecked" )
		final NativeTypeFactory< T, ? super A > typeFactory = ( NativeTypeFactory< T, ? super A > ) type.getNativeTypeFactory();
		setLinkedType( typeFactory.createLinkedType( this ) );
	}

	public VolatileCache< Long, Cell< A > > getCache()
	{
		return cache;
	}

	/**
	 * Set {@link CacheHints hints} on how to handle cell requests for this
	 * cache. The hints comprise {@link LoadingStrategy}, queue priority, and
	 * queue order.
	 * <p>
	 * Whenever a cell is accessed its data may be invalid, meaning that the
	 * cell data has not been loaded yet. In this case, the
	 * {@link LoadingStrategy} determines when the data should be loaded:
	 * <ul>
	 * <li>{@link LoadingStrategy#VOLATILE}: Enqueue the cell for asynchronous
	 * loading by a fetcher thread, if it has not been enqueued in the current
	 * frame already.
	 * <li>{@link LoadingStrategy#BLOCKING}: Load the cell data immediately.
	 * <li>{@link LoadingStrategy#BUDGETED}: Load the cell data immediately if
	 * there is enough {@link IoTimeBudget} left for the current thread group.
	 * Otherwise enqueue for asynchronous loading, if it has not been enqueued
	 * in the current frame already.
	 * <li>{@link LoadingStrategy#DONTLOAD}: Do nothing.
	 * </ul>
	 * <p>
	 * If a cell is enqueued, it is enqueued in the queue with the specified
	 * {@link CacheHints#getQueuePriority() queue priority}. Priorities are
	 * consecutive integers <em>0 ... n-1</em>, where 0 is the highest priority.
	 * Requests with priority <em>i &lt; j</em> will be handled before requests
	 * with priority <em>j</em>.
	 * <p>
	 * Finally, the {@link CacheHints#isEnqueuToFront() queue order} determines
	 * whether the cell is enqueued to the front or to the back of the queue
	 * with the specified priority.
	 * <p>
	 * Note, that the queues are {@link BlockingFetchQueues#clearToPrefetch()
	 * cleared} whenever a {@link CacheControl#prepareNextFrame() new frame} is
	 * rendered.
	 *
	 * @param cacheHints
	 *            describe handling of cell requests for this cache. May be
	 *            {@code null}, in which case the default hints are restored.
	 */
	public void setCacheHints( final CacheHints cacheHints )
	{
		cells.cacheHints = ( cacheHints != null ) ? cacheHints : cells.defaultCacheHints;
	}

	public CacheHints getDefaultCacheHints()
	{
		return cells.defaultCacheHints;
	}

	@Override
	public ImgFactory< T > factory()
	{
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@Override
	public Img< T > copy()
	{
		throw new UnsupportedOperationException( "not implemented yet" );
	}

	@FunctionalInterface
	public interface Get< T >
	{
		T get( long index, CacheHints cacheHints );
	}

	public static final class VolatileCachedCells< T > extends AbstractLongListImg< T >
	{
		private final Get< T > get;

		final CacheHints defaultCacheHints;

		CacheHints cacheHints;

		protected VolatileCachedCells( final long[] dimensions, final Get< T > get, final CacheHints cacheHints )
		{
			super( dimensions );
			this.get = get;
			this.defaultCacheHints = cacheHints;
			this.cacheHints = cacheHints;
		}

		@Override
		protected T get( final long index )
		{
			return get.get( index, cacheHints );
		}

		@Override
		protected void set( final long index, final T value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public ImgFactory< T > factory()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Img< T > copy()
		{
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * @deprecated Construct with a {@code VolatileCache} using {@link #VolatileCachedCellImg(CellGrid, NativeType, CacheHints, VolatileCache)}.
	 */
	@Deprecated
	public VolatileCachedCellImg(
			final CellGrid grid,
			final Fraction entitiesPerPixel,
			final Function< NativeImg< T, ? super A >, T > typeFactory,
			final CacheHints cacheHints,
			final Get< Cell< A > > get )
	{
		super( grid, new VolatileCachedCells<>( grid.getGridDimensions(), get, cacheHints ), entitiesPerPixel );
		this.cache = null;
		setLinkedType( typeFactory.apply( this ) );
	}

	/**
	 * @deprecated Construct with a {@code VolatileCache} using {@link #VolatileCachedCellImg(CellGrid, NativeType, CacheHints, VolatileCache)}.
	 */
	@Deprecated
	public VolatileCachedCellImg(
			final CellGrid grid,
			final T type,
			final CacheHints cacheHints,
			final Get< Cell< A > > get )
	{
		super( grid, new VolatileCachedCells<>( grid.getGridDimensions(), get, cacheHints ), type.getEntitiesPerPixel() );
		this.cache = null;
		@SuppressWarnings( "unchecked" )
		final NativeTypeFactory< T, ? super A > typeFactory = ( NativeTypeFactory< T, ? super A > ) type.getNativeTypeFactory();
		setLinkedType( typeFactory.createLinkedType( this ) );
	}
}
