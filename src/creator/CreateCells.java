package creator;

import static viewer.hdf5.Util.reorder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.io.ConfigurationParserException;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import org.xml.sax.SAXException;

import viewer.hdf5.Hdf5ImageLoader;
import viewer.hdf5.Util;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import creator.spim.SpimSequence;
import creator.spim.WriteSequenceToXml;
import creator.spim.imgloader.FileSequenceImageLoader;

public class CreateCells
{
	public static class MipMapDefinition
	{
	//  mipmap def 1
//		public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 1 } };
//		public static final int[][] subdivisions = { { 32, 32, 4 }, { 32, 32, 4 }, { 16, 16, 4 } };

	//  mipmap def 2
//		public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
//		public static final int[][] subdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

		public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 2 }, { 4, 4, 4 } };
		public static final int[][] subdivisions = { { 32, 32, 32 }, { 16, 16, 16 }, { 8, 8, 8 } };
	}


	public static void main( final String[] args ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, SAXException, IOException, ConfigurationParserException, TransformerFactoryConfigurationError, TransformerException
	{
		// openspim deconvolved dataset
		final String inputDirectory = "/Users/tobias/workspace/data/openspim/";
		final String inputFilePattern = "spim_TL{tt}_Angle{a}.tif";
		final String angles = "0-4";
		final String timepoints = "0-2";
		final int referenceTimePoint = 100;
		final boolean overrideImageZStretching = true;
		final double zStretching = 9.30232558139535;

		final String filepath = inputDirectory + "/output/";
		final String filepattern = "DC(l=0.0060)_t%d_ch0_%03d.tif";
		final int numSlices = 465;
		final double min = 0;
		final double max = 2;

		final SpimSequence lsmseq = new SpimSequence( inputDirectory, inputFilePattern, angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching );
		final SequenceDescription lsmdesc = lsmseq.getSequenceDescription();

		final ImgLoader sequenceLoader = new FileSequenceImageLoader( filepath, filepattern, numSlices, min, max );

		final ViewSetup[] setups = new ViewSetup[] { new ViewSetup( 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 ) };
		final SequenceDescription desc = new SequenceDescription( setups, lsmdesc.timepoints, lsmdesc.getBasePath(), sequenceLoader );

		final File seqFile = new File( "/Users/tobias/Desktop/openspim-deconvolved.xml" );
		final File hdf5File = new File( "/Users/tobias/Desktop/openspim-deconvolved.h5" );

		final int[][] resolutions = MipMapDefinition.resolutions;
		final int[][] subdivisions = MipMapDefinition.subdivisions;

		createHdf5File( desc, hdf5File, resolutions, subdivisions );

		final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File );
		final SequenceDescription sequenceDescription = new SequenceDescription( desc.setups, desc.timepoints, seqFile.getParentFile(), loader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		final AffineTransform3D transform = new AffineTransform3D();
		for ( int timepoint = 0; timepoint < desc.numTimepoints(); ++timepoint )
			registrations.add( new ViewRegistration( timepoint, 0, transform ) );
		final ViewRegistrations viewRegistrations = new ViewRegistrations( registrations, 0 );
		WriteSequenceToXml.writeSequenceToXml( sequenceDescription, viewRegistrations, seqFile.getAbsolutePath() );
	}

	public static void main2( final String[] args ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, SAXException, IOException, ConfigurationParserException, TransformerFactoryConfigurationError, TransformerException
	{
		// parhyale dataset
//		final String inputDirectory = "/Users/tobias/workspace/data/parhyale/";
//		final String inputFilePattern = "spim_TL{tt}_Angle{aaa}.lsm";
//		final String angles = "150,190,230";
//		final String timepoints = "71-90";
//		final int referenceTimePoint = 269;
//		final boolean overrideImageZStretching = true;
//		final double zStretching = 5.46448087431694;
//
//		final File seqFile = new File( "/Users/tobias/Desktop/parhyale.xml" );
//		final File hdf5File = new File( "/Users/tobias/Desktop/parhyale.h5" );

		// openspim dataset
		final String inputDirectory = "/Users/tobias/workspace/data/openspim/";
		final String inputFilePattern = "spim_TL{tt}_Angle{a}.tif";
		final String angles = "0-4";
		final String timepoints = "0-2";
		final int referenceTimePoint = 100;
		final boolean overrideImageZStretching = true;
		final double zStretching = 9.30232558139535;

		final File seqFile = new File( "/Users/tobias/Desktop/openspim.xml" );
		final File hdf5File = new File( "/Users/tobias/Desktop/openspim.h5" );

		final int[][] resolutions = MipMapDefinition.resolutions;
		final int[][] subdivisions = MipMapDefinition.subdivisions;

		final SpimSequence lsmseq = new SpimSequence( inputDirectory, inputFilePattern, angles, timepoints, referenceTimePoint, overrideImageZStretching, zStretching );
		final SequenceDescription desc = lsmseq.getSequenceDescription();
		createHdf5File( desc, hdf5File, resolutions, subdivisions );

		final Hdf5ImageLoader loader = new Hdf5ImageLoader( hdf5File );
		final SequenceDescription sequenceDescription = new SequenceDescription( desc.setups, desc.timepoints, seqFile.getParentFile(), loader );
		final ViewRegistrations viewRegistrations = lsmseq.getViewRegistrations();
		WriteSequenceToXml.writeSequenceToXml( sequenceDescription, viewRegistrations, seqFile.getAbsolutePath() );
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation. Every image is stored
	 * in multiple resolutions. The resolutions are described as int[] arrays
	 * defining multiple of original pixel size in every dimension. For example
	 * {1,1,1} is the original resolution, {4,4,2} is downsampled by factor 4 in
	 * X and Y and factor 2 in Z. Each resolution of the image is stored as a
	 * chunked three-dimensional array (each chunk corresponds to one cell of a
	 * {@link CellImg} when the data is loaded). The chunk sizes are defined by
	 * the subdivisions parameter which is an array of int[], one per
	 * resolution. Each int[] array describes the X,Y,Z chunk size for one
	 * resolution.
	 *
	 * @param seqFile
	 *            XML sequence description to be read and converted to hdf5.
	 *            (This contains number of setups and timepoints and an image
	 *            loader).
	 * @param hdf5File
	 *            hdf5 to which the image data is written
	 * @param resolutions
	 *            each int[] element of the array describes one resolution level
	 * @param subdivisions
	 */
	public static void createHdf5File( final SequenceDescription seq, final File hdf5File, final int[][] resolutions, final int[][] subdivisions )
	{
		final int numTimepoints = seq.numTimepoints();
		final int numSetups = seq.numViewSetups();
		final int numLevels = resolutions.length;

		// open HDF5 output file
		if ( hdf5File.exists() )
			hdf5File.delete();
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );

		// write Mipmap descriptions
		final double[][] dres = new double[ resolutions.length ][];
		for ( int l = 0; l < resolutions.length; ++l )
		{
			dres[ l ] = new double[ resolutions[ l ].length ];
			for ( int d = 0; d < resolutions[ l ].length; ++d )
				dres[ l ][ d ] = resolutions[ l ][ d ];
		}
		hdf5Writer.writeDoubleMatrix( "resolutions", dres );
		hdf5Writer.writeIntMatrix( "subdivisions", subdivisions );

		// write number of timepoints and setups
		hdf5Writer.writeInt( "numTimepoints", numTimepoints );
		hdf5Writer.writeInt( "numSetups", numSetups );

		// write image data for all views to the HDF5 file
		final int n = 3;
		final long[] dimensions = new long[ n ];
		for ( int timepoint = 0; timepoint < numTimepoints; ++timepoint )
		{
			System.out.println( String.format( "proccessing timepoint %d / %d", timepoint, numTimepoints ) );
			for ( int setup = 0; setup < numSetups; ++setup )
			{
				System.out.println( String.format( "proccessing setup %d / %d", setup, numSetups ) );
				// final View view = loader.getView( timepoint, setup );
				final View view = new View( seq, timepoint, setup, null );
				System.out.println( "loading image" );
				final ImgPlus< UnsignedShortType > img = seq.imgLoader.getUnsignedShortImage( view );

				for ( int level = 0; level < numLevels; ++level )
				{
					System.out.println( "writing level " + level );
					img.dimensions( dimensions );
					final RandomAccessible< UnsignedShortType > source;
					final int[] factor = resolutions[ level ];
					if ( factor[ 0 ] == 1 && factor[ 1 ] == 1 && factor[ 2 ] == 1 )
						source = img;
					else
					{
						for ( int d = 0; d < n; ++d )
							dimensions[ d ] /= factor[ d ];
						final Img< UnsignedShortType > downsampled = ArrayImgs.unsignedShorts( dimensions );
						Downsample.downsample( img, downsampled, factor );
						source = downsampled;
					}

					final int[] cellDimensions = subdivisions[ level ];
					hdf5Writer.createGroup( Util.getGroupPath( view, level ) );
					final String path = Util.getCellsPath( view, level );
					hdf5Writer.createShortMDArray( path, reorder( dimensions ), reorder( cellDimensions ), HDF5IntStorageFeatures.INT_AUTO_SCALING );

					final long[] numCells = new long[ n ];
					final int[] borderSize = new int[ n ];
					for ( int d = 0; d < n; ++d )
					{
						numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
						borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
					}

					final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( numCells );
					final long[] currentCellMin = new long[ n ];
					final long[] currentCellMax = new long[ n ];
					final long[] currentCellDim = new long[ n ];
					final long[] currentCellPos = new long[ n ];
					final long[] currentCellMinRM = new long[ n ];
					final long[] currentCellDimRM = new long[ n ];
					while ( i.hasNext() )
					{
						i.fwd();
						i.localize( currentCellPos );
						for ( int d = 0; d < n; ++d )
						{
							currentCellMin[ d ] = currentCellPos[ d ] * cellDimensions[ d ];
							currentCellDim[ d ] = ( currentCellPos[ d ] + 1 == numCells[ d ] ) ? borderSize[ d ] : cellDimensions[ d ];
							currentCellMax[ d ] = currentCellMin[ d ] + currentCellDim[ d ] - 1;
						}
						reorder( currentCellMin, currentCellMinRM );
						reorder( currentCellDim, currentCellDimRM );

						final ArrayImg< UnsignedShortType, ? > cell = ArrayImgs.unsignedShorts( currentCellDim );
						final Cursor< UnsignedShortType > c = Views.flatIterable( Views.interval( source, new FinalInterval( currentCellMin, currentCellMax ) ) ).cursor();
						for ( final UnsignedShortType t : cell )
							t.set( c.next() );

						final MDShortArray array = new MDShortArray( ( ( ShortArray ) cell.update( null ) ).getCurrentStorageArray(), currentCellDimRM );
						hdf5Writer.writeShortMDArrayBlockWithOffset( path, array, currentCellMinRM );
					}
				}
			}
		}
		hdf5Writer.close();
	}
}
