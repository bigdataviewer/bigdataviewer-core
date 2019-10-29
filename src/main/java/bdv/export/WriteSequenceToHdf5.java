/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.export;

import bdv.export.ExportScalePyramid.AfterEachPlane;
import bdv.export.ExportScalePyramid.Block;
import bdv.export.ExportScalePyramid.DatasetIO;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;
import bdv.spimdata.SequenceDescriptionMinimal;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;

/**
 * Create a hdf5 files containing image data from all views and all timepoints
 * in a chunked, mipmaped representation.
 *
 * <p>
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
 * <p>
 * For every mipmap level we have a (3D) int[] resolution array, so the full
 * mipmap pyramid is specified by a nested int[][] array. Likewise, we have a
 * (3D) int[] subdivions array for every mipmap level, so the full chunking of
 * the full pyramid is specfied by a nested int[][] array.
 *
 * <p>
 * A data-set can be stored in a single hdf5 file or split across several hdf5
 * "partitions" with one master hdf5 linking into the partitions.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class WriteSequenceToHdf5
{
	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader}
	 *            that provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 * @param deflate
	 *            whether to compress the data with the HDF5 DEFLATE filter.
	 * @param hdf5File
	 *            hdf5 file to which the image data is written.
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of chunks" is written, giving
	 *            the opportunity to clear caches, etc.
	 * @param numCellCreatorThreads
	 *            The number of threads that will be instantiated to generate
	 *            cell data. Must be at least 1. (In addition the cell creator
	 *            threads there is one writer thread that saves the generated
	 *            data to HDF5.)
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5File(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo,
			final boolean deflate,
			final File hdf5File,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final int numCellCreatorThreads,
			final ProgressWriter progressWriter )
	{
		final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap<>();
		for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() )
			timepointIdSequenceToPartition.put( timepoint.getId(), timepoint.getId() );

		final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap<>();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			setupIdSequenceToPartition.put( setup.getId(), setup.getId() );

		final Partition partition = new Partition( hdf5File.getPath(), timepointIdSequenceToPartition, setupIdSequenceToPartition );
		writeHdf5PartitionFile( seq, perSetupMipmapInfo, deflate, partition, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, progressWriter );
	}

	/**
	 * Create a hdf5 file containing image data from all views and all
	 * timepoints in a chunked, mipmaped representation. This is the same as
	 * {@link WriteSequenceToHdf5#writeHdf5File(AbstractSequenceDescription, Map, boolean, File, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}
	 * except that only one set of supsampling factors and and subdivision
	 * blocksizes is given, which is used for all {@link BasicViewSetup views}.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader}
	 *            that provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param resolutions
	 *            this nested arrays contains per mipmap level, the subsampling
	 *            factors.
	 * @param subdivisions
	 *            this nested arrays contains per mipmap level, the subdivision
	 *            block sizes.
	 * @param deflate
	 *            whether to compress the data with the HDF5 DEFLATE filter.
	 * @param hdf5File
	 *            hdf5 file to which the image data is written.
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of chunks" is written, giving
	 *            the opportunity to clear caches, etc.
	 * @param numCellCreatorThreads
	 *            The number of threads that will be instantiated to generate
	 *            cell data. Must be at least 1. (In addition the cell creator
	 *            threads there is one writer thread that saves the generated
	 *            data to HDF5.)
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5File(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final int[][] resolutions,
			final int[][] subdivisions,
			final boolean deflate,
			final File hdf5File,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final int numCellCreatorThreads,
			final ProgressWriter progressWriter )
	{
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap<>();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( resolutions, subdivisions );
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupMipmapInfo.put( setup.getId(), mipmapInfo );
		writeHdf5File( seq, perSetupMipmapInfo, deflate, hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, progressWriter );
	}

	/**
	 * Create a hdf5 master file linking to image data from all views and all
	 * timepoints. This is the same as
	 * {@link #writeHdf5PartitionLinkFile(AbstractSequenceDescription, Map, ArrayList, File)},
	 * except that the information about the partition files as well as the
	 * path of the master file to be written is obtained from the
	 * {@link BasicImgLoader} of the sequence, which must be a
	 * {@link Hdf5ImageLoader}.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader}
	 *            that provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 */
	public static void writeHdf5PartitionLinkFile( final AbstractSequenceDescription< ?, ?, ? > seq, final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo )
	{
		if ( !( seq.getImgLoader() instanceof Hdf5ImageLoader ) )
			throw new IllegalArgumentException( "sequence has " + seq.getImgLoader().getClass() + " imgloader. Hdf5ImageLoader required." );
		final Hdf5ImageLoader loader = ( Hdf5ImageLoader ) seq.getImgLoader();
		writeHdf5PartitionLinkFile( seq, perSetupMipmapInfo, loader.getPartitions(), loader.getHdf5File() );
	}

	/**
	 * Create a hdf5 master file linking to image data from all views and all
	 * timepoints. Which hdf5 files contain which part of the image data is
	 * specified in the {@code portitions} parameter.
	 *
	 * Note that this method only writes the master file containing links. The
	 * individual partitions need to be written with
	 * {@link #writeHdf5PartitionFile(AbstractSequenceDescription, Map, boolean, Partition, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}.
	 *
	 * @param seq
	 *            description of the sequence to be stored as hdf5. (The
	 *            {@link AbstractSequenceDescription} contains the number of
	 *            setups and timepoints as well as an {@link BasicImgLoader}
	 *            that provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 * @param partitions
	 *            which parts of the dataset are stored in which files.
	 * @param hdf5File
	 *            hdf5 master file to which the image data from the partition
	 *            files is linked.
	 */
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
	 *            setups and timepoints as well as an {@link BasicImgLoader}
	 *            that provides the image data, Registration information is not
	 *            needed here, that will go into the accompanying xml).
	 * @param perSetupMipmapInfo
	 *            this maps from setup {@link BasicViewSetup#getId() id} to
	 *            {@link ExportMipmapInfo} for that setup. The
	 *            {@link ExportMipmapInfo} contains for each mipmap level, the
	 *            subsampling factors and subdivision block sizes.
	 * @param deflate
	 *            whether to compress the data with the HDF5 DEFLATE filter.
	 * @param partition
	 *            which part of the dataset to write, and to which file.
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of chunks" is written, giving
	 *            the opportunity to clear caches, etc.
	 * @param numCellCreatorThreads
	 *            The number of threads that will be instantiated to generate
	 *            cell data. Must be at least 1. (In addition the cell creator
	 *            threads there is one writer thread that saves the generated
	 *            data to HDF5.)
	 * @param progressWriter
	 *            completion ratio and status output will be directed here.
	 */
	public static void writeHdf5PartitionFile(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo,
			final boolean deflate,
			final Partition partition,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final int numCellCreatorThreads,
			ProgressWriter progressWriter )
	{
		final int blockWriterQueueLength = 100;

		if ( progressWriter == null )
			progressWriter = new ProgressWriterConsole();
		progressWriter.setProgress( 0 );

		// get sequence timepointIds for the timepoints contained in this partition
		final ArrayList< Integer > timepointIdsSequence = new ArrayList<>( partition.getTimepointIdSequenceToPartition().keySet() );
		Collections.sort( timepointIdsSequence );
		final int numTimepoints = timepointIdsSequence.size();
		final ArrayList< Integer > setupIdsSequence = new ArrayList<>( partition.getSetupIdSequenceToPartition().keySet() );
		Collections.sort( setupIdsSequence );

		// get the BasicImgLoader that supplies the images
		final BasicImgLoader imgLoader = seq.getImgLoader();

		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {
			final Object type = imgLoader.getSetupImgLoader( setup.getId() ).getImageType();
			if ( !( type instanceof UnsignedShortType ) )
				throw new IllegalArgumentException( "Expected BasicImgLoader<UnsignedShortTyp> but your dataset has BasicImgLoader<"
						+ type.getClass().getSimpleName() + ">.\nCurrently writing to HDF5 is only supported for UnsignedShortType." );
		}


		// open HDF5 partition output file
		final File hdf5File = new File( partition.getPath() );
		if ( hdf5File.exists() )
			hdf5File.delete();
		final Hdf5BlockWriterThread writerQueue = new Hdf5BlockWriterThread( hdf5File, blockWriterQueueLength );
		try
		{
			writerQueue.start();

			// start CellCreatorThreads
			final ExecutorService executorService = Executors.newFixedThreadPool( numCellCreatorThreads );
			try
			{
				// calculate number of tasks for progressWriter
				int numTasks = 0; // first task is for writing mipmap descriptions etc...
				for ( final int timepointIdSequence : timepointIdsSequence )
					for ( final int setupIdSequence : setupIdsSequence )
						if ( seq.getViewDescriptions().get( new ViewId( timepointIdSequence, setupIdSequence ) ).isPresent() )
							numTasks++;
				int numCompletedTasks = 0;

				// write Mipmap descriptions
				for ( final Entry< Integer, Integer > entry : partition.getSetupIdSequenceToPartition().entrySet() )
				{
					final int setupIdSequence = entry.getKey();
					final int setupIdPartition = entry.getValue();
					final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupIdSequence );
					writerQueue.writeMipmapDescription( setupIdPartition, mipmapInfo );
				}

				// Progress of 1% for writing meta data
				progressWriter.setProgress(0.01);
				progressWriter = new SubTaskProgressWriter(progressWriter, 0.01, 1.0);

				// write image data for all views to the HDF5 file
				int timepointIndex = 0;
				for ( final int timepointIdSequence : timepointIdsSequence )
				{
					final int timepointIdPartition = partition.getTimepointIdSequenceToPartition().get( timepointIdSequence );
					progressWriter.out().printf( "proccessing timepoint %d / %d\n", ++timepointIndex, numTimepoints );

					// assemble the viewsetups that are present in this timepoint
					final ArrayList< Integer > setupsTimePoint = new ArrayList<>();

					for ( final int setupIdSequence : setupIdsSequence )
						if ( seq.getViewDescriptions().get( new ViewId( timepointIdSequence, setupIdSequence ) ).isPresent() )
							setupsTimePoint.add( setupIdSequence );

					final int numSetups = setupsTimePoint.size();

					int setupIndex = 0;
					for ( final int setupIdSequence : setupsTimePoint )
					{
						final int setupIdPartition = partition.getSetupIdSequenceToPartition().get( setupIdSequence );
						progressWriter.out().printf( "proccessing setup %d / %d\n", ++setupIndex, numSetups );

						@SuppressWarnings( "unchecked" )
						final RandomAccessibleInterval< UnsignedShortType > img = ( ( BasicSetupImgLoader< UnsignedShortType > ) imgLoader.getSetupImgLoader( setupIdSequence ) ).getImage( timepointIdSequence );
						final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupIdSequence );
						final double startCompletionRatio = ( double ) numCompletedTasks++ / numTasks;
						final double endCompletionRatio = ( double ) numCompletedTasks / numTasks;
						final ProgressWriter subProgressWriter = new SubTaskProgressWriter( progressWriter, startCompletionRatio, endCompletionRatio );

						writeViewToHdf5PartitionFile(
								img, timepointIdPartition, setupIdPartition, mipmapInfo, false,
								deflate, writerQueue, executorService, numCellCreatorThreads, loopbackHeuristic, afterEachPlane, subProgressWriter );
					}
				}
			}
			finally
			{
				executorService.shutdown();
			}
		}
		finally {
			writerQueue.close();
		}
		progressWriter.setProgress( 1.0 );
	}

	/**
	 * Write a single view to a hdf5 partition file, in a chunked, mipmaped
	 * representation. Note that the specified view must not already exist in
	 * the partition file!
	 *
	 * @param img
	 *            the view to be written.
	 * @param partition
	 *            describes which part of the full sequence is contained in this
	 *            partition, and to which file this partition is written.
	 * @param timepointIdPartition
	 *            the timepoint id wrt the partition of the view to be written.
	 *            The information in {@code partition} relates this to timepoint
	 *            id in the full sequence.
	 * @param setupIdPartition
	 *            the setup id wrt the partition of the view to be written. The
	 *            information in {@code partition} relates this to setup id in
	 *            the full sequence.
	 * @param mipmapInfo
	 *            contains for each mipmap level of the setup, the subsampling
	 *            factors and subdivision block sizes.
	 * @param writeMipmapInfo
	 *            whether to write mipmap description for the setup. must be
	 *            done (at least) once for each setup in the partition.
	 * @param deflate
	 *            whether to compress the data with the HDF5 DEFLATE filter.
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of chunks" is written, giving
	 *            the opportunity to clear caches, etc.
	 * @param numCellCreatorThreads
	 *            The number of threads that will be instantiated to generate
	 *            cell data. Must be at least 1. (In addition the cell creator
	 *            threads there is one writer thread that saves the generated
	 *            data to HDF5.)
	 * @param progressWriter
	 *            completion ratio and status output will be directed here. may
	 *            be null.
	 */
	public static void writeViewToHdf5PartitionFile(
			final RandomAccessibleInterval< UnsignedShortType > img,
			final Partition partition,
			final int timepointIdPartition,
			final int setupIdPartition,
			final ExportMipmapInfo mipmapInfo,
			final boolean writeMipmapInfo,
			final boolean deflate,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final int numCellCreatorThreads,
			final ProgressWriter progressWriter )
	{
		final int blockWriterQueueLength = 100;

		// create and start Hdf5BlockWriterThread
		final Hdf5BlockWriterThread writerQueue = new Hdf5BlockWriterThread( partition.getPath(), blockWriterQueueLength );
		try
		{
			writerQueue.start();
			final ExecutorService executorService = Executors.newFixedThreadPool( numCellCreatorThreads );
			try
			{
				// write the image
				writeViewToHdf5PartitionFile( img, timepointIdPartition, setupIdPartition, mipmapInfo, writeMipmapInfo, deflate, writerQueue, executorService, numCellCreatorThreads, loopbackHeuristic, afterEachPlane, progressWriter );
			}
			finally
			{
				executorService.shutdown();
			}
		}
		finally
		{
			writerQueue.close();
		}
	}

	static class LoopBackImageLoader extends Hdf5ImageLoader
	{
		private LoopBackImageLoader( final IHDF5Reader existingHdf5Reader, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
		{
			super( null, existingHdf5Reader, null, sequenceDescription, false );
		}

		static LoopBackImageLoader create( final IHDF5Reader existingHdf5Reader, final int timepointIdPartition, final int setupIdPartition, final Dimensions imageDimensions )
		{
			final HashMap< Integer, TimePoint > timepoints = new HashMap<>();
			timepoints.put( timepointIdPartition, new TimePoint( timepointIdPartition ) );
			final HashMap< Integer, BasicViewSetup > setups = new HashMap<>();
			setups.put( setupIdPartition, new BasicViewSetup( setupIdPartition, null, imageDimensions, null ) );
			final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, null, null );
			return new LoopBackImageLoader( existingHdf5Reader, seq );
		}
	}

	/*
	 TODO:
	 	This approximately implements DatasetIO in terms of IHDFAccess.
	 	It works with how DatasetIO is currently used,
	 	but for general usage, IHDF5Access must be revised.
	 */
	static class HDF5DatasetIO implements DatasetIO< Object, UnsignedShortType >
	{
		private final IHDF5Access writerQueue;
		private final ViewId viewIdPartition;
		private final HDF5IntStorageFeatures storage;
		private final LoopBackImageLoader loopback;

		public HDF5DatasetIO( final IHDF5Access writerQueue, final ViewId viewIdPartition, final HDF5IntStorageFeatures storage, final LoopBackImageLoader loopback )
		{
			this.writerQueue = writerQueue;
			this.viewIdPartition = viewIdPartition;
			this.storage = storage;
			this.loopback = loopback;
		}

		@Override
		public Object createDataset( final int level, final long[] dimensions, final int[] blockSize )
		{
			final String path = Util.getCellsPath( viewIdPartition, level );
			writerQueue.createAndOpenDataset( path, dimensions.clone(), blockSize.clone(), storage );
			return null;
		}

		@Override
		public void writeBlock( final Object dataset, final Block< UnsignedShortType > dataBlock )
		{
			final SingleCellArrayImg< UnsignedShortType, ? > img = dataBlock.getData();
			final long[] blockDimensions = Intervals.dimensionsAsLongArray( img );
			final long[] offset = Intervals.minAsLongArray( img );
			writerQueue.writeBlockWithOffset( Cast.unchecked( img.getStorageArray() ), blockDimensions, offset );
		}

		@Override
		public void flush( final Object dataset )
		{
			writerQueue.closeDataset();
		}

		@Override
		public RandomAccessibleInterval< UnsignedShortType > getImage( final int level )
		{
			return loopback.getSetupImgLoader( viewIdPartition.getViewSetupId() ).getImage( viewIdPartition.getTimePointId(), level );
		}
	}

	/**
	 * Write a single view to a hdf5 partition file, in a chunked, mipmaped
	 * representation. Note that the specified view must not already exist in
	 * the partition file!
	 *
	 * @param img
	 *            the view to be written.
	 * @param timepointIdPartition
	 *            the timepoint id wrt the partition of the view to be written.
	 *            The information in {@code partition} relates this to timepoint
	 *            id in the full sequence.
	 * @param setupIdPartition
	 *            the setup id wrt the partition of the view to be written. The
	 *            information in {@code partition} relates this to setup id in
	 *            the full sequence.
	 * @param mipmapInfo
	 *            contains for each mipmap level of the setup, the subsampling
	 *            factors and subdivision block sizes.
	 * @param writeMipmapInfo
	 *            whether to write mipmap description for the setup. must be
	 *            done (at least) once for each setup in the partition.
	 * @param deflate
	 *            whether to compress the data with the HDF5 DEFLATE filter.
	 * @param writerQueue
	 *            block writing tasks are enqueued here.
	 * @param executorService
	 *            executor used for creating (possibly down-sampled) blocks of
	 *            the view to be written.
	 * @param numThreads
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of chunks" is written, giving
	 *            the opportunity to clear caches, etc.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here. may
	 *            be null.
	 */
	public static void writeViewToHdf5PartitionFile(
			final RandomAccessibleInterval< UnsignedShortType > img,
			final int timepointIdPartition,
			final int setupIdPartition,
			final ExportMipmapInfo mipmapInfo,
			final boolean writeMipmapInfo,
			final boolean deflate,
			final IHDF5Access writerQueue,
			final ExecutorService executorService, // TODO
			final int numThreads, // TODO
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			ProgressWriter progressWriter )
	{
		// write Mipmap descriptions
		if ( writeMipmapInfo )
			writerQueue.writeMipmapDescription( setupIdPartition, mipmapInfo );

		// create loopback image-loader to read already written chunks from the
		// h5 for generating low-resolution versions.
		final LoopBackImageLoader loopback = ( loopbackHeuristic == null ) ? null : LoopBackImageLoader.create( writerQueue.getIHDF5Writer(), timepointIdPartition, setupIdPartition, img );

		final DatasetIO< Object, UnsignedShortType > io = new HDF5DatasetIO(
				writerQueue,
				new ViewId( timepointIdPartition, setupIdPartition ),
				deflate ? HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE : HDF5IntStorageFeatures.INT_AUTO_SCALING,
				loopback );

		try
		{
			ExportScalePyramid.writeScalePyramid(
					img,
					new UnsignedShortType(),
					mipmapInfo,
					io,
					executorService,
					numThreads,
					loopbackHeuristic,
					afterEachPlane,
					progressWriter );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		if ( loopback != null )
			loopback.close();
	}
}
