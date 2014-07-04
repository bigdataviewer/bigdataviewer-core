package bdv.export;

import static bdv.img.hdf5.Util.reorder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodFactory;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodUnsafe;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.type.numeric.RealType;
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
 * {32,32,8} says that the (downsampled) image is divided into 32x32x8 pixel
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
	public static void writeHdf5PartitionLinkFile( final AbstractSequenceDescription< ?, ?, ? > seq, final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo )
	{
		if ( ! ( seq.getImgLoader() instanceof Hdf5ImageLoader ) )
			throw new IllegalArgumentException( "sequence has " + seq.getImgLoader().getClass() + " imgloader. Hdf5ImageLoader required." );
		final Hdf5ImageLoader loader = ( Hdf5ImageLoader ) seq.getImgLoader();
		writeHdf5PartitionLinkFile( seq, perSetupMipmapInfo, loader.getPartitions(), loader.getHdf5File() );
	}

	public static void writeHdf5PartitionLinkFile( final AbstractSequenceDescription< ?, ?, ? > seq, final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo, final ArrayList< Partition > partitions, final File hdf5File )
	{
		// open HDF5 output file
		if ( hdf5File.exists() )
			hdf5File.delete();
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );

		// write Mipmap descriptions
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final int setupId = setup.getId();
			final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupId );
			hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupId ), mipmapInfo.getResolutions() );
			hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupId ), mipmapInfo.getSubdivisions() );
		}

		// link Cells for all views in the partition
		final File basePath = hdf5File.getParentFile();
		for ( final Partition partition : partitions )
		{
			final Map< Integer, Integer > timepointIdSequenceToPartition = partition.getTimepointIdSequenceToPartition();
			final Map< Integer, Integer > setupIdSequenceToPartition = partition.getSetupIdSequenceToPartition();

			for ( final Entry< Integer, Integer > tEntry : timepointIdSequenceToPartition.entrySet() )
			{
				final int tSequence = tEntry.getKey();
				final int tPartition = tEntry.getValue();
				for ( final Entry< Integer, Integer > sEntry : setupIdSequenceToPartition.entrySet() )
				{
					final int sSequence = sEntry.getKey();
					final int sPartition = sEntry.getValue();

					final ViewId idSequence = new ViewId( tSequence, sSequence );
					final ViewId idPartition = new ViewId( tPartition, sPartition );

					final int numLevels = perSetupMipmapInfo.get( sSequence ).getNumLevels();
					for ( int level = 0; level < numLevels; ++level )
					{
						final String relativePath = XmlHelpers.getRelativePath( new File( partition.getPath() ), basePath ).getPath();
						hdf5Writer.object().createOrUpdateExternalLink( relativePath, Util.getCellsPath( idPartition, level ), Util.getCellsPath( idSequence, level ) );
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
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader} that
	 *            provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 * @param partition
	 *            which part of the dataset to write, and to which file.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5PartitionFile( final AbstractSequenceDescription< ?, ?, ? > seq, final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo, final Partition partition, ProgressWriter progressWriter )
	{
		if ( progressWriter == null )
			progressWriter = new ProgressWriterConsole();

		final ArrayList< Integer > timepointIdsSequence = new ArrayList< Integer >( partition.getTimepointIdSequenceToPartition().keySet() );
		Collections.sort( timepointIdsSequence );
		final int numTimepoints = timepointIdsSequence.size();
		final ArrayList< Integer > setupIdsSequence = new ArrayList< Integer >( partition.getSetupIdSequenceToPartition().keySet() );
		Collections.sort( setupIdsSequence );
		final int numSetups = setupIdsSequence.size();

		if ( ! ( seq.getImgLoader().getImageType() instanceof UnsignedShortType ) )
			throw new IllegalArgumentException( "Expected BasicImgLoader<UnsignedShortTyp> but your dataset has BasicImgLoader<"
					+ seq.getImgLoader().getImageType().getClass().getSimpleName() + ">.\nCurrently writing to HDF5 is only supported for UnsignedShortType." );

		@SuppressWarnings( "unchecked" )
		final BasicImgLoader< UnsignedShortType > imgLoader = ( BasicImgLoader< UnsignedShortType > ) seq.getImgLoader();

		// for progressWriter
		// initial 1 is for writing resolutions etc.
		// (numLevels + 1) is for writing each of the levels plus reading the source image
		int numTasks = 1;
		for ( final int setupIdSequence : setupIdsSequence )
		{
			final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupIdSequence );
			final int numLevels = mipmapInfo.getNumLevels();
			numTasks += numTimepoints * ( numLevels + 1 );
		}
		int numCompletedTasks = 0;
		progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

		// open HDF5 output file
		final File hdf5File = new File( partition.getPath() );
		if ( hdf5File.exists() )
			hdf5File.delete();
		final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5File );

		// write Mipmap descriptions
		for ( final Entry< Integer, Integer > entry : partition.getSetupIdSequenceToPartition().entrySet() )
		{
			final int setupIdSequence = entry.getKey();
			final int setupIdPartition = entry.getValue();
			final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupIdSequence );
			hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupIdPartition ), mipmapInfo.getResolutions() );
			hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupIdPartition ), mipmapInfo.getSubdivisions() );
		}
		progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

		// write image data for all views to the HDF5 file
		final int n = 3;
		final long[] dimensions = new long[ n ];
		int timepointIndex = 0;
		for ( final int timepointIdSequence : timepointIdsSequence )
		{
			final int timepointIdPartition = partition.getTimepointIdSequenceToPartition().get( timepointIdSequence );
			progressWriter.out().printf( "proccessing timepoint %d / %d\n", ++timepointIndex, numTimepoints );
			int setupIndex = 0;
			for ( final int setupIdSequence : setupIdsSequence )
			{
				final int setupIdPartition = partition.getSetupIdSequenceToPartition().get( setupIdSequence );
				progressWriter.out().printf( "proccessing setup %d / %d\n", ++setupIndex, numSetups );

				final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupIdSequence );
				final int[][] resolutions = mipmapInfo.getExportResolutions();
				final int[][] subdivisions = mipmapInfo.getSubdivisions();
				final int numLevels = mipmapInfo.getNumLevels();

				final ViewId viewIdSequence = new ViewId( timepointIdSequence, setupIdSequence );
				final RandomAccessibleInterval< UnsignedShortType > img = imgLoader.getImage( viewIdSequence );
				progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

				for ( int level = 0; level < numLevels; ++level )
				{
					progressWriter.out().println( "writing level " + level );
					img.dimensions( dimensions );
					final int[] factor = resolutions[ level ];
					final boolean fullResolution = ( factor[ 0 ] == 1 && factor[ 1 ] == 1 && factor[ 2 ] == 1 );
					long size = 1;
					if ( !fullResolution )
					{
						for ( int d = 0; d < n; ++d )
						{
							dimensions[ d ] = Math.max( dimensions[ d ] / factor[ d ], 1 );
							size *= factor[ d ];
						}
					}
					final double scale = 1.0 / size;

					final RectangleNeighborhoodFactory< UnsignedShortType > f = RectangleNeighborhoodUnsafe.< UnsignedShortType >factory();
					final long[] spanDim = new long[ n ];
					for ( int d = 0; d < n; ++d )
						spanDim[ d ] = factor[ d ];
					final Interval spanInterval = new FinalInterval( spanDim );

					final NeighborhoodsAccessible< UnsignedShortType > neighborhoods = new NeighborhoodsAccessible< UnsignedShortType >( img, spanInterval, f );
					final RandomAccess< Neighborhood< UnsignedShortType > > block = neighborhoods.randomAccess();

					final long[] minRequiredInput = new long[ n ];
					final long[] maxRequiredInput = new long[ n ];
					img.max( maxRequiredInput );
					for ( int d = 0; d < n; ++d )
						maxRequiredInput[ d ] += factor[ d ] - 1;
					final RandomAccessibleInterval< UnsignedShortType > extendedImg = Views.interval( Views.extendBorder( img ), new FinalInterval( minRequiredInput, maxRequiredInput ) );

					final NeighborhoodsAccessible< UnsignedShortType > extendedNeighborhoods = new NeighborhoodsAccessible< UnsignedShortType >( extendedImg, spanInterval, f );
					final RandomAccess< Neighborhood< UnsignedShortType > > extendedBlock = extendedNeighborhoods.randomAccess();

					final RandomAccess< UnsignedShortType > in = img.randomAccess();

					final int[] cellDimensions = subdivisions[ level ];
					final ViewId viewIdPartition = new ViewId( timepointIdPartition, setupIdPartition );
					hdf5Writer.object().createGroup( Util.getGroupPath( viewIdPartition, level ) );
					final String path = Util.getCellsPath( viewIdPartition, level );
//					hdf5Writer.int16().createMDArray( path, reorder( dimensions ), reorder( cellDimensions ), HDF5IntStorageFeatures.INT_AUTO_SCALING );
					hdf5Writer.int16().createMDArray( path, reorder( dimensions ), reorder( cellDimensions ), HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE );

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
					final long[] blockMin = new long[ n ];
					while ( i.hasNext() )
					{
						i.fwd();
						i.localize( currentCellPos );
						boolean isBorderCell = false;
						for ( int d = 0; d < n; ++d )
						{
							currentCellMin[ d ] = currentCellPos[ d ] * cellDimensions[ d ];
							blockMin[ d ] = currentCellMin[ d ] * factor[ d ];
							final boolean isBorderCellInThisDim = ( currentCellPos[ d ] + 1 == numCells[ d ] );
							currentCellDim[ d ] = isBorderCellInThisDim ? borderSize[ d ] : cellDimensions[ d ];
							currentCellMax[ d ] = currentCellMin[ d ] + currentCellDim[ d ] - 1;
							isBorderCell |= isBorderCellInThisDim;
						}

						final ArrayImg< UnsignedShortType, ? > cell = ArrayImgs.unsignedShorts( currentCellDim );
						final RandomAccess< UnsignedShortType > out = cell.randomAccess();
						if ( fullResolution )
						{
							copyBlock( out, currentCellDim, in, blockMin );
						}
						else
						{
							boolean requiresExtension = false;
							if ( isBorderCell )
								for ( int d = 0; d < n; ++d )
									if ( ( currentCellMax[ d ] + 1 ) * factor[ d ] > img.dimension( d ) )
										requiresExtension = true;
							downsampleBlock( out, currentCellDim, requiresExtension ? extendedBlock : block, blockMin, factor, scale );
						}

						reorder( currentCellMin, currentCellMinRM );
						reorder( currentCellDim, currentCellDimRM );
						final MDShortArray array = new MDShortArray( ( ( ShortArray ) cell.update( null ) ).getCurrentStorageArray(), currentCellDimRM );
						hdf5Writer.int16().writeMDArrayBlockWithOffset( path, array, currentCellMinRM );
					}
					progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );
				}
			}
		}
		hdf5Writer.close();
	}

	private static < T extends RealType< T > > void copyBlock( final RandomAccess< T > out, final long[] outDim, final RandomAccess< T > in, final long[] blockMin )
	{
		out.setPosition( new int[] { 0, 0, 0 } );
		in.setPosition( blockMin );
		for ( out.setPosition( 0, 2 ); out.getLongPosition( 2 ) < outDim[ 2 ]; out.fwd( 2 ) )
		{
			for ( out.setPosition( 0, 1 ); out.getLongPosition( 1 ) < outDim[ 1 ]; out.fwd( 1 ) )
			{
				for ( out.setPosition( 0, 0 ); out.getLongPosition( 0 ) < outDim[ 0 ]; out.fwd( 0 ), in.fwd( 0 ) )
				{
					out.get().set( in.get() );
				}
				in.setPosition( blockMin[ 0 ], 0 );
				in.fwd( 1 );
			}
			in.setPosition( blockMin[ 1 ], 1 );
			in.fwd( 2 );
		}
	}

	private static < T extends RealType< T > > void downsampleBlock( final RandomAccess< T > out, final long[] outDim, final RandomAccess< Neighborhood< T > > block, final long[] blockMin, final int[] blockSize, final double scale )
	{
		out.setPosition( new int[] {0,0,0} );
		block.setPosition( blockMin );
		for ( out.setPosition( 0, 2 ); out.getLongPosition( 2 ) < outDim[ 2 ]; out.fwd( 2 ) )
		{
			for ( out.setPosition( 0, 1 ); out.getLongPosition( 1 ) < outDim[ 1 ]; out.fwd( 1 ) )
			{
				for ( out.setPosition( 0, 0 ); out.getLongPosition( 0 ) < outDim[ 0 ]; out.fwd( 0 ) )
				{
					double sum = 0;
					for ( final T in : block.get() )
						sum += in.getRealDouble();
					out.get().setReal( sum * scale );
					block.move( blockSize[ 0 ], 0 );
				}
				block.setPosition( blockMin[ 0 ], 0 );
				block.move( blockSize[ 1 ], 1 );
			}
			block.setPosition( blockMin[ 1 ], 1 );
			block.move( blockSize[ 2 ], 2 );
		}
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader} that
	 *            provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 * @param hdf5File
	 *            hdf5 file to which the image data is written.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5File( final AbstractSequenceDescription< ?, ?, ? > seq, final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo, final File hdf5File, final ProgressWriter progressWriter )
	{
		final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() )
			timepointIdSequenceToPartition.put( timepoint.getId(), timepoint.getId() );

		final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			setupIdSequenceToPartition.put( setup.getId(), setup.getId() );

		final Partition partition = new Partition( hdf5File.getPath(), timepointIdSequenceToPartition, setupIdSequenceToPartition );
		writeHdf5PartitionFile( seq, perSetupMipmapInfo, partition, progressWriter );
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation. This is the same as
	 * {@link WriteSequenceToHdf5#writeHdf5File(AbstractSequenceDescription, ArrayList, ArrayList, File, ProgressWriter)}
	 * except that only one set of supsampling factors and and subdivision
	 * blocksizes is given, which is used for all {@link BasicViewSetup views}.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of setups and
	 *            timepoints as well as an {@link BasicImgLoader} that provides the
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
	public static void writeHdf5File( final AbstractSequenceDescription< ?, ?, ? > seq, final int[][] resolutions, final int[][] subdivisions, final File hdf5File, final ProgressWriter progressWriter )
	{
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( resolutions, subdivisions );
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupMipmapInfo.put( setup.getId(), mipmapInfo );
		writeHdf5File( seq, perSetupMipmapInfo, hdf5File, progressWriter );
	}
}
