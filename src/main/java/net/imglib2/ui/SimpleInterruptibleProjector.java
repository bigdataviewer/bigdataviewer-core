/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
package net.imglib2.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

/**
 * An {@link InterruptibleProjector}, that renders a target 2D
 * {@link RandomAccessibleInterval} by copying values from a source
 * {@link RandomAccessible}. The source can have more dimensions than the
 * target. Target coordinate <em>(x,y)</em> is copied from source coordinate
 * <em>(x,y,0,...,0)</em>.
 * <p>
 * A specified number of threads is used for rendering.
 *
 * @param <A>
 *            pixel type of the source {@link RandomAccessible}.
 * @param <B>
 *            pixel type of the target {@link RandomAccessibleInterval}.
 *
 * @author Tobias Pietzsch
 * @author Stephan Saalfeld
 */
public class SimpleInterruptibleProjector< A, B > implements InterruptibleProjector
{
	/**
	 * A converter from the source pixel type to the target pixel type.
	 */
	protected final Converter< ? super A, B > converter;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link InterruptibleProjector#map()}
	 */
	protected final RandomAccessibleInterval< B > target;

	protected final RandomAccessible< A > source;

	/**
	 * Source interval which will be used for rendering. This is the 2D target
	 * interval expanded to source dimensionality (usually 3D) with
	 * {@code min=max=0} in the additional dimensions.
	 */
	protected final FinalInterval sourceInterval;

	/**
	 * Number of threads to use for rendering
	 */
	protected final int numThreads;

	protected final ExecutorService executorService;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	protected long lastFrameRenderNanoTime;

	protected AtomicBoolean interrupted = new AtomicBoolean();

	/**
	 * Create new projector with the given source and a converter from source to
	 * target pixel type.
	 *
	 * @param source
	 *            source pixels.
	 * @param converter
	 *            converts from the source pixel type to the target pixel type.
	 * @param target
	 *            the target interval that this projector maps to
	 * @param numThreads
	 *            how many threads to use for rendering.
	 */
	public SimpleInterruptibleProjector(
			final RandomAccessible< A > source,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads )
	{
		this( source, converter, target, numThreads, null );
	}

	public SimpleInterruptibleProjector(
			final RandomAccessible< A > source,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		this.converter = converter;
		this.target = target;
		this.source = source;

		final int n = Math.max( 2, source.numDimensions() );
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		min[ 0 ] = target.min( 0 );
		max[ 0 ] = target.max( 0 );
		min[ 1 ] = target.min( 1 );
		max[ 1 ] = target.max( 1 );
		sourceInterval = new FinalInterval( min, max );
		this.numThreads = numThreads;
		this.executorService = executorService;
		lastFrameRenderNanoTime = -1;
	}

	@Override
	public void cancel()
	{
		interrupted.set( true );
	}

	@Override
	public long getLastFrameRenderNanoTime()
	{
		return lastFrameRenderNanoTime;
	}

	/**
	 * Render the 2D target image by copying values from the source. Source can
	 * have more dimensions than the target. Target coordinate <em>(x,y)</em> is
	 * copied from source coordinate <em>(x,y,0,...,0)</em>.
	 *
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	@Override
	public boolean map()
	{
		interrupted.set( false );

		final StopWatch stopWatch = StopWatch.createAndStart();

		final int targetHeight = ( int ) target.dimension( 1 );
		final int numTasks = numThreads <= 1 ? 1 : Math.min( numThreads * 10, targetHeight );
		final double taskHeight = ( double ) targetHeight / numTasks;
		final int[] taskStartHeights = new int[ numTasks + 1 ];
		for ( int i = 0; i < numTasks; ++i )
			taskStartHeights[ i ] = ( int ) ( i * taskHeight );
		taskStartHeights[ numTasks ] = targetHeight;

		final boolean createExecutor = ( executorService == null );
		final ExecutorService ex = createExecutor ? Executors.newFixedThreadPool( numThreads ) : executorService;

		final List< Callable< Void > > tasks = new ArrayList<>( numTasks );
		for( int i = 0; i < numTasks; ++i )
			tasks.add( createMapTask( taskStartHeights[ i ], taskStartHeights[ i + 1 ] ) );
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

		return !interrupted.get();
	}

	/**
	 * @return a {@code Callable} that runs {@code map(startHeight, endHeight)}
	 */
	private Callable< Void > createMapTask( final int startHeight, final int endHeight )
	{
		return Executors.callable( () -> map( startHeight, endHeight ), null );
	}

	/**
	 * Copy lines from {@code y = startHeight} up to {@code endHeight}
	 * (exclusive) from source {to target. Check after
	 * each line whether rendering was {@link #cancel() canceled}.
	 *
	 * @param startHeight
	 *     start of line range to copy (relative to target min coordinate)
	 * @param endHeight
	 *     end (exclusive) of line range to copy (relative to target min
	 *     coordinate)
	 */
	private void map( final int startHeight, final int endHeight )
	{
		if ( interrupted.get() )
			return;

		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
		final RandomAccess< A > sourceRandomAccess = source.randomAccess( sourceInterval );
		final int width = ( int ) target.dimension( 0 );
		final long[] smin = Intervals.minAsLongArray( sourceInterval );

		for ( int y = startHeight; y < endHeight; ++y )
		{
			if ( interrupted.get() )
				return;
			smin[ 1 ] = y;
			sourceRandomAccess.setPosition( smin );
			targetRandomAccess.setPosition( smin );
			for ( int x = 0; x < width; ++x )
			{
				converter.convert( sourceRandomAccess.get(), targetRandomAccess.get() );
				sourceRandomAccess.fwd( 0 );
				targetRandomAccess.fwd( 0 );
			}
		}
	}
}
