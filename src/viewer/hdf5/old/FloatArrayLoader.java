package viewer.hdf5.old;

import static viewer.hdf5.Util.getCellsPath;
import static viewer.hdf5.Util.reorder;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import viewer.hdf5.Util;
import viewer.hdf5.img.Hdf5ArrayLoader;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class FloatArrayLoader implements Hdf5ArrayLoader< FloatArray >
{
	final IHDF5Reader hdf5Reader;

	public FloatArrayLoader( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
	}

	@Override
	public FloatArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
	{
		synchronized( hdf5Reader )
		{
			final MDFloatArray array = hdf5Reader.readFloatMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorder( dimensions ), Util.reorder( min ) );
			return new FloatArray( array.getAsFlatArray() );
		}
	}

	@Override
	public int getBytesPerElement() {
		return 4;
	}
}
