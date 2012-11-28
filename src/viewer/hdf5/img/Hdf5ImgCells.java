package viewer.hdf5.img;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCells;
import net.imglib2.img.list.AbstractListImg;
import net.imglib2.util.IntervalIndexer;
import viewer.hdf5.img.Hdf5Cell.CellLoader;

public class Hdf5ImgCells< A > extends AbstractCells< A, Hdf5Cell< A >, Hdf5ImgCells< A >.Hdf5Cache >
{
	protected final Hdf5Cache cells;

	public Hdf5ImgCells( final CellLoader< A > creator, final int entitiesPerPixel, final long[] dimensions, final int[] cellDimensions )
	{
		super( entitiesPerPixel, dimensions, cellDimensions );
		cells = new Hdf5Cache( creator, numCells );
	}

	@Override
	protected Hdf5Cache cells()
	{
		return cells;
	}

	public class Hdf5Cache extends AbstractListImg< Hdf5Cell< A > >
	{
		protected final CellLoader< A > creator;

		protected final HashMap< Integer, WeakReference< Hdf5Cell< A > > > cache;

		protected Hdf5Cache( final CellLoader< A > creator, final long[] dim )
		{
			super( dim );
			this.creator = creator;
			cache = new HashMap< Integer, WeakReference< Hdf5Cell< A > > >();
		}

		@Override
		public Img< Hdf5Cell< A >> copy()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		protected Hdf5Cell< A > getPixel( final int index )
		{
			if( cache.containsKey( index ) )
			{
				final WeakReference< Hdf5Cell< A > > ref = cache.get( index );
				final Hdf5Cell< A > cell = ref.get();
				if ( cell != null )
					return cell;
			}
			final long[] cellGridPosition = new long[ n ];
			final long[] cellMin = new long[ n ];
			final int[] cellDims  = new int[ n ];
			IntervalIndexer.indexToPosition( index, dim, cellGridPosition );
			getCellDimensions( cellGridPosition, cellMin, cellDims );
			final Hdf5Cell< A > cell = new Hdf5Cell< A >( creator, cellDims, cellMin, entitiesPerPixel );
			cache.put( index, new WeakReference< Hdf5Cell< A > >( cell ) );
			return cell;
		}

		@Override
		protected void setPixel( final int index, final Hdf5Cell< A > value )
		{
			throw new UnsupportedOperationException( "Not supported" );
		}
	}
}
