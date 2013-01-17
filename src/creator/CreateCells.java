package creator;

import static viewer.hdf5.Util.reorder;

import java.io.File;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
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
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import viewer.hdf5.Util;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

public class CreateCells
{
	public static class MipMapDefinition
	{
	//  mipmap def 1
//		public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 1 } };
//		public static final int[][] subdivisions = { { 32, 32, 4 }, { 32, 32, 4 }, { 16, 16, 4 } };

	//  mipmap def 2
		public static final int[][] resolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		public static final int[][] subdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };
	}


	public static void main( final String[] args )
	{
//		final File seqFile = new File( "/Users/tobias/workspace/data/fast fly/111010_weber/e012-seq.xml" );
//		final File hdf5File = new File( "/Users/tobias/Desktop/e012-cells.h5" );
		final File seqFile = new File( "/Users/tobias/Desktop/celegans/celegans-desc.xml" );
		final File hdf5File = new File( "/Users/tobias/Desktop/celegans/celegans-cells.h5" );
		final int[][] resolutions = MipMapDefinition.resolutions;
		final int[][] subdivisions = MipMapDefinition.subdivisions;

		createHdf5File( seqFile, hdf5File, resolutions, subdivisions );
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
	public static void createHdf5File( final File seqFile, final File hdf5File, final int[][] resolutions, final int[][] subdivisions )
	{
		try
		{
//			final SequenceViewsLoader loader = new SequenceViewsLoader( viewRegistrationsFilename );
//			final SequenceDescription seq = loader.getSequenceDescription();
			final SequenceDescription seq = SequenceDescription.load( seqFile.getAbsolutePath(), true );
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
//					final View view = loader.getView( timepoint, setup );
					final View view = new View( seq, timepoint, setup, null );
					final ImgPlus< UnsignedShortType > img = seq.imgLoader.getUnsignedShortImage( view );

					for ( int level = 0; level < numLevels; ++level )
					{
						img.dimensions( dimensions );
						final RandomAccessible< UnsignedShortType > source;
						final int[] factor = resolutions[ level ];
						if ( factor[0] == 1 && factor[1] == 1 && factor[2] == 1 )
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
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
