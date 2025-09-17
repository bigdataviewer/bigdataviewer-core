/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.viewer.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.iotiming.IoStatistics;
import net.imglib2.converter.Converter;
import net.imglib2.type.operators.SetZero;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;
import net.imglib2.view.Views;

/**
 * {@link VolatileProjector} for a hierarchy of {@link Volatile} inputs.  After each
 * {@link #map()} call, the projector has a {@link #isValid() state} that
 * signalizes whether all projected pixels were perfect.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
abstract class AbstractVolatileHierarchyProjector< A, B extends SetZero > implements VolatileProjector
{
	/**
	 * A converter from the source pixel type to the target pixel type.
	 */
	final Converter< ? super A, B > converter;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link #map}
	 */
	private final RandomAccessibleInterval< B > target;

	/**
	 * List of source resolutions starting with the optimal resolution at index
	 * 0. During each {@link #map(boolean)}, for every pixel, resolution levels
	 * are successively queried until a valid pixel is found.
	 */
	private final List< RandomAccessible< A > > sources;

	/**
	 * Records, for every target pixel, the best (smallest index) source
	 * resolution level that has provided a valid value. Only better (lower
	 * index) resolutions are re-tried in successive {@link #map(boolean)}
	 * calls.
	 */
	final byte[] mask;

	/**
	 * {@code true} iff all target pixels were rendered with valid data from the
	 * optimal resolution level (level {@code 0}).
	 */
	private volatile boolean valid = false;

	/**
	 * How many levels (starting from level {@code 0}) have to be re-rendered in
	 * the next rendering pass, i.e., {@code map()} call.
	 */
	private int numInvalidLevels;

	/**
	 * Source interval which will be used for rendering. This is the 2D target
	 * interval expanded to source dimensionality (usually 3D) with
	 * {@code min=max=0} in the additional dimensions.
	 */
	private final FinalInterval sourceInterval;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 * This does not include time spent in blocking IO.
	 */
	private long lastFrameRenderNanoTime;

	/**
	 * Time spent in blocking IO rendering the last frame, in nano-seconds.
	 */
	private long lastFrameIoNanoTime; // TODO move to derived implementation for local sources only

	/**
	 * temporary variable to store the number of invalid pixels in the current
	 * rendering pass.
	 */
	private int numInvalidPixels;

	/**
	 * Flag to indicate that someone is trying to {@link #cancel()} rendering.
	 */
	private volatile boolean canceled = false;

	AbstractVolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final byte[] maskArray)
	{
		this.converter = converter;
		this.target = target;
		this.sources = new ArrayList<>( sources );
		numInvalidLevels = sources.size();
		mask = maskArray;

		final int n = Math.max( 2, sources.get( 0 ).numDimensions() );
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		min[ 0 ] = target.min( 0 );
		max[ 0 ] = target.max( 0 );
		min[ 1 ] = target.min( 1 );
		max[ 1 ] = target.max( 1 );
		sourceInterval = new FinalInterval( min, max );

		lastFrameRenderNanoTime = -1;
		clearMask();
	}

	@Override
	public void cancel()
	{
		canceled = true;
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	public long getLastFrameIoNanoTime()
	{
		return lastFrameIoNanoTime;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Set all pixels in target to 100% transparent zero, and mask to all
	 * Integer.MAX_VALUE.
	 */
	public void clearMask()
	{
		final int size = ( int ) Intervals.numElements( target );
		Arrays.fill( mask, 0, size, Byte.MAX_VALUE );
		numInvalidLevels = sources.size();
	}

	/**
	 * Clear target pixels that were never written.
	 */
	private void clearUntouchedTargetPixels()
	{
		int i = 0;
		for ( final B t : Views.flatIterable( target ) )
			if ( mask[ i++ ] == Byte.MAX_VALUE )
				t.setZero();
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled )
			return false;

		valid = false;

		final StopWatch stopWatch = StopWatch.createAndStart();
		final IoStatistics iostat = CacheIoTiming.getIoStatistics();
		final long startTimeIo = iostat.getIoNanoTime();

		/*
		 * After the for loop, resolutionLevel is the highest (coarsest)
		 * resolution for which all pixels could be filled from valid data. This
		 * means that in the next pass, i.e., map() call, levels up to
		 * resolutionLevel have to be re-rendered.
		 */
		int resolutionLevel;
		for ( resolutionLevel = 0; resolutionLevel < numInvalidLevels; ++resolutionLevel )
		{
			numInvalidPixels = 0;
			map( ( byte ) resolutionLevel);
			if ( canceled )
				return false;
			if ( numInvalidPixels == 0 )
				// if this pass was all valid
				numInvalidLevels = resolutionLevel;
		}

		if ( clearUntouchedTargetPixels && numInvalidPixels != 0 && !canceled )
			clearUntouchedTargetPixels();

		lastFrameIoNanoTime = iostat.getIoNanoTime() - startTimeIo;
		final long lastFrameTime = stopWatch.nanoTime();

		// TODO (FORKJOIN): This is inaccurate now, should only use the io time used by current thread
		//  --> requires additional API in IoStatistics (imglib2-cache)
		lastFrameRenderNanoTime = lastFrameTime - lastFrameIoNanoTime;

		valid = numInvalidLevels == 0;

		return !canceled;
	}

	/**
	 * Copy lines from {@code y = startHeight} up to {@code endHeight}
	 * (exclusive) from source {@code resolutionIndex} to target. Check after
	 * each line whether rendering was {@link #cancel() canceled}.
	 * <p>
	 * Only valid source pixels with a current mask value
	 * {@code mask>resolutionIndex} are copied to target, and their mask value
	 * is set to {@code mask=resolutionIndex}. Invalid source pixels are
	 * ignored. Pixels with {@code mask<=resolutionIndex} are ignored, because
	 * they have already been written to target during a previous pass.
	 * <p>
	 *
	 * @param resolutionIndex
	 *     index of source resolution level
	 */
	private void map( final byte resolutionIndex )
	{
		if ( canceled )
			return;

		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
		final RandomAccess< A > sourceRandomAccess = sources.get( resolutionIndex ).randomAccess( sourceInterval );
		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );
		final long[] smin = Intervals.minAsLongArray( sourceInterval );
		int myNumInvalidPixels = 0;

		for ( int y = 0; y < height; ++y )
		{
			// TODO (FORKJOIN) With tiles being granular enough, probably
			//   projectors shouldn't check after each line for cancellation
			//   (maybe not at all).
			if ( canceled )
				return;

			sourceRandomAccess.setPosition( smin );
			targetRandomAccess.setPosition( smin );
			myNumInvalidPixels += processLine( sourceRandomAccess, targetRandomAccess, resolutionIndex, width, y );
			++smin[ 1 ];
		}

		numInvalidPixels += myNumInvalidPixels;
	}

	abstract int processLine(RandomAccess< A > sourceRandomAccess, RandomAccess< B > targetRandomAccess, byte resolutionIndex, int width, int y );
}
