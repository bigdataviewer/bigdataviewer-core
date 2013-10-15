package viewer.hdf5.old;

import static viewer.hdf5.Util.reorder;

import java.io.File;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import viewer.hdf5.Util;
import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5GlobalCellCache;
import viewer.hdf5.img.Hdf5ImgCells;
import viewer.hdf5.img.ShortArrayLoader;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5CellImgFactory
{
	public static CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > > createUnsignedShort( final File hdf5CellsFile )
	{
		final int timepoint = 0;
		final int setup = 0;
		final int level = 0;
		final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5CellsFile );
		final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( Util.getCellsPath( timepoint, setup, level ) );
		final long[] dimensions = reorder( info.getDimensions() );
		final int[] cellDimensions = reorder( info.tryGetChunkSizes() );

		final int numTimepoints = hdf5Reader.readInt( "numTimepoints" );
		final int numSetups = hdf5Reader.readInt( "numSetups" );
		final int numLevels = hdf5Reader.readDoubleMatrix( "resolutions" ).length;
		final Hdf5GlobalCellCache< ShortArray > cache = new Hdf5GlobalCellCache< ShortArray >( new ShortArrayLoader( hdf5Reader ), numTimepoints, numSetups, numLevels );
		final Hdf5GlobalCellCache< ShortArray >.Hdf5CellCache c = cache.new Hdf5CellCache( timepoint, setup, level );
		final Hdf5ImgCells< ShortArray > cells = new Hdf5ImgCells< ShortArray >( c, 1, dimensions, cellDimensions );
		final CellImgFactory< UnsignedShortType > factory = null;
		final CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > > img = new CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > >( factory, cells );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );

		return img;
	}
}
