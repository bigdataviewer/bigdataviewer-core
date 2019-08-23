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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Write an image to a chunked mipmap representation.
 */
public class ExportScalePyramid
{
	/**
	 * A heuristic to decide for a given resolution level whether the source
	 * pixels should be taken from the original image or read from a previously
	 * written resolution level in the output dataset.
	 */
	public interface LoopbackHeuristic
	{
		/**
		 * @return {@code true} if source pixels should be read back from
		 *         dataset. {@code false} if source pixels should be taken from
		 *         original image.
		 */
		boolean decide(
				final RandomAccessibleInterval< ? > originalImg,
				final int[] factorsToOriginalImg,
				final int previousLevel,
				final int[] factorsToPreviousLevel,
				final int[] chunkSize );
	}

	/**
	 * Simple heuristic: use loopback image loader if saving 8 times or more on
	 * number of pixel access with respect to the original image.
	 */
	public static class DefaultLoopbackHeuristic implements LoopbackHeuristic
	{
		@Override
		public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
		{
			if ( previousLevel < 0 )
				return false;

			if ( Intervals.numElements( factorsToOriginalImg ) / Intervals.numElements( factorsToPreviousLevel ) >= 8 )
				return true;

			return false;
		}
	}

	/**
	 * Callback that is called after each "plane of blocks" is written, giving
	 * the opportunity to clear caches, etc.
	 */
	public interface AfterEachPlane
	{
		/**
		 * Called after a "plane of blocks" is written.
		 *
		 * @param usedLoopBack
		 *            {@code true}, if source was previously written resolution
		 *            level in the output dataset. {@code false}, if source was
		 *            the original image.
		 */
		void afterEachPlane( final boolean usedLoopBack );
	}

	/**
	 * A block to be written. See {@link DatasetIO#writeBlock(Object, Block)
	 * DatasetIO.writeBlock()}.
	 */
	public static class Block< T extends NativeType< T > >
	{
		final SingleCellArrayImg< T, ? > data;
		final int[] size;
		final long[] position;

		Block( final SingleCellArrayImg< T, ? > data, final int[] size, final long[] position )
		{
			this.data = data;
			this.size = size;
			this.position = position;
		}

		public SingleCellArrayImg< T, ? > getData()
		{
			return data;
		}

		public int[] getSize()
		{
			return size;
		}

		public long[] getGridPosition()
		{
			return position;
		}
	}

	/**
	 * Writing and reading back data for each resolution level.
	 *
	 * @param <D>
	 *            Dataset handle
	 * @param <T>
	 *            Pixel type
	 */
	public interface DatasetIO< D, T extends NativeType< T > >
	{
		/**
		 * Create a dataset for the image of the given resolution {@code level}.
		 *
		 * @return a handle to the dataset.
		 */
		D createDataset(
				final int level,
				final long[] dimensions,
				final int[] blockSize ) throws IOException;

		/**
		 * Write the given {@code dataBlock} to the {@code dataset}.
		 */
		void writeBlock(
				final D dataset,
				final Block< T > dataBlock ) throws IOException;

		/**
		 * Blocks until all pending data was written to {@code dataset}.
		 */
		void flush( D dataset ) throws IOException;

		/**
		 * Opens a dataset that was already written as a
		 * {@code RaπdomAccessibleInterval}.
		 */
		default RandomAccessibleInterval< T > getImage( final int level ) throws IOException
		{
			return null;
		}
	}

	/**
	 * Write an image to a chunked mipmap representation.
	 *
	 * @param img
	 *            the image to be written.
	 * @param type
	 *            instance of the pixel type of the image.
	 * @param mipmapInfo
	 *            contains for each mipmap level of the setup, the subsampling
	 *            factors and block sizes.
	 * @param io
	 *            writer for image blocks.
	 * @param executorService
	 *            ExecutorService where block-creator tasks are submitted.
	 * @param numThreads
	 *            How many block-creator tasks to run in parallel. (This many
	 *            tasks are submitted to the @code
	 * @param loopbackHeuristic
	 *            heuristic to decide whether to create each resolution level by
	 *            reading pixels from the original image or by reading back a
	 *            finer resolution level already written to the hdf5. may be
	 *            null (in this case always use the original image).
	 * @param afterEachPlane
	 *            this is called after each "plane of blocks" is written, giving
	 *            the opportunity to clear caches, etc. may be null.
	 * @param progressWriter
	 *            completion ratio and status output will be directed here. may
	 *            be null.
	 *
	 * @param <T>
	 *            Pixel type
	 * @param <D>
	 *            Dataset handle
	 *
	 * @throws IOException
	 */
	public static < T extends RealType< T > & NativeType< T >, D > void writeScalePyramid(
			final RandomAccessibleInterval< T > img,
			final T type,
			final ExportMipmapInfo mipmapInfo,
			final DatasetIO< D, T > io,
			final ExecutorService executorService,
			final int numThreads,
			final LoopbackHeuristic loopbackHeuristic,
			final AfterEachPlane afterEachPlane,
			ProgressWriter progressWriter ) throws IOException
	{
		final BlockCreator< T > blockCreator = BlockCreator.forType( type );

		if ( progressWriter == null )
			progressWriter = new ProgressWriterNull();

		// for progressWriter
		final int numTasks = mipmapInfo.getNumLevels();
		int numCompletedTasks = 0;
		progressWriter.setProgress( 0.0 );

		// write image data for all views to the HDF5 file
		final int n = 3; // TODO checkNumDimensions( img.numDimensions() );
		final long[] dimensions = new long[ n ];

		final int[][] resolutions = mipmapInfo.getExportResolutions();
		final int[][] subdivisions = mipmapInfo.getSubdivisions();
		final int numLevels = mipmapInfo.getNumLevels();

		for ( int level = 0; level < numLevels; ++level )
		{
			progressWriter.out().println( "writing level " + level );

			boolean useLoopBack = false;
			int[] factorsToPreviousLevel = null;
			RandomAccessibleInterval< T > loopbackImg = null;
			if ( loopbackHeuristic != null )
			{
				// Are downsampling factors a multiple of a level that we have
				// already written?
				int previousLevel = -1;
				A:
				for ( int l = level - 1; l >= 0; --l )
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

				if ( previousLevel >= 0 )
					useLoopBack = loopbackHeuristic.decide( img, resolutions[ level ], previousLevel, factorsToPreviousLevel, subdivisions[ level ] );

				if ( useLoopBack )
					loopbackImg = io.getImage( previousLevel );

				if ( loopbackImg == null )
					useLoopBack = false;
			}

			final RandomAccessibleInterval< T > sourceImg;
			final int[] factor;
			if ( useLoopBack )
			{
				sourceImg = loopbackImg;
				factor = factorsToPreviousLevel;
			}
			else
			{
				sourceImg = img;
				factor = resolutions[ level ];
			}

			sourceImg.dimensions( dimensions );

			final long size = Intervals.numElements( factor );
			final boolean fullResolution = size == 1;
			if ( !fullResolution )
			{
				for ( int d = 0; d < n; ++d )
					dimensions[ d ] = Math.max( dimensions[ d ] / factor[ d ], 1 );
			}

			final long[] minRequiredInput = new long[ n ];
			final long[] maxRequiredInput = new long[ n ];
			sourceImg.min( minRequiredInput );
			for ( int d = 0; d < n; ++d )
				maxRequiredInput[ d ] = minRequiredInput[ d ] + dimensions[ d ] * factor[ d ] - 1;

			// TODO: pass OutOfBoundsFactory
			final RandomAccessibleInterval< T > extendedImg = Views.interval( Views.extendBorder( sourceImg ), new FinalInterval( minRequiredInput, maxRequiredInput ) );

			final int[] cellDimensions = subdivisions[ level ];
			final D dataset = io.createDataset( level, dimensions, cellDimensions );

			final ProgressWriter subProgressWriter = new SubTaskProgressWriter(
					progressWriter, ( double ) numCompletedTasks / numTasks,
					( double ) ( numCompletedTasks + 1 ) / numTasks );
			// generate one "plane" of cells after the other to avoid cache thrashing when exporting from virtual stacks
			final CellGrid grid = new CellGrid( dimensions, cellDimensions );
			final long[] numCells = grid.getGridDimensions();
			final long numBlocksPerPlane = numElements( numCells, 0, 2 );
			final long numPlanes = numElements( numCells, 2, n );
			for ( int plane = 0; plane < numPlanes; ++plane )
			{
				final long planeBaseIndex = numBlocksPerPlane * plane;
				final AtomicInteger nextCellInPlane = new AtomicInteger();
				final List< Callable< Void > > tasks = new ArrayList<>();
				for ( int threadNum = 0; threadNum < numThreads; ++threadNum )
				{
					tasks.add( () -> {
						final long[] currentCellMin = new long[ n ];
						final int[] currentCellDim = new int[ n ];
						final long[] currentCellPos = new long[ n ];
						final long[] blockMin = new long[ n ];
						final RandomAccess< T > in = extendedImg.randomAccess();

						final Class< ? extends RealType > kl1 = type.getClass();
						final Class< ? extends RandomAccess > kl2 = in.getClass();
						final CopyBlock< T > copyBlock = fullResolution ? CopyBlock.create( n, kl1, kl2 ) : null;
						final DownsampleBlock< T > downsampleBlock = fullResolution ? null : DownsampleBlock.create( cellDimensions, factor, kl1, kl2 );

						for ( int i = nextCellInPlane.getAndIncrement(); i < numBlocksPerPlane; i = nextCellInPlane.getAndIncrement() )
						{
							final long index = planeBaseIndex + i;

							grid.getCellDimensions( index, currentCellMin, currentCellDim );
							grid.getCellGridPositionFlat( index, currentCellPos );
							final Block< T > block = blockCreator.create( currentCellDim, currentCellMin, currentCellPos );

							if ( fullResolution )
							{
								final RandomAccess< T > out = block.getData().randomAccess();
								in.setPosition( currentCellMin );
								out.setPosition( currentCellMin );
								copyBlock.copyBlock( in, out, currentCellDim );
							}
							else
							{
								for ( int d = 0; d < n; ++d )
									blockMin[ d ] = currentCellMin[ d ] * factor[ d ];
								in.setPosition( blockMin );
								downsampleBlock.downsampleBlock( in, block.getData().cursor(), currentCellDim );
							}

							io.writeBlock( dataset, block );
						}
						return null;
					} );
				}
				try
				{
					final List< Future< Void > > futures = executorService.invokeAll( tasks );
					for ( final Future< Void > future : futures )
						future.get();
				}
				catch ( final InterruptedException | ExecutionException e )
				{
					// TODO...
					e.printStackTrace();
					throw new IOException( e );
				}
				if ( afterEachPlane != null )
					afterEachPlane.afterEachPlane( useLoopBack );

				subProgressWriter.setProgress( ( double ) plane / numPlanes );
			}
			io.flush( dataset );
			progressWriter.setProgress( ( double ) ++numCompletedTasks / numTasks );
		}
	}

	private static long numElements( final long[] size, final int mind, final int maxd )
	{
		long numElements = 1;
		for ( int d = mind; d < maxd; ++d )
			numElements *= size[ d ];
		return numElements;
	}

	private interface BlockCreator< T extends NativeType< T > >
	{
		Block< T > create( final int[] blockSize, final long[] blockMin, final long[] gridPosition );

		static < T extends NativeType< T > & RealType< T >, A extends ArrayDataAccess< A > > BlockCreator< T > forType( final T type )
		{
			final A accessFactory = Cast.unchecked( ArrayDataAccessFactory.get( type ) );
			final NativeTypeFactory< T, A > nativeTypeFactory = Cast.unchecked( type.getNativeTypeFactory() );
			return ( blockSize, blockMin, gridPosition ) -> {
				final A data = accessFactory.createArray( ( int ) Intervals.numElements( blockSize ) );
				final SingleCellArrayImg< T, A > img = new SingleCellArrayImg<>( blockSize, blockMin, data, null );
				img.setLinkedType( nativeTypeFactory.createLinkedType( img ) );
				return new Block<>( img, blockSize, gridPosition );
			};
		}
	}
}
