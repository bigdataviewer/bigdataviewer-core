/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;
import net.imglib2.view.Views;

// TODO javadoc
public abstract class AccumulateProjector< A, B > implements VolatileProjector
{
	/**
	 * Projectors that render the source images to accumulate.
	 * For every rendering pass, ({@link VolatileProjector#map(boolean)}) is run on each source projector that is not yet {@link VolatileProjector#isValid() valid}.
	 */
	private List< VolatileProjector > sourceProjectors;

	/**
	 * The source images to accumulate
	 */
	private final List< IterableInterval< ? extends A > > sources;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link #map}
	 */
	private final RandomAccessibleInterval< B > target;

	/**
	 * A reference to the target image as an iterable.  Used for source-less
	 * operations such as clearing its content.
	 */
	private final IterableInterval< B > iterableTarget;

    /**
     * Time needed for rendering the last frame, in nano-seconds.
     */
    private long lastFrameRenderNanoTime;

	private volatile boolean canceled = false;

	private volatile boolean valid = false;

	/**
	 * @deprecated
	 * Use {@link #AccumulateProjector(List, List, RandomAccessibleInterval)}.
	 * The numThreads and executorService arguments are ignored.
	 */
	@Deprecated
	public AccumulateProjector(
			final List< VolatileProjector > sourceProjectors,
			final List< ? extends RandomAccessible< ? extends A > > sources,
			final RandomAccessibleInterval< B > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		this( sourceProjectors, sources, target );
	}

	public AccumulateProjector(
			final List< VolatileProjector > sourceProjectors,
			final List< ? extends RandomAccessible< ? extends A > > sources,
			final RandomAccessibleInterval< B > target )
	{
		this.sourceProjectors = sourceProjectors;
		this.sources = new ArrayList<>();
		for ( final RandomAccessible< ? extends A > source : sources )
			this.sources.add( Views.flatIterable( Views.interval( source, target ) ) );
		this.target = target;
		this.iterableTarget = Views.flatIterable( target );
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled )
			return false;

		if ( isValid() )
			return true;

		final StopWatch stopWatch = StopWatch.createAndStart();

		if ( Intervals.numElements( target ) < Tiling.MIN_ACCUMULATE_FORK_SIZE )
		{
			sourceProjectors.forEach( p -> p.map( clearUntouchedTargetPixels ) );
		}
		else
		{
			ForkJoinTask.invokeAll(
					sourceProjectors.stream()
							.map( p -> ForkJoinTask.adapt( () -> p.map( clearUntouchedTargetPixels ) ) )
							.collect( Collectors.toList() ) );
		}
		if ( canceled )
			return false;
		mapAccumulate();
		sourceProjectors = sourceProjectors.stream()
				.filter( p -> !p.isValid() )
				.collect( Collectors.toList() );
		lastFrameRenderNanoTime = stopWatch.nanoTime();
		valid = sourceProjectors.isEmpty();
		return !canceled;
	}

	/**
	 * Accumulate pixels of all sources to target. Before starting, check
	 * whether rendering was {@link #cancel() canceled}.
	 */
	private void mapAccumulate()
	{
		if ( canceled )
			return;

		final int numSources = sources.size();
		final Cursor< ? extends A >[] sourceCursors = new Cursor[ numSources ];
		Arrays.setAll( sourceCursors, s -> sources.get( s ).cursor() );
		final Cursor< B > targetCursor = iterableTarget.cursor();

		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );
		final int size = width * height;
		for ( int i = 0; i < size; ++i )
		{
			for ( int s = 0; s < numSources; ++s )
				sourceCursors[ s ].fwd();
			accumulate( sourceCursors, targetCursor.next() );
		}
	}

	protected abstract void accumulate( final Cursor< ? extends A >[] accesses, final B target );

	@Override
	public void cancel()
	{
		canceled = true;
		for ( final VolatileProjector p : sourceProjectors )
			p.cancel();
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}
}
