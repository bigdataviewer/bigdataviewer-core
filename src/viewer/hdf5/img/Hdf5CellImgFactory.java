package viewer.hdf5.img;

import static viewer.hdf5.Reorder.reorder;

import java.io.File;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import viewer.hdf5.img.Hdf5Cell.CellLoader;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5CellImgFactory
{
	public static CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > > create( final File hdf5CellsFile )
	{
		final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5CellsFile );
		final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( "cells" );
		final long[] dimensions = reorder( info.getDimensions() );
		final int[] cellDimensions = reorder( info.tryGetChunkSizes() );
		hdf5Reader.close();

		final CellLoader< FloatArray > loader = new FloatCellLoader( hdf5CellsFile );
		final Hdf5ImgCells< FloatArray > cells = new Hdf5ImgCells< FloatArray >( loader, 1, dimensions, cellDimensions );
		final CellImgFactory< FloatType > factory = null;
		final CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > > img = new CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > >( factory, cells );
		final FloatType linkedType = new FloatType( img );
		img.setLinkedType( linkedType );

		return img;
	}
}
