package bdv.img.gencache;

import java.lang.reflect.InvocationTargetException;

import bdv.img.gencache.CachedCellImg.CachedCells;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.list.AbstractLongListImg;
import net.imglib2.type.NativeType;

public class CachedCellImg< T extends NativeType< T >, A >
		extends AbstractCellImg< T, A, Cell< A >, CachedCells< Cell< A > > >
{
	@FunctionalInterface
	public interface Get< T >
	{
		T get( long index );
	}

	public CachedCellImg( final CellGrid grid, final T type, final Get< Cell< A > > get )
	{
		super( grid, new CachedCells<>( grid.getGridDimensions(), get ), type.getEntitiesPerPixel() );
		try
		{
			VolatileCachedCellImg.linkType( type, this );
		}
		catch ( NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e )
		{
			throw new RuntimeException( e );
		}
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

	public static final class CachedCells< T > extends AbstractLongListImg< T >
	{
		private final Get< T > get;

		protected CachedCells( final long[] dimensions, final Get< T > get )
		{
			super( dimensions );
			this.get = get;
		}

		@Override
		protected T get( final long index )
		{
			return get.get( index );
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
}
