package viewer.hdf5;

import static viewer.hdf5.Reorder.reorder;

import java.io.File;

import mpicbg.tracking.data.View;
import mpicbg.tracking.data.io.ImgLoader;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Element;

import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5Cell.CellLoader;
import viewer.hdf5.img.Hdf5ImgCells;
import viewer.hdf5.img.ShortCellLoader;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5ImageLoader implements ImgLoader
{
	IHDF5Reader hdf5Reader = null;

	@Override
	public void init( final Element elem )
	{
		final String path = elem.getElementsByTagName( "hdf5" ).item( 0 ).getTextContent();
		hdf5Reader = HDF5Factory.openForReading( new File( path ) );
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "currently not used" );

		/*
		if ( hdf5Reader == null )
			throw new RuntimeException( "no hdf5 file open" );

		synchronized ( hdf5Reader )
		{
			final String cellsPath = CreateCells.getCellsPath( view );
			System.out.println( "loading " + cellsPath );
			final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( cellsPath );
			final long[] dimensions = reorder( info.getDimensions() );
			final int[] cellDimensions = reorder( info.tryGetChunkSizes() );

			final CellLoader< FloatArray > loader = new FloatCellLoader( hdf5Reader, cellsPath );
			final Hdf5ImgCells< FloatArray > cells = new Hdf5ImgCells< FloatArray >( loader, 1, dimensions, cellDimensions );
			final CellImgFactory< FloatType > factory = null;
			final CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > > img = new CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > >( factory, cells );
			final FloatType linkedType = new FloatType( img );
			img.setLinkedType( linkedType );

			return new ImgPlus< FloatType >( img );
		}
		*/
	}

	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		if ( hdf5Reader == null )
			throw new RuntimeException( "no hdf5 file open" );

		synchronized ( hdf5Reader )
		{
			final String cellsPath = CreateCells.getCellsPath( view );
			System.out.println( "loading " + cellsPath );
			final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( cellsPath );
			final long[] dimensions = reorder( info.getDimensions() );
			final int[] cellDimensions = reorder( info.tryGetChunkSizes() );

			final CellLoader< ShortArray > loader = new ShortCellLoader( hdf5Reader, cellsPath );
			final Hdf5ImgCells< ShortArray > cells = new Hdf5ImgCells< ShortArray >( loader, 1, dimensions, cellDimensions );
			final CellImgFactory< UnsignedShortType > factory = null;
			final CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > > img = new CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > >( factory, cells );
			final UnsignedShortType linkedType = new UnsignedShortType( img );
			img.setLinkedType( linkedType );

			return new ImgPlus< UnsignedShortType >( img );
		}
	}

}
