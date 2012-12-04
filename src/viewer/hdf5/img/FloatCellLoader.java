package viewer.hdf5.img;

import static viewer.hdf5.Reorder.reorder;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import viewer.hdf5.Reorder;
import viewer.hdf5.img.Hdf5Cell.CellLoader;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class FloatCellLoader implements CellLoader< FloatArray >
{
	final IHDF5Reader hdf5Reader;

	final String cellsPath;

	public FloatCellLoader( final IHDF5Reader hdf5Reader, final String cellsPath )
	{
		this.hdf5Reader = hdf5Reader;
		this.cellsPath = cellsPath;
	}

	@Override
	public FloatArray loadCell( final int[] dimensions, final long[] min, final int entitiesPerPixel )
	{
		synchronized( hdf5Reader )
		{
			// TODO: use entitiesPerPixel!
			final MDFloatArray array = hdf5Reader.readFloatMDArrayBlockWithOffset( cellsPath, reorder( dimensions ), Reorder.reorder( min ) );
			return new FloatArray( array.getAsFlatArray() );
		}
	}
}
