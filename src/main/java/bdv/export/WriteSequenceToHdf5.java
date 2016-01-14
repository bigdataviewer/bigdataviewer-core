/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.img.hdf5.Util;
import bdv.spimdata.SequenceDescriptionMinimal;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

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
		final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() )
			timepointIdSequenceToPartition.put( timepoint.getId(), timepoint.getId() );

		final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
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
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
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
	 * {@link #writeHdf5PartitionFile(AbstractSequenceDescription, Map, boolean, Partition, LoopbackHeuristic, AfterEachPlane, ProgressWriter)}.
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
		final ArrayList< Integer > timepointIdsSequence = new ArrayList< Integer >( partition.getTimepointIdSequenceToPartition().keySet() );
		Collections.sort( timepointIdsSequence );
		final int numTimepoints = timepointIdsSequence.size();
		final ArrayList< Integer > setupIdsSequence = new ArrayList< Integer >( partition.getSetupIdSequenceToPartition().keySet() );
		Collections.sort( setupIdsSequence );

		// get the BasicImgLoader that supplies the images
		final BasicImgLoader imgLoader = ( BasicImgLoader ) seq.getImgLoader();

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
		writerQueue.start();

		// start CellCreatorThreads
		final CellCreatorThread[] cellCreatorThreads = createAndStartCellCreatorThreads( numCellCreatorThreads );

		// calculate number of tasks for progressWriter
		int numTasks = 1; // first task is for writing mipmap descriptions etc...
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
		progressWriter.setProgress( ( double ) ++numCompletedTasks / numTasks );

		// write image data for all views to the HDF5 file
		int timepointIndex = 0;
		for ( final int timepointIdSequence : timepointIdsSequence )
		{
			final int timepointIdPartition = partition.getTimepointIdSequenceToPartition().get( timepointIdSequence );
			progressWriter.out().printf( "proccessing timepoint %d / %d\n", ++timepointIndex, numTimepoints );

			// assemble the viewsetups that are present in this timepoint
			final ArrayList< Integer > setupsTimePoint = new ArrayList< Integer >();

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
						deflate, writerQueue, cellCreatorThreads, loopbackHeuristic, afterEachPlane, subProgressWriter );
			}
		}

		// shutdown and close file
		stopCellCreatorThreads( cellCreatorThreads );
		writerQueue.close();
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
		writerQueue.start();
		final CellCreatorThread[] cellCreatorThreads = createAndStartCellCreatorThreads( numCellCreatorThreads );

		// write the image
		writeViewToHdf5PartitionFile( img, timepointIdPartition, setupIdPartition, mipmapInfo, writeMipmapInfo, deflate, writerQueue, cellCreatorThreads, loopbackHeuristic, afterEachPlane, progressWriter );

		stopCellCreatorThreads( cellCreatorThreads );
		writerQueue.close();
	}

	static class LoopBackImageLoader extends Hdf5ImageLoader
	{
		private LoopBackImageLoader( final IHDF5Reader existingHdf5Reader, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
		{
			super( null, existingHdf5Reader, null, sequenceDescription, false );
		}

		static LoopBackImageLoader create( final IHDF5Reader existingHdf5Reader, final int timepointIdPartition, final int setupIdPartition, final Dimensions imageDimensions )
		{
			final HashMap< Integer, TimePoint > timepoints = new HashMap< Integer, TimePoint >();
			timepoints.put( timepointIdPartition, new TimePoint( timepointIdPartition ) );
			final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >();
			setups.put( setupIdPartition, new BasicViewSetup( setupIdPartition, null, imageDimensions, null ) );
			final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, null, null );
			return new LoopBackImageLoader( existingHdf5Reader, seq );
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
	 * @param cellCreatorThreads
	 *            threads used for creating (possibly down-sampled) blocks of
	 *            the view to be written.
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
			final Hdf5BlockWriterThread writerQueue,
			final CellCreatorThread[] cellCreatorThreads,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			ProgressWriter progressWriter )
	{
		final HDF5IntStorageFeatures storage = deflate ? HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE : HDF5IntStorageFeatures.INT_AUTO_SCALING;

		if ( progressWriter == null )
			progressWriter = new ProgressWriterConsole();

		// for progressWriter
		final int numTasks = mipmapInfo.getNumLevels();
		int numCompletedTasks = 0;
		progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );

		// write Mipmap descriptions
		if ( writeMipmapInfo )
			writerQueue.writeMipmapDescription( setupIdPartition, mipmapInfo );

		// create loopback image-loader to read already written chunks from the
		// h5 for generating low-resolution versions.
		final LoopBackImageLoader loopback = ( loopbackHeuristic == null ) ? null : LoopBackImageLoader.create( writerQueue.getIHDF5Writer(), timepointIdPartition, setupIdPartition, img );

		// write image data for all views to the HDF5 file
		final int n = 3;
		final long[] dimensions = new long[ n ];

		final int[][] resolutions = mipmapInfo.getExportResolutions();
		final int[][] subdivisions = mipmapInfo.getSubdivisions();
		final int numLevels = mipmapInfo.getNumLevels();

		for ( int level = 0; level < numLevels; ++level )
		{
			progressWriter.out().println( "writing level " + level );

			final long t0 = System.currentTimeMillis();

			final RandomAccessibleInterval< UnsignedShortType > sourceImg;
			final int[] factor;
			final boolean useLoopBack;
			if ( loopbackHeuristic == null )
			{
				sourceImg = img;
				factor = resolutions[ level ];
				useLoopBack = false;
			}
			else
			{
				// Are downsampling factors a multiple of a level that we have
				// already written?
				int[] factorsToPreviousLevel = null;
				int previousLevel = -1;
				A: for ( int l = level - 1; l >= 0; --l )
				{
					final int[] f = new int[ n ];
					for ( int d = 0; d < n; ++d )
					{
						f[ d ] = resolutions[ level ][ d ] / resolutions[ l ][ d ];
						if ( f[ d ] * resolutions[ l ][ d ] != resolutions[ level ][ d ] )
							continue A;
					}
					factorsToPreviousLevel = f;
					previousLevel = l;
					break;
				}
				// Now, if previousLevel >= 0 we can use loopback ImgLoader on
				// previousLevel and downsample with factorsToPreviousLevel.
				//
				// whether it makes sense to actually do so is determined by a
				// heuristic based on the following considerations:
				// * if downsampling a lot over original image, the cost of
				//   reading images back from hdf5 outweighs the cost of
				//   accessing and averaging original pixels.
				// * original image may already be cached (for example when
				//   exporting an ImageJ virtual stack. To compute blocks
				//   that downsample a lot in Z, many planes of the virtual
				//   stack need to be accessed leading to cache thrashing if
				//   individual planes are very large.

				useLoopBack = loopbackHeuristic.decide( img, resolutions[ level ], previousLevel, factorsToPreviousLevel, subdivisions[ level ] );
				if ( useLoopBack )
				{
					sourceImg = loopback.getSetupImgLoader( setupIdPartition ).getImage( timepointIdPartition, previousLevel );
					factor = factorsToPreviousLevel;
				}
				else
				{
					sourceImg = img;
					factor = resolutions[ level ];
				}
			}

			sourceImg.dimensions( dimensions );
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

			final long[] minRequiredInput = new long[ n ];
			final long[] maxRequiredInput = new long[ n ];
			sourceImg.min( minRequiredInput );
			for ( int d = 0; d < n; ++d )
				maxRequiredInput[ d ] = minRequiredInput[ d ] + dimensions[ d ] * factor[ d ] - 1;
			final RandomAccessibleInterval< UnsignedShortType > extendedImg = Views.interval( Views.extendBorder( sourceImg ), new FinalInterval( minRequiredInput, maxRequiredInput ) );

			final int[] cellDimensions = subdivisions[ level ];
			final ViewId viewIdPartition = new ViewId( timepointIdPartition, setupIdPartition );
			final String path = Util.getCellsPath( viewIdPartition, level );
			writerQueue.createAndOpenDataset( path, dimensions.clone(), cellDimensions.clone(), storage );

			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			final long[] minCell = new long[ n ];
			final long[] maxCell = new long[ n ];
			for ( int d = 0; d < n; ++d )
			{
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				maxCell[ d ] = numCells[ d ] - 1;
				borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
			}

			// generate one "plane" of cells after the other to avoid cache thrashing when exporting from virtual stacks
			for ( int lastDimCell = 0; lastDimCell < numCells[ n - 1 ]; ++lastDimCell )
			{
				minCell[ n - 1 ] = lastDimCell;
				maxCell[ n - 1 ] = lastDimCell;
				final LocalizingIntervalIterator i = new LocalizingIntervalIterator( minCell, maxCell );

				final int numThreads = cellCreatorThreads.length;
				final CountDownLatch doneSignal = new CountDownLatch( numThreads );
				for ( int threadNum = 0; threadNum < numThreads; ++threadNum )
				{
					cellCreatorThreads[ threadNum ].run( new Runnable()
					{
						@Override
						public void run()
						{
							final double[] accumulator = fullResolution ? null : new double[ cellDimensions[ 0 ] * cellDimensions[ 1 ] * cellDimensions[ 2 ] ];
							final long[] currentCellMin = new long[ n ];
							final long[] currentCellMax = new long[ n ];
							final long[] currentCellDim = new long[ n ];
							final long[] currentCellPos = new long[ n ];
							final long[] blockMin = new long[ n ];
							final RandomAccess< UnsignedShortType > in = extendedImg.randomAccess();
							while ( true )
							{
								synchronized ( i )
								{
									if ( !i.hasNext() )
										break;
									i.fwd();
									i.localize( currentCellPos );
								}
								for ( int d = 0; d < n; ++d )
								{
									currentCellMin[ d ] = currentCellPos[ d ] * cellDimensions[ d ];
									blockMin[ d ] = currentCellMin[ d ] * factor[ d ];
									final boolean isBorderCellInThisDim = ( currentCellPos[ d ] + 1 == numCells[ d ] );
									currentCellDim[ d ] = isBorderCellInThisDim ? borderSize[ d ] : cellDimensions[ d ];
									currentCellMax[ d ] = currentCellMin[ d ] + currentCellDim[ d ] - 1;
								}

								final ArrayImg< UnsignedShortType, ? > cell = ArrayImgs.unsignedShorts( currentCellDim );
								if ( fullResolution )
									copyBlock( cell.randomAccess(), currentCellDim, in, blockMin );
								else
									downsampleBlock( cell.cursor(), accumulator, currentCellDim, in, blockMin, factor, scale );

								writerQueue.writeBlockWithOffset( ( ( ShortArray ) cell.update( null ) ).getCurrentStorageArray(), currentCellDim.clone(), currentCellMin.clone() );
							}
							doneSignal.countDown();
						}
					} );
				}
				try
				{
					doneSignal.await();
				}
				catch ( final InterruptedException e )
				{
					e.printStackTrace();
				}
				if ( afterEachPlane != null )
					afterEachPlane.afterEachPlane( useLoopBack );
			}
			writerQueue.closeDataset();
			progressWriter.setProgress( ( double ) numCompletedTasks++ / numTasks );
		}
		if ( loopback != null )
			loopback.close();
	}

	/**
	 * A heuristic to decide for a given resolution level whether the source
	 * pixels should be taken from the original image or read from a previously
	 * written resolution level in the hdf5 file.
	 */
	public interface LoopbackHeuristic
	{
		public boolean decide(
				final RandomAccessibleInterval< ? > originalImg,
				final int[] factorsToOriginalImg,
				final int previousLevel,
				final int[] factorsToPreviousLevel,
				final int[] chunkSize );
	}

	public interface AfterEachPlane
	{
		public void afterEachPlane( final boolean usedLoopBack );
	}

	/**
	 * Simple heuristic: use loopback image loader if saving 8 times or more on
	 * number of pixel access with respect to the original image.
	 *
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	public static class DefaultLoopbackHeuristic implements LoopbackHeuristic
	{
		@Override
		public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
		{
			if ( previousLevel < 0 )
				return false;

			if ( numElements( factorsToOriginalImg ) / numElements( factorsToPreviousLevel ) >= 8 )
				return true;

			return false;
		}
	}

	public static int numElements( final int[] size )
	{
		int numElements = size[ 0 ];
		for ( int d = 1; d < size.length; ++d )
			numElements *= size[ d ];
		return numElements;
	}

	public static CellCreatorThread[] createAndStartCellCreatorThreads( final int numThreads )
	{
		final CellCreatorThread[] cellCreatorThreads = new CellCreatorThread[ numThreads ];
		for ( int threadNum = 0; threadNum < numThreads; ++threadNum )
		{
			cellCreatorThreads[ threadNum ] = new CellCreatorThread();
			cellCreatorThreads[ threadNum ].setName( "CellCreatorThread " + threadNum );
			cellCreatorThreads[ threadNum ].start();
		}
		return cellCreatorThreads;
	}

	public static void stopCellCreatorThreads( final CellCreatorThread[] cellCreatorThreads )
	{
		for ( final CellCreatorThread thread : cellCreatorThreads )
			thread.interrupt();
	}

	public static class CellCreatorThread extends Thread
	{
		private Runnable currentTask = null;

		public synchronized void run( final Runnable task )
		{
			currentTask = task;
			notify();
		}

		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				synchronized ( this )
				{
					try
					{
						if ( currentTask == null )
							wait();
						else
						{
							currentTask.run();
							currentTask = null;
						}
					}
					catch ( final InterruptedException e )
					{
						break;
					}
				}
			}

		}
	}

	private static < T extends RealType< T > > void copyBlock( final RandomAccess< T > out, final long[] outDim, final RandomAccess< T > in, final long[] blockMin )
	{
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

	private static < T extends RealType< T > > void downsampleBlock( final Cursor< T > out, final double[] accumulator, final long[] outDim, final RandomAccess< UnsignedShortType > randomAccess, final long[] blockMin, final int[] blockSize, final double scale )
	{
		final int numBlockPixels = ( int ) ( outDim[ 0 ] * outDim[ 1 ] * outDim[ 2 ] );
		Arrays.fill( accumulator, 0, numBlockPixels, 0 );

		randomAccess.setPosition( blockMin );

		final int ox = ( int ) outDim[ 0 ];
		final int oy = ( int ) outDim[ 1 ];
		final int oz = ( int ) outDim[ 2 ];

		final int sx = ox * blockSize[ 0 ];
		final int sy = oy * blockSize[ 1 ];
		final int sz = oz * blockSize[ 2 ];

		int i = 0;
		for ( int z = 0, bz = 0; z < sz; ++z )
		{
			for ( int y = 0, by = 0; y < sy; ++y )
			{
				for ( int x = 0, bx = 0; x < sx; ++x )
				{
					accumulator[ i ] += randomAccess.get().getRealDouble();
					randomAccess.fwd( 0 );
					if ( ++bx == blockSize[ 0 ] )
					{
						bx = 0;
						++i;
					}
				}
				randomAccess.move( -sx, 0 );
				randomAccess.fwd( 1 );
				if ( ++by == blockSize[ 1 ] )
					by = 0;
				else
					i -= ox;
			}
			randomAccess.move( -sy, 1 );
			randomAccess.fwd( 2 );
			if ( ++bz == blockSize[ 2 ] )
				bz = 0;
			else
				i -= ox * oy;
		}

		for ( int j = 0; j < numBlockPixels; ++j )
			out.next().setReal( accumulator[ j ] * scale );
	}

	/**
	 * DEPRECATED. Use
	 * {@link #writeHdf5File(AbstractSequenceDescription, Map, boolean, File, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}
	 * instead.
	 */
	@Deprecated
	public static void writeHdf5File(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo,
			final boolean deflate,
			final File hdf5File,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final ProgressWriter progressWriter )
	{
		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() - 2 );
		writeHdf5File( seq, perSetupMipmapInfo, deflate, hdf5File, loopbackHeuristic, afterEachPlane, numThreads, progressWriter );
	}

	/**
	 * DEPRECATED. Use
	 * {@link #writeHdf5File(AbstractSequenceDescription, int[][], int[][], boolean, File, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}
	 * instead.
	 */
	@Deprecated
	public static void writeHdf5File(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final int[][] resolutions,
			final int[][] subdivisions,
			final boolean deflate,
			final File hdf5File,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final ProgressWriter progressWriter )
	{
		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() - 2 );
		writeHdf5File( seq, resolutions, subdivisions, deflate, hdf5File, loopbackHeuristic, afterEachPlane, numThreads, progressWriter );
	}

	/**
	 * DEPRECATED. Use
	 * {@link #writeHdf5PartitionFile(AbstractSequenceDescription, Map, boolean, Partition, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}
	 * instead.
	 */
	@Deprecated
	public static void writeHdf5PartitionFile(
			final AbstractSequenceDescription< ?, ?, ? > seq,
			final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo,
			final boolean deflate,
			final Partition partition,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			final ProgressWriter progressWriter )
	{
		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() - 2 );
		writeHdf5PartitionFile( seq, perSetupMipmapInfo, deflate, partition, loopbackHeuristic, afterEachPlane, numThreads, progressWriter );
	}

	/**
	 * DEPRECATED. Use
	 * {@link #writeViewToHdf5PartitionFile(RandomAccessibleInterval, Partition, int, int, ExportMipmapInfo, boolean, boolean, LoopbackHeuristic, AfterEachPlane, int, ProgressWriter)}
	 * instead.
	 */
	@Deprecated
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
			final ProgressWriter progressWriter )
	{
		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() - 2 );
		writeViewToHdf5PartitionFile( img, partition, timepointIdPartition, setupIdPartition, mipmapInfo, writeMipmapInfo, deflate, loopbackHeuristic, afterEachPlane, numThreads, progressWriter );
	}
}
