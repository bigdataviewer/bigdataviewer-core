/*-
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.imglib2.parallel.Parallelization;
import net.imglib2.util.StopWatch;

/**
 * A {@code VolatileProjector} that iterates over a list of other {@code
 * VolatileProjector}s, one for each rendered tile.
 */
class TiledProjector implements VolatileProjector
{
	/**
	 * Projectors for individual tiles.
	 */
	private List< VolatileProjector > tileProjectors;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	private long lastFrameRenderNanoTime;

	private volatile boolean canceled = false;

	private boolean valid = false;

	TiledProjector( final List< VolatileProjector > tileProjectors )
	{
		this.tileProjectors = tileProjectors;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public void cancel()
	{
		canceled = true;
		for ( VolatileProjector tileProjector : tileProjectors )
			tileProjector.cancel();
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	/**
	 * Returns {@code true}, if all tile projectors are {@link VolatileProjector#isValid() valid}.
	 *
	 * @return {@code true}, if all tile projectors are valid.
	 */
	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Call {@code map()} for each tile projector that is not {@link VolatileProjector#isValid() valid} yet.
	 * Returns {@code true}, if all tile {@code map()}s returned {@code true}.
	 *
	 * @param clearUntouchedTargetPixels
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled )
			return false;

		if ( isValid() )
			return true;

		final StopWatch stopWatch = StopWatch.createAndStart();
		ForkJoinTask.invokeAll(
				tileProjectors.stream()
						.map( p -> ForkJoinTask.adapt( () -> p.map( clearUntouchedTargetPixels ) ) )
						.collect( Collectors.toList() ) );
		if ( canceled )
			return false;
		tileProjectors = tileProjectors.stream()
				.filter( p -> !p.isValid() )
				.collect( Collectors.toList() );
		lastFrameRenderNanoTime = stopWatch.nanoTime();
		valid = tileProjectors.isEmpty();
		return !canceled;
	}
}
