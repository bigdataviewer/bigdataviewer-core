package bdv.img.cache;

import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import bdv.img.cache.VolatileImgCells.CellCache;

final public class CachedCellImg< T extends NativeType< T >, A extends VolatileAccess >
		extends AbstractCellImg< T, A, VolatileCell< A >, CellImgFactory< T > >
{
	private final CellCache< A > cache;

	public CachedCellImg( final VolatileImgCells< A > cells )
	{
		super( null, cells );
		this.cache = cells.cache;
	}

	public void setCacheHints( final CacheHints cacheHints )
	{
		cache.setCacheHints( cacheHints );
	}

	@Override
	public CachedCellImg< T, A > copy()
	{
		throw new UnsupportedOperationException();
	}
}
