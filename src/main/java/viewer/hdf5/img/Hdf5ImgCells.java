package viewer.hdf5.img;

import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.AbstractCells;
import net.imglib2.img.list.AbstractListImg;
import net.imglib2.util.IntervalIndexer;

public class Hdf5ImgCells< A extends VolatileAccess > extends AbstractCells< A, Hdf5Cell< A >, Hdf5ImgCells< A >.CachedCells >
{
	public static interface CellCache< A extends VolatileAccess >
	{
		/**
		 * Get the cell at a specified index.
		 *
		 * @return cell at index or null if the cell is not in the cache.
		 */
		public Hdf5Cell< A > get( final int index );

		/**
		 * Load a cell into memory (eventually) and put it into the cache at the
		 * specified index. Depending on the implementation, loading may be
		 * asynchronous, so the {@link VolatileAccess} of the returned cell may
		 * be invalid for a while.
		 *
		 * @param index
		 *            cell is stored at this index in the cache.
		 * @param cellDims
		 *            dimensions of the cell.
		 * @param cellMin
		 *            offset of the cell in image coordinates.
		 * @return cell at index
		 */
		public Hdf5Cell< A > load( final int index, final int[] cellDims, final long[] cellMin );
	}

	protected final CachedCells cells;

	protected final CellCache< A > cache;

	public Hdf5ImgCells( final CellCache< A > cache, final int entitiesPerPixel, final long[] dimensions, final int[] cellDimensions )
	{
		super( entitiesPerPixel, dimensions, cellDimensions );
		this.cache = cache;
		cells = new CachedCells( numCells );
	}

	@Override
	protected CachedCells cells()
	{
		return cells;
	}

	public class CachedCells extends AbstractListImg< Hdf5Cell< A > >
	{
		protected CachedCells( final long[] dim )
		{
			super( dim );
		}

		@Override
		protected Hdf5Cell< A > get( final int index )
		{
			final Hdf5Cell< A > cell = cache.get( index );
			if ( cell != null )
				return cell;
			final long[] cellGridPosition = new long[ n ];
			final long[] cellMin = new long[ n ];
			final int[] cellDims  = new int[ n ];
			IntervalIndexer.indexToPosition( index, dim, cellGridPosition );
			getCellDimensions( cellGridPosition, cellMin, cellDims );
			return cache.load( index, cellDims, cellMin );
		}

		@Override
		public Img< Hdf5Cell< A > > copy()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		protected void set( final int index, final Hdf5Cell< A > value )
		{
			throw new UnsupportedOperationException( "Not supported" );
		}
	}
}
