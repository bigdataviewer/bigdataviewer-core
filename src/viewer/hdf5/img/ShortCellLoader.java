package viewer.hdf5.img;

import static viewer.hdf5.Reorder.reorder;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import viewer.hdf5.Reorder;
import viewer.hdf5.img.Hdf5Cell.CellLoader;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class ShortCellLoader implements CellLoader< ShortArray >
{
	final IHDF5Reader hdf5Reader;

	final String cellsPath;

	public ShortCellLoader( final IHDF5Reader hdf5Reader, final String cellsPath )
	{
		this.hdf5Reader = hdf5Reader;
		this.cellsPath = cellsPath;
	}

	@Override
	public ShortArray loadCell( final int[] dimensions, final long[] min, final int entitiesPerPixel )
	{
		synchronized( hdf5Reader )
		{
			// TODO: use entitiesPerPixel!
			final MDShortArray array = hdf5Reader.readShortMDArrayBlockWithOffset( cellsPath, reorder( dimensions ), Reorder.reorder( min ) );
			return new ShortArray( array.getAsFlatArray() );
		}
	}
}
