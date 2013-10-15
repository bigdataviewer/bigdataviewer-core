package viewer.hdf5.img;

import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCells;
import net.imglib2.img.list.AbstractListImg;
import net.imglib2.util.IntervalIndexer;

public class Hdf5ImgCells< A, CA extends A > extends AbstractCells< A, Hdf5Cell< CA >, Hdf5ImgCells< A, CA >.CachedCells >
{
	public static interface CellCache< CA >
	{
		/**
		 * Get the cell at a specified index.
		 *
		 * @return cell at index or null if the cell is not in the cache.
		 */
		public Hdf5Cell< CA > get( final int index );

		/**
		 * Load a cell into memory and put it into the cache at the specified index.
		 *
		 * @param index
		 * 			  cell is stored at this index in the cache.
		 * @param cellDims
		 *            dimensions of the cell.
		 * @param cellMin
		 *            offset of the cell in image coordinates.
		 * @return cell at index
		 */
		public Hdf5Cell< CA > load( final int index, final int[] cellDims, final long[] cellMin );

	}

	protected final CachedCells cells;

	protected final CellCache< CA > cache;

	public Hdf5ImgCells( final CellCache< CA > cache, final int entitiesPerPixel, final long[] dimensions, final int[] cellDimensions )
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

	public class CachedCells extends AbstractListImg< Hdf5Cell< CA > >
	{
		protected CachedCells( final long[] dim )
		{
			super( dim );
		}

		@Override
		protected Hdf5Cell< CA > get( final int index )
		{
			final Hdf5Cell< CA > cell = cache.get( index );
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
		public Img< Hdf5Cell< CA > > copy()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		protected void set( final int index, final Hdf5Cell< CA > value )
		{
			throw new UnsupportedOperationException( "Not supported" );
		}
	}
}
