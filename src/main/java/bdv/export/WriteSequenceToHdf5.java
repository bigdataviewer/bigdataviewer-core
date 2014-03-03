package bdv.export;

import static bdv.img.hdf5.Util.reorder;

import java.io.File;
import java.util.ArrayList;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Create a hdf5 files containing image data from all views and all timepoints
 * in a chunked, mipmaped representation.
 *
 * Every image is stored in multiple resolutions. The resolutions are described
 * as int[] arrays defining multiple of original pixel size in every dimension.
 * For example {1,1,1} is the original resolution, {4,4,2} is downsampled by
 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image is stored
 * as a chunked three-dimensional array (each chunk corresponds to one cell of a
 * {@link CellImg} when the data is loaded). The chunk sizes are defined by the
 * subdivisions parameter which is an array of int[], one per resolution. Each
 * int[] array describes the X,Y,Z chunk size for one resolution. For instance
 * {32,32,8} says that the (downsampled) image is dividex into 32x32x8 pixel
 * blocks.
 *
 * For every mipmap level we have a (3D) int[] resolution array, so the full
 * mipmap pyramid is specified by a nested int[][] array. Likewise, we have a
 * (3D) int[] subdivions array for every mipmap level, so the full chunking of
 * the full pyramid is specfied by a nested int[][] array.
 *
 * A data-set can be stored in a single hdf5 file or split across several hdf5
 * "partitions" with one master hdf5 linking into the partitions.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class WriteSequenceToHdf5
{
	public static void writeHdf5PartitionLinkFile( final SequenceDescription seq, final ArrayList< int[][] > perSetupResolutions, final ArrayList< int[][] > perSetupSubdivisions )
	{
		if ( ! ( seq.imgLoader instanceof Hdf5ImageLoader ) )
			throw new IllegalArgumentException( "sequence has " + seq.imgLoader.getClass() + " imgloader. Hdf5ImageLoader required." );
		final Hdf5ImageLoader loader = ( Hdf5ImageLoader ) seq.imgLoader;
		writeHdf5PartitionLinkFile( seq, perSetupResolutions, perSetupSubdivisions, loader.getPartitions(), loader.getHdf5File() );
	}

	public static void writeHdf5PartitionLinkFile( final SequenceDescription seq, final ArrayList< int[][] > perSetupResolutions, final ArrayList< int[][] > perSetupSubdivisions, final ArrayList< Partition > partitions, final File hdf5File )
	{
		final int totalNumSetups = seq.numViewSetups();

		assert( perSetupResolutions.size() == totalNumSetups );
		assert( perSetupSubdivisions.size() == totalNumSetups );

		// open HDF5 output file
		if ( hdf5File.exists() )
			hdf5File.delete();
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );

		// write Mipmap descriptions
		for ( int setup = 0; setup < totalNumSetups; ++setup )
		{
			final int[][] resolutions = perSetupResolutions.get( setup );
			final int[][] subdivisions = perSetupSubdivisions.get( setup );
			final double[][] dres = new double[ resolutions.length ][];
			for ( int l = 0; l < resolutions.length; ++l )
			{
				dres[ l ] = new double[ resolutions[ l ].length ];
				for ( int d = 0; d < resolutions[ l ].length; ++d )
					dres[ l ][ d ] = resolutions[ l ][ d ];
			}
			hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setup ), dres );
			hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setup ), subdivisions );
		}

		// write number of timepoints and setups
		hdf5Writer.writeInt( "numTimepoints", seq.numTimepoints() );
		hdf5Writer.writeInt( "numSetups", seq.numViewSetups() );

		final File basePath = hdf5File.getParentFile();
		for ( final Partition partition : partitions )
		{
			final int timepointOffsetSeq = partition.getTimepointOffset() + partition.getTimepointStart();
			final int timepointOffsetFile = partition.getTimepointStart();
			final int numTimepoints = partition.getTimepointLength();
			final int setupOffsetSeq = partition.getSetupOffset() + partition.getSetupStart();
			final int setupOffsetFile = partition.getSetupStart();
			final int numSetups = partition.getSetupLength();

			// link Cells for all views in the partition
			for ( int timepoint = 0; timepoint < numTimepoints; ++timepoint )
			{
				final int timepointSeq = timepoint + timepointOffsetSeq;
				final int timepointFile = timepoint + timepointOffsetFile;
				for ( int setup = 0; setup < numSetups; ++setup )
				{
					final int setupSeq = setup + setupOffsetSeq;
					final int setupFile = setup + setupOffsetFile;
					final int numLevels = perSetupResolutions.get( setupSeq ).length;
					for ( int level = 0; level < numLevels; ++level )
					{
						final String relativePath = XmlHelpers.getRelativePath( new File( partition.getPath() ), basePath ).getPath();
						hdf5Writer.createOrUpdateExternalLink( relativePath, Util.getCellsPath( timepointFile, setupFile, level ), Util.getCellsPath( timepointSeq, setupSeq, level ) );
					}
				}
			}
		}
		hdf5Writer.close();
	}

	/**
	 * Create a hdf5 partition file containing image data for a subset of views
	 * and timepoints in a chunked, mipmaped representation.
	 *
	 * Please note that the description of the <em>full</em> dataset must be
	 * given in the <code>seq</code>, <code>perSetupResolutions</code>, and
	 * <code>perSetupSubdivisions</code> parameters. Then only the part
	 * described by <code>partition</code> will be written.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link SequenceDescription} contains the number of setups and
	 *            timepoints as well as an {@link ImgLoader} that provides the
	 *            image data, Registration information is not needed here, that
	 *            will go into the accompanying xml).
	 * @param perSetupResolutions
	 *            this list of nested arrays contains per {@link ViewSetup}, per
	 *            mipmap level, the subsampling factors.
	 * @param perSetupSubdivisions
	 *            this list of nested arrays contains per {@link ViewSetup}, per
	 *            mipmap level, the subdivision block sizes.
	 * @param partition
	 *            which part of the dataset to write, and to which file.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5PartitionFile( final SequenceDescription seq, final ArrayList< int[][] > perSetupResolutions, final ArrayList< int[][] > perSetupSubdivisions, final Partition partition, ProgressWriter progressWriter )
	{
		if ( progressWriter == null )
			progressWriter = new ProgressWriterConsole();

		final int timepointOffsetSeq = partition.getTimepointOffset() + partition.getTimepointStart();
		final int timepointOffsetFile = partition.getTimepointStart();
		final int numTimepoints = partition.getTimepointLength();
		final int setupOffsetSeq = partition.getSetupOffset() + partition.getSetupStart();
		final int setupOffsetFile = partition.getSetupStart();
		final int numSetups = partition.getSetupLength();
		@SuppressWarnings( "unchecked" )
		final ImgLoader< UnsignedShortType > imgLoader = ( ImgLoader< UnsignedShortType > ) seq.imgLoader;

		// for progressWriter
		// initial 1 is for writing resolutions etc.
		// (numLevels + 1) is for writing each of the levels plus reading the source image
		int numTasks = 1;
		for ( int setup = 0; setup < numSetups; ++setup )
		{
			final int numLevels = perSetupResolutions.get( setup ).length;
			numTasks += numTimepoints * ( numLevels + 1 );
		}
		int numCompletedTasks = 0;
		progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

		assert( perSetupResolutions.size() >= setupOffsetSeq + numSetups );
		assert( perSetupSubdivisions.size() >= setupOffsetSeq + numSetups );

		// open HDF5 output file
		final File hdf5File = new File( partition.getPath() );
		if ( hdf5File.exists() )
			hdf5File.delete();
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );

		// write Mipmap descriptions
		for ( int setup = 0; setup < numSetups; ++setup )
		{
			final int setupSeq = setup + setupOffsetSeq;
			final int setupFile = setup + setupOffsetFile;
			final int[][] resolutions = perSetupResolutions.get( setupSeq );
			final int[][] subdivisions = perSetupSubdivisions.get( setupSeq );
			final double[][] dres = new double[ resolutions.length ][];
			for ( int l = 0; l < resolutions.length; ++l )
			{
				dres[ l ] = new double[ resolutions[ l ].length ];
				for ( int d = 0; d < resolutions[ l ].length; ++d )
					dres[ l ][ d ] = resolutions[ l ][ d ];
			}
			hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupFile ), dres );
			hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupFile ), subdivisions );
		}

		// write number of timepoints and setups
		hdf5Writer.writeInt( "numTimepoints", numTimepoints );
		hdf5Writer.writeInt( "numSetups", numSetups );

		progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

		// write image data for all views to the HDF5 file
		final int n = 3;
		final long[] dimensions = new long[ n ];
		for ( int timepoint = 0; timepoint < numTimepoints; ++timepoint )
		{
			final int timepointSeq = timepoint + timepointOffsetSeq;
			final int timepointFile = timepoint + timepointOffsetFile;
			progressWriter.out().printf( "proccessing timepoint %d / %d\n", timepoint + 1, numTimepoints );
			for ( int setup = 0; setup < numSetups; ++setup )
			{
				final int setupSeq = setup + setupOffsetSeq;
				final int setupFile = setup + setupOffsetFile;
				final int[][] resolutions = perSetupResolutions.get( setupSeq );
				final int[][] subdivisions = perSetupSubdivisions.get( setupSeq );
				final int numLevels = resolutions.length;

				progressWriter.out().printf( "proccessing setup %d / %d\n", setup + 1, numSetups );
				final View view = new View( seq, timepointSeq, setupSeq, null );
				progressWriter.out().println( "loading image" );
				final RandomAccessibleInterval< UnsignedShortType > img = imgLoader.getImage( view );
				progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

				for ( int level = 0; level < numLevels; ++level )
				{
					progressWriter.out().println( "writing level " + level );
					img.dimensions( dimensions );
					final RandomAccessible< UnsignedShortType > source;
					final int[] factor = resolutions[ level ];
					if ( factor[ 0 ] == 1 && factor[ 1 ] == 1 && factor[ 2 ] == 1 )
						source = img;
					else
					{
						for ( int d = 0; d < n; ++d )
							dimensions[ d ] = Math.max( dimensions[ d ] / factor[ d ], 1 );

						final Img< UnsignedShortType > downsampled = ArrayImgs.unsignedShorts( dimensions );
						Downsample.downsample( Views.extendBorder( img ), downsampled, factor );
						source = downsampled;
					}

					final int[] cellDimensions = subdivisions[ level ];
					hdf5Writer.createGroup( Util.getGroupPath( timepointFile, setupFile, level ) );
					final String path = Util.getCellsPath( timepointFile, setupFile, level );
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
					progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );
				}
			}
		}
		hdf5Writer.close();
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link SequenceDescription} contains the number of setups and
	 *            timepoints as well as an {@link ImgLoader} that provides the
	 *            image data, Registration information is not needed here, that
	 *            will go into the accompanying xml).
	 * @param perSetupResolutions
	 *            this list of nested arrays contains per {@link ViewSetup}, per
	 *            mipmap level, the subsampling factors.
	 * @param perSetupSubdivisions
	 *            this list of nested arrays contains per {@link ViewSetup}, per
	 *            mipmap level, the subdivision block sizes.
	 * @param hdf5File
	 *            hdf5 file to which the image data is written.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5File( final SequenceDescription seq, final ArrayList< int[][] > perSetupResolutions, final ArrayList< int[][] > perSetupSubdivisions, final File hdf5File, final ProgressWriter progressWriter )
	{
		final Partition partition = new Partition( hdf5File.getPath(), 0, 0, seq.numTimepoints(), 0, 0, seq.numViewSetups() );
		writeHdf5PartitionFile( seq, perSetupResolutions, perSetupSubdivisions, partition, progressWriter );
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation. This is the same as
	 * {@link WriteSequenceToHdf5#writeHdf5File(SequenceDescription, ArrayList, ArrayList, File, ProgressWriter)}
	 * except that only one set of supsampling factors and and subdivision
	 * blocksizes is given, which is used for all {@link ViewSetup views}.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link SequenceDescription} contains the number of setups and
	 *            timepoints as well as an {@link ImgLoader} that provides the
	 *            image data, Registration information is not needed here, that
	 *            will go into the accompanying xml).
	 * @param resolutions
	 *            this nested arrays contains per mipmap level, the subsampling
	 *            factors.
	 * @param subdivisions
	 *            this nested arrays contains per mipmap level, the subdivision
	 *            block sizes.
	 * @param hdf5File
	 *            hdf5 file to which the image data is written.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5File( final SequenceDescription seq, final int[][] resolutions, final int[][] subdivisions, final File hdf5File, final ProgressWriter progressWriter )
	{
		final int numSetups = seq.numViewSetups();
		final ArrayList< int[][] > perSetupResolutions = new ArrayList< int[][] >();
		final ArrayList< int[][] > perSetupSubdivisions = new ArrayList< int[][] >();
		for ( int setup = 0; setup < numSetups; ++setup )
		{
			perSetupResolutions.add( resolutions );
			perSetupSubdivisions.add( subdivisions );
		}
		writeHdf5File( seq, perSetupResolutions, perSetupSubdivisions, hdf5File, progressWriter );
	}
}
