package viewer.hdf5;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static viewer.hdf5.Util.reorder;

import java.io.File;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Element;

import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5GlobalCellCache;
import viewer.hdf5.img.Hdf5ImgCells;
import viewer.hdf5.img.ShortArrayLoader;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5ImageLoader implements ImgLoader
{
	IHDF5Reader hdf5Reader = null;

	Hdf5GlobalCellCache< ShortArray > cache = null;

	double[][] mipmapResolutions = null;

	@Override
	public void init( final Element elem, final File basePath )
	{
		String path;
		try
		{
			path = loadPath( elem, "hdf5", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
		hdf5Reader = HDF5Factory.openForReading( new File( path ) );
		mipmapResolutions = hdf5Reader.readDoubleMatrix( "resolutions" );
		final int numTimepoints = hdf5Reader.readInt( "numTimepoints" );
		final int numSetups = hdf5Reader.readInt( "numSetups" );
		final int numLevels = mipmapResolutions.length;
		cache = new Hdf5GlobalCellCache< ShortArray >( new ShortArrayLoader( hdf5Reader ), numTimepoints, numSetups, numLevels );
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "currently not used" );
	}

	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		return getUnsignedShortImage( view, 0 );
	}

	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view, final int level )
	{
		if ( hdf5Reader == null )
			throw new RuntimeException( "no hdf5 file open" );

		synchronized ( hdf5Reader )
		{
			final String cellsPath = Util.getCellsPath( view, level );
//			System.out.println( "loading " + cellsPath );
			final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( cellsPath );
			final long[] dimensions = reorder( info.getDimensions() );
			final int[] cellDimensions = reorder( info.tryGetChunkSizes() );

			final Hdf5GlobalCellCache< ShortArray >.Hdf5CellCache c = cache.new Hdf5CellCache( view.getTimepointIndex(), view.getSetupIndex(), level );
			final Hdf5ImgCells< ShortArray > cells = new Hdf5ImgCells< ShortArray >( c, 1, dimensions, cellDimensions );
			final CellImgFactory< UnsignedShortType > factory = null;
			final CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > > img = new CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > >( factory, cells );
			final UnsignedShortType linkedType = new UnsignedShortType( img );
			img.setLinkedType( linkedType );

			return new ImgPlus< UnsignedShortType >( img );
		}
	}

	public Hdf5GlobalCellCache< ShortArray > getCache()
	{
		return cache;
	}

	public double[][] getMipmapResolutions()
	{
		return mipmapResolutions;
	}

	public int numMipmapLevels()
	{
		return mipmapResolutions.length;
	}
}
