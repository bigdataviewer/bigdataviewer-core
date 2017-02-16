package bdv.img.gencache;

import java.util.concurrent.ExecutionException;

import net.imglib2.cache.LoadingCache;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.list.AbstractLongListImg;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

public class CachedCellImg< T extends NativeType< T >, A >
		extends AbstractCellImg< T, A, Cell< A >, CachedCellImg.CachedCells< A > >
{
	public CachedCellImg( final CellGrid grid, final LoadingCache< Long, Cell< A > > cache, final Fraction entitiesPerPixel )
	{
		super( grid, new CachedCells<>( grid, cache ), entitiesPerPixel );
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

	static class CachedCells< A > extends AbstractLongListImg< Cell< A > >
	{
		final LoadingCache< Long, Cell< A > > cache;

		protected CachedCells(
				final CellGrid grid, final LoadingCache< Long, Cell< A > > cache )
		{
			super( grid.getGridDimensions() );
			this.cache = cache;
		}

		@Override
		protected Cell< A > get( final long index )
		{
			try
			{
				return cache.get( index );
			}
			catch ( final ExecutionException e )
			{
				throw new RuntimeException( e );
			}
		}

		@Override
		protected void set( final long index, final Cell< A > value )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public ImgFactory< Cell< A > > factory()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Img< Cell< A > > copy()
		{
			throw new UnsupportedOperationException();
		}
	}
}
