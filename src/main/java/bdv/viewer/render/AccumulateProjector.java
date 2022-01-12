/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.StopWatch;
import net.imglib2.view.Views;

// TODO javadoc
public abstract class AccumulateProjector< A, B > implements VolatileProjector
{
	/**
	 * Projectors that render the source images to accumulate.
	 * For every rendering pass, ({@link VolatileProjector#map(boolean)}) is run on each source projector that is not yet {@link VolatileProjector#isValid() valid}.
	 */
	private final List< VolatileProjector > sourceProjectors;

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
     * Number of threads to use for rendering
     */
    private final int numThreads;

	private final ExecutorService executorService;

    /**
     * Time needed for rendering the last frame, in nano-seconds.
     */
    private long lastFrameRenderNanoTime;

	private final AtomicBoolean canceled = new AtomicBoolean();

	private volatile boolean valid = false;

	public AccumulateProjector(
			final List< VolatileProjector > sourceProjectors,
			final List< ? extends RandomAccessible< ? extends A > > sources,
			final RandomAccessibleInterval< B > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		this.sourceProjectors = sourceProjectors;
		this.sources = new ArrayList<>();
		for ( final RandomAccessible< ? extends A > source : sources )
			this.sources.add( Views.flatIterable( Views.interval( source, target ) ) );
		this.target = target;
		this.iterableTarget = Views.flatIterable( target );
		this.numThreads = numThreads;
		this.executorService = executorService;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		canceled.set( false );

		final StopWatch stopWatch = StopWatch.createAndStart();

		valid = true;
		for ( final VolatileProjector p : sourceProjectors )
			if ( !p.isValid() )
				if ( !p.map( clearUntouchedTargetPixels ) )
					return false;
				else
					valid &= p.isValid();

		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );
		final int size = width * height;

		final int numTasks = numThreads <= 1 ? 1 : Math.min( numThreads * 10, height );
		final double taskLength = ( double ) size / numTasks;
		final int[] taskOffsets = new int[ numTasks + 1 ];
		for ( int i = 0; i < numTasks; ++i )
			taskOffsets[ i ] = ( int ) ( i * taskLength );
		taskOffsets[ numTasks ] = size;

		final boolean createExecutor = ( executorService == null );
		final ExecutorService ex = createExecutor ? Executors.newFixedThreadPool( numThreads ) : executorService;

		final List< Callable< Void > > tasks = new ArrayList<>( numTasks );
		for( int i = 0; i < numTasks; ++i )
			tasks.add( createMapTask( taskOffsets[ i ], taskOffsets[ i + 1 ] ) );
		try
		{
			ex.invokeAll( tasks );
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
		}
		if ( createExecutor )
			ex.shutdown();

		lastFrameRenderNanoTime = stopWatch.nanoTime();

		return !canceled.get();
	}

	/**
	 * @return a {@code Callable} that runs {@code map(startOffset, endOffset)}
	 */
	private Callable< Void > createMapTask( final int startOffset, final int endOffset )
	{
		return Executors.callable( () -> map( startOffset, endOffset ), null );
	}

	/**
	 * Accumulate pixels from {@code startOffset} up to {@code endOffset}
	 * (exclusive) of all sources to target. Before starting, check
	 * whether rendering was {@link #cancel() canceled}.
	 *
	 * @param startOffset
	 *     pixel range start (flattened index)
	 * @param endOffset
	 *     pixel range end (exclusive, flattened index)
	 */
	private void map( final int startOffset, final int endOffset )
	{
		if ( canceled.get() )
			return;

		final int numSources = sources.size();
		final Cursor< ? extends A >[] sourceCursors = new Cursor[ numSources ];
		for ( int s = 0; s < numSources; ++s )
		{
			final Cursor< ? extends A > c = sources.get( s ).cursor();
			c.jumpFwd( startOffset );
			sourceCursors[ s ] = c;
		}
		final Cursor< B > targetCursor = iterableTarget.cursor();
		targetCursor.jumpFwd( startOffset );

		final int size = endOffset - startOffset;
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
		canceled.set( true );
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
