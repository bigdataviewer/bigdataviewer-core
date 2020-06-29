/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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

import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

public class AccumulateProjectorARGB implements VolatileProjector
{
	public static AccumulateProjectorFactory< ARGBType > factory = new AccumulateProjectorFactory< ARGBType >()
	{
		@Override
		public VolatileProjector createProjector(
				final ArrayList< VolatileProjector > sourceProjectors,
				final ArrayList< SourceAndConverter< ? > > sources,
				final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImage,
				final int numThreads,
				final ExecutorService executorService )
		{
			try
			{
				return new AccumulateProjectorARGB( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
			}
			catch ( IllegalArgumentException ignored )
			{}
			return new AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
		}
	};

	public static class AccumulateProjectorARGBGeneric extends AccumulateProjector< ARGBType, ARGBType >
	{
		public AccumulateProjectorARGBGeneric(
				final ArrayList< VolatileProjector > sourceProjectors,
				final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sources,
				final RandomAccessibleInterval< ARGBType > target,
				final int numThreads,
				final ExecutorService executorService )
		{
			super( sourceProjectors, sources, target, numThreads, executorService );
		}

		@Override
		protected void accumulate( final Cursor< ? extends ARGBType >[] accesses, final ARGBType target )
		{
			int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
			for ( final Cursor< ? extends ARGBType > access : accesses )
			{
				final int value = access.get().get();
				final int a = ARGBType.alpha( value );
				final int r = ARGBType.red( value );
				final int g = ARGBType.green( value );
				final int b = ARGBType.blue( value );
				aSum += a;
				rSum += r;
				gSum += g;
				bSum += b;
			}
			if ( aSum > 255 )
				aSum = 255;
			if ( rSum > 255 )
				rSum = 255;
			if ( gSum > 255 )
				gSum = 255;
			if ( bSum > 255 )
				bSum = 255;
			target.set( ARGBType.rgba( rSum, gSum, bSum, aSum ) );
		}
	}

	/**
	 * Projectors that render the source images to accumulate.
	 * For every rendering pass, ({@link VolatileProjector#map(boolean)}) is run on each source projector that is not yet {@link VolatileProjector#isValid() valid}.
	 */
	private final ArrayList< VolatileProjector > sourceProjectors;

	/**
	 * The source images to accumulate
	 */
	private final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sources;

	private final int[][] sourceData;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link #map}
	 */
	private final RandomAccessibleInterval< ARGBType > target;

	private final int[] targetData;

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

	public AccumulateProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		this.sourceProjectors = sourceProjectors;
		this.sources = sources;
		this.target = target;
		this.numThreads = numThreads;
		this.executorService = executorService;
		lastFrameRenderNanoTime = -1;

		targetData = ProjectorUtils.getARGBArrayImgData( target );
		if ( targetData == null )
			throw new IllegalArgumentException();

		final int numSources = sources.size();
		sourceData = new int[ numSources ][];
		for ( int i = 0; i < numSources; ++i )
		{
			final RandomAccessible< ? extends ARGBType > source = sources.get( i );
			if ( ! ( source instanceof RandomAccessibleInterval ) )
				throw new IllegalArgumentException();
			if ( ! Intervals.equals( target, ( Interval ) source ) )
				throw new IllegalArgumentException();
			sourceData[ i ] = ProjectorUtils.getARGBArrayImgData( source );
			if ( sourceData[ i ] == null )
				throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled.get() )
			return false;

		final StopWatch stopWatch = StopWatch.createAndStart();

		valid = true;
		for ( final VolatileProjector p : sourceProjectors )
			if ( !p.isValid() )
				if ( !p.map( clearUntouchedTargetPixels ) )
					return false;
				else
					valid &= p.isValid();

		final int size = ( int ) Intervals.numElements( target );

		final int numTasks = numThreads <= 1 ? 1 : Math.min( numThreads * 10, size );
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
		final int[] values = new int[ numSources ];
		for ( int i = startOffset; i < endOffset; ++i )
		{
			for ( int s = 0; s < numSources; ++s )
				values[ s ] = sourceData[ s ][ i ];
			targetData[ i ] = accumulate( values );
		}
	}

	protected int accumulate( final int[] values )
	{
		int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
		for ( final int value : values )
		{
			final int a = ARGBType.alpha( value );
			final int r = ARGBType.red( value );
			final int g = ARGBType.green( value );
			final int b = ARGBType.blue( value );
			aSum += a;
			rSum += r;
			gSum += g;
			bSum += b;
		}
		if ( aSum > 255 )
			aSum = 255;
		if ( rSum > 255 )
			rSum = 255;
		if ( gSum > 255 )
			gSum = 255;
		if ( bSum > 255 )
			bSum = 255;
		return ARGBType.rgba( rSum, gSum, bSum, aSum );
	}
}
