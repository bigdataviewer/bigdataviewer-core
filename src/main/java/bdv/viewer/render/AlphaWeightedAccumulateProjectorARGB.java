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

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.ProjectorUtils.ArrayData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

public class AlphaWeightedAccumulateProjectorARGB
{
	public static class Factory implements AccumulateProjectorFactory< ARGBType >
	{
		@Override
		public VolatileProjector createProjector(
				final List< VolatileProjector > sourceProjectors,
				final List< SourceAndConverter< ? > > sources,
				final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImage,
				final int numThreads,
				final ExecutorService executorService )
		{
			final ProjectorData projectorData = getProjectorData( sourceScreenImages, targetScreenImage );
			if ( projectorData == null )
				return new AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage );
			else
				return new AccumulateProjectorARGBArrayData( sourceProjectors, projectorData );
		}

		@Override
		public boolean requiresMaskedSources() {
			return true;
		}
	}

	public static AccumulateProjectorFactory< ARGBType > factory = new Factory();

	private static ProjectorData getProjectorData(
			final List< ? extends RandomAccessible< ? extends ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target )
	{
		final ArrayData targetData = ProjectorUtils.getARGBArrayData( target );
		if ( targetData == null )
			return null;

		final int numSources = sources.size();
		final List< int[] > sourceData = new ArrayList<>( numSources );
		for ( int i = 0; i < numSources; ++i )
		{
			final RandomAccessible< ? extends ARGBType > source = sources.get( i );
			if ( ! ( source instanceof RandomAccessibleInterval ) )
				return null;
			if ( ! Intervals.equals( target, ( Interval ) source ) )
				return null;
			final int[] data = ProjectorUtils.getARGBArrayImgData( source );
			if ( data == null )
				return null;
			sourceData.add( data );
		}

		return new ProjectorData( targetData, sourceData );
	}

	private static class ProjectorData
	{
		private final ArrayData targetData;
		private final List< int[] > sourceData;

		ProjectorData( final ArrayData targetData, final List< int[] > sourceData )
		{
			this.targetData = targetData;
			this.sourceData = sourceData;
		}

		ArrayData targetData()
		{
			return targetData;
		}

		List< int[] > sourceData()
		{
			return sourceData;
		}
	}

	private static class AccumulateProjectorARGBArrayData implements VolatileProjector
	{
		/**
		 * Projectors that render the source images to accumulate.
		 * For every rendering pass, ({@link VolatileProjector#map(boolean)}) is run on each source projector that is not yet {@link VolatileProjector#isValid() valid}.
		 */
		private List< VolatileProjector > sourceProjectors;

		/**
		 * The source images to accumulate
		 */
		private final List< int[] > sources;

		/**
		 * The target interval.
		 */
		private final ArrayData target;

		/**
		 * Time needed for rendering the last frame, in nano-seconds.
		 */
		private long lastFrameRenderNanoTime;

		private volatile boolean canceled = false;

		private volatile boolean valid = false;

		public AccumulateProjectorARGBArrayData(
				final List< VolatileProjector > sourceProjectors,
				final ProjectorData projectorData )
		{
			this.sourceProjectors = sourceProjectors;
			this.target = projectorData.targetData();
			this.sources = projectorData.sourceData();
		}

		@Override
		public boolean map( final boolean clearUntouchedTargetPixels )
		{
			if ( canceled )
				return false;

			if ( isValid() )
				return true;

			final StopWatch stopWatch = StopWatch.createAndStart();

			if ( target.size() < Tiling.MIN_ACCUMULATE_FORK_SIZE )
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
			final int[] acc = new int[ target.width() << 2 ];
			for ( int y = 0; y < target.height(); ++y )
			{
				final int oTarget = ( y + target.oy() ) * target.stride() + target.ox();
				final int oSource = y * target.width();
				Arrays.fill( acc, 0 );
				for ( int s = 0; s < numSources; ++s )
				{
					final int[] source = sources.get( s );
					for ( int x = 0; x < target.width(); ++x )
					{
						final int value = source[ oSource + x ];
						final int alpha = ARGBType.alpha( value );
						final int red = ARGBType.red( value );
						final int green = ARGBType.green( value );
						final int blue = ARGBType.blue( value );
						acc[ ( x << 2 ) ] += alpha;
						acc[ ( x << 2 ) + 1 ] += red * alpha;
						acc[ ( x << 2 ) + 2 ] += green * alpha;
						acc[ ( x << 2 ) + 3 ] += blue * alpha;
					}
				}
				for ( int x = 0; x < target.width(); ++x )
				{
					final int aSum = acc[ ( x << 2 ) ];
					final int rSum;
					final int gSum;
					final int bSum;
					if ( aSum == 0 ) {
						rSum = 0;
						gSum = 0;
						bSum = 0;
					} else {
						rSum = Math.min( 255, acc[ ( x << 2 ) + 1 ] / aSum );
						gSum = Math.min( 255, acc[ ( x << 2 ) + 2 ] / aSum );
						bSum = Math.min( 255, acc[ ( x << 2 ) + 3 ] / aSum );
					}
					target.data()[ oTarget + x ] = ARGBType.rgba( rSum, gSum, bSum, 255 );
				}
			}
		}

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

	private static class AccumulateProjectorARGBGeneric extends AccumulateProjector< ARGBType, ARGBType >
	{
		public AccumulateProjectorARGBGeneric(
				final List< VolatileProjector > sourceProjectors,
				final List< ? extends RandomAccessible< ? extends ARGBType > > sources,
				final RandomAccessibleInterval< ARGBType > target )
		{
			super( sourceProjectors, sources, target );
		}

		@Override
		protected void accumulate( final Cursor< ? extends ARGBType >[] accesses, final ARGBType target )
		{
			int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
			for ( final Cursor< ? extends ARGBType > access : accesses )
			{
				final int value = access.get().get();
				final int alpha = ARGBType.alpha( value );
				final int red = ARGBType.red( value );
				final int green = ARGBType.green( value );
				final int blue = ARGBType.blue( value );
				aSum += alpha;
				rSum += red * alpha;
				gSum += green * alpha;
				bSum += blue * alpha;
			}
			if ( aSum == 0 )
			{
				rSum = 0;
				gSum = 0;
				bSum = 0;
			}
			else
			{
				rSum = Math.min( 255, rSum / aSum );
				gSum = Math.min( 255, gSum / aSum );
				bSum = Math.min( 255, bSum / aSum );
			}
			target.set( ARGBType.rgba( rSum, gSum, bSum, 255 ) );
		}
	}
}
