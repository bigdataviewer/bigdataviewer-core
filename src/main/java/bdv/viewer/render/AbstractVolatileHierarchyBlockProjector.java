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

import java.util.Arrays;

import net.imglib2.Interval;
import net.imglib2.Volatile;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.iotiming.IoStatistics;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;
import net.imglib2.util.Util;

/**
 * {@link VolatileProjector} for a hierarchy of {@link Volatile} inputs.  After each
 * {@link #map()} call, the projector has a {@link #isValid() state} that
 * signalizes whether all projected pixels were perfect.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
abstract class AbstractVolatileHierarchyBlockProjector implements VolatileProjector
{
	/**
	 * Records, for every target pixel, the best (smallest index) source
	 * resolution level that has provided a valid value. Only better (lower
	 * index) resolutions are re-tried in successive {@link #map(boolean)}
	 * calls.
	 */
	final byte[] mask;

	/**
	 * Number of elements in the target interval.
	 */
	final int length;

	/**
	 * Source interval which will be used for rendering. This is the 2D target
	 * interval expanded to source dimensionality (usually 3D) with
	 * {@code min=max=0} in the additional dimensions.
	 */
	final BlockInterval sourceInterval;

	/**
	 * How many sources (resolution levels) there are.
	 */
	private final int numSources;

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
	 * Time needed for rendering the last frame, in nano-seconds.
	 * This does not include time spent in blocking IO.
	 */
	private long lastFrameRenderNanoTime;

	/**
	 * Time spent in blocking IO rendering the last frame, in nano-seconds.
	 */
	private long lastFrameIoNanoTime; // TODO move to derived implementation for local sources only

	/**
	 * Flag to indicate that someone is trying to {@link #cancel()} rendering.
	 */
	private volatile boolean canceled = false;

	/**
	 */
	AbstractVolatileHierarchyBlockProjector(
			final int numSources,
			final int numSourceDimensions,
			final Interval target,
			final byte[] maskArray )
	{
		this.numSources = numSources;
		numInvalidLevels = numSources;
		mask = maskArray;

		sourceInterval = new BlockInterval( numSourceDimensions );
		Arrays.setAll( sourceInterval.min(), d -> d < 2 ? target.min( d ) : 0 );
		Arrays.setAll( sourceInterval.size(), d -> d < 2 ? Util.safeInt( target.dimension( d ) ) : 1 );

		length = Util.safeInt( Intervals.numElements( sourceInterval ) );

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
	 * Set all mask pixels to Integer.MAX_VALUE.
	 */
	public void clearMask()
	{
		Arrays.fill( mask, 0, length, Byte.MAX_VALUE );
		numInvalidLevels = numSources;
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
		 * After the for loop, numInvalidLevels is the highest (coarsest)
		 * resolution for which all pixels could be filled from valid data. This
		 * means that in the next pass, i.e., map() call, levels up to
		 * numInvalidLevels have to be re-rendered.
		 */
		for ( int resolutionLevel = 0; resolutionLevel < numInvalidLevels; ++resolutionLevel )
		{
			int numInvalidPixels = map( ( byte ) resolutionLevel);
			if ( canceled )
				return false;
			if ( numInvalidPixels == 0 )
				// if this pass was all valid
				numInvalidLevels = resolutionLevel;
		}

		valid = numInvalidLevels == 0;

		// TODO: REMOVE!?
		//		if ( clearUntouchedTargetPixels && valid && !canceled )
		//			clearUntouchedTargetPixels();

		if ( !canceled )
			convert();

		lastFrameIoNanoTime = iostat.getIoNanoTime() - startTimeIo;
		final long lastFrameTime = stopWatch.nanoTime();

		// TODO (FORKJOIN): This is inaccurate now, should only use the io time used by current thread
		//  --> requires additional API in IoStatistics (imglib2-cache)
		lastFrameRenderNanoTime = lastFrameTime - lastFrameIoNanoTime;

		return !canceled;
	}

	/**
	 * Render source {@code resolutionIndex} to the temporary target buffer.
	 * <p>
	 * Only valid source pixels with a current mask value
	 * {@code mask>resolutionIndex} are copied to target, and their mask value
	 * is set to {@code mask=resolutionIndex}. Invalid source pixels are
	 * ignored. Pixels with {@code mask<=resolutionIndex} are ignored, because
	 * they have already been written to target during a previous pass.
	 *
	 * @param resolutionIndex
	 *     index of source resolution level
	 * @return the number of pixels that remain invalid afterward
	 */
	abstract int map( final byte resolutionIndex );

	/**
	 * Convert the temporary target buffer to target type {@code B} and copy to
	 * {@code target}.
	 */
	abstract void convert();
}
