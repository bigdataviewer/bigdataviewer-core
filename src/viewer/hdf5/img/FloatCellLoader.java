package viewer.hdf5.img;

import static viewer.hdf5.Reorder.reorder;

import java.io.File;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import viewer.hdf5.Reorder;
import viewer.hdf5.img.Hdf5Cell.CellLoader;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class FloatCellLoader implements CellLoader< FloatArray >
{
	final IHDF5Reader hdf5Reader;

	public FloatCellLoader( final File hdf5CellsFile )
	{
		hdf5Reader = HDF5Factory.openForReading( hdf5CellsFile );
	}

	@Override
	public FloatArray loadCell( final int[] dimensions, final long[] min, final int entitiesPerPixel )
	{
		// TODO: use entitiesPerPixel!
		final MDFloatArray array = hdf5Reader.readFloatMDArrayBlockWithOffset( "cells", reorder( dimensions ), Reorder.reorder( min ) );
		return new FloatArray( array.getAsFlatArray() );
	}
}
