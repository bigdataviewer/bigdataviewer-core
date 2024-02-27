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

import java.util.function.Supplier;

import bdv.viewer.render.ProjectorUtils.ArrayData;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.converter.Converter;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.PrimitiveType;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

// TODO Fix naming. This is a VolatileProjector for a non-volatile source...
/**
 * An {@link VolatileProjector}, that renders a target 2D
 * {@code RandomAccessibleInterval} by copying values from a source
 * {@code RandomAccessible}. The source can have more dimensions than the
 * target. Target coordinate <em>(x,y)</em> is copied from source coordinate
 * <em>(x,y,0,...,0)</em>.
 *
 * @param <A>
 *            pixel type of the source {@code RandomAccessible}.
 * @param <B>
 *            pixel type of the target {@code RandomAccessibleInterval}.
 *
 * @author Tobias Pietzsch
 * @author Stephan Saalfeld
 */
public class SimpleVolatileProjector< A, B > implements VolatileProjector
{
	/**
	 * A converter from the source pixel type to the target pixel type.
	 */
	private final Converter< ? super A, B > converter;

	/**
	 * The target interval. Pixels of the target interval should be set by
	 * {@link #map}
	 */
	private final RandomAccessibleInterval< B > target;

	private final RandomAccessible< A > source;

	private final BlockSupplier< ? > blk;

	/**
	 * Time needed for rendering the last frame, in nano-seconds.
	 */
	private long lastFrameRenderNanoTime;

	private volatile boolean canceled = false;

	private boolean valid = false;

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
	 */
	public SimpleVolatileProjector(
			final RandomAccessible< A > source,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target )
	{
		this.converter = converter;
		this.target = target;
		this.source = source;
		lastFrameRenderNanoTime = -1;

		blk = getBlockSupplierIfPossible( ( RandomAccessible ) source, ( RandomAccessibleInterval ) target, ( Converter ) converter );
	}


	static < A extends NativeType< A >, B extends NativeType< B > > BlockSupplier< B > getBlockSupplierIfPossible(
			final RandomAccessible< A > source,
			final RandomAccessibleInterval< B > target,
			final Converter< ? super A, B > converter )
	{
		A type = source.getType();
		B targetType = target.getType();

		if ( !( source instanceof AffineRandomAccessible ) )
			return null;
		final AffineRandomAccessible< A, ? > s0 = ( AffineRandomAccessible< A, ? > ) source;
		final AffineGet transformFromSource = s0.getTransformToSource().inverse();

		final RealRandomAccessible< A > s1 = s0.getSource();
		if ( !( s1 instanceof Interpolant ) )
			return null;
		final Interpolant< A, ? > s2 = ( Interpolant< A, ? > ) s1;

		final InterpolatorFactory< A, ? > f = s2.getInterpolatorFactory();
		final Transform.Interpolation interpolation;
		if ( f instanceof ClampingNLinearInterpolatorFactory )
		{
			interpolation = Transform.Interpolation.NLINEAR;
		}
		else if ( f instanceof NearestNeighborInterpolatorFactory )
		{
			interpolation = Transform.Interpolation.NEARESTNEIGHBOR;
		}
		else
		{
			System.out.println( "cannot handle " + f.getClass() + " (yet)" );
			return null;
		}

		final RandomAccessible< A > s3 = ( RandomAccessible< A > ) s2.getSource();
		final Supplier< Converter< ? super A, B > > converterSupplier = () -> converter;
		return BlockSupplier.of( s3 )
				.andThen( Transform.affine( transformFromSource, interpolation ) )
				.andThen( Convert.convert( targetType, converterSupplier ) );
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

	@Override
	public boolean isValid()
	{
		return valid;
	}

	private boolean blked()
	{
		final StopWatch stopWatch = StopWatch.createAndStart();

		// Source interval which will be used for rendering. This is the 2D
		// target interval expanded to source dimensionality (usually 3D) with
		// min=max=0 in the additional dimensions.
		final int n = Math.max( 2, source.numDimensions() );
		final long[] smin = new long[ n ];
		final long[] smax = new long[ n ];
		smin[ 0 ] = target.min( 0 );
		smax[ 0 ] = target.max( 0 );
		smin[ 1 ] = target.min( 1 );
		smax[ 1 ] = target.max( 1 );
		final FinalInterval sourceInterval = FinalInterval.wrap( smin, smax );

		// TODO: use correct primitive array type here ...
		final int[] dest = new int[ ( int ) Intervals.numElements( sourceInterval ) ];
		blk.copy( sourceInterval, dest );
		final ArrayData targetData = ProjectorUtils.getARGBArrayData( target );
//		System.out.println( "targetData = " + targetData );
		final int[] o_src = { 0, 0 };
		final int[] size_src = { ( int ) sourceInterval.dimension( 0 ), ( int ) sourceInterval.dimension( 1 ) };
		final int[] o_dst = { targetData.ox(), targetData.oy() };
		final int[] size_dst = { targetData.stride(), 1 };
		SubArrayCopy
				.forPrimitiveType( PrimitiveType.INT )
				.copy( dest, size_src, o_src, targetData.data(), size_dst, o_dst, size_src );

		lastFrameRenderNanoTime = stopWatch.nanoTime();

		final boolean success = !canceled;
		valid |= success;

		if (!success)
			System.out.println( "success = " + success );
		return success;
	}

	public static boolean DEBUG_USE_BLK_AFFINE = true;

	/**
	 * Render the 2D target image by copying values from the source. Source can
	 * have more dimensions than the target. Target coordinate <em>(x,y)</em> is
	 * copied from source coordinate <em>(x,y,0,...,0)</em>.
	 * <p>
	 * Check after each line whether rendering was {@link #cancel() canceled}.
	 *
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	@Override
	public boolean map( final boolean clearUntouchedTargetPixels )
	{
		if ( canceled )
			return false;

		if ( blk != null && DEBUG_USE_BLK_AFFINE )
			return blked();

		final StopWatch stopWatch = StopWatch.createAndStart();

		// Source interval which will be used for rendering. This is the 2D
		// target interval expanded to source dimensionality (usually 3D) with
		// min=max=0 in the additional dimensions.
		final int n = Math.max( 2, source.numDimensions() );
		final long[] smin = new long[ n ];
		final long[] smax = new long[ n ];
		smin[ 0 ] = target.min( 0 );
		smax[ 0 ] = target.max( 0 );
		smin[ 1 ] = target.min( 1 );
		smax[ 1 ] = target.max( 1 );
		final FinalInterval sourceInterval = new FinalInterval( smin, smax );

		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
		final RandomAccess< A > sourceRandomAccess = source.randomAccess( sourceInterval );
		final int width = ( int ) target.dimension( 0 );
		final int height = ( int ) target.dimension( 1 );
		for ( int y = 0; y < height; ++y )
		{
			// TODO (FORKJOIN) With tiles being granular enough, probably
			//   projectors shouldn't check after each line for cancellation
			//   (maybe not at all).
			if ( canceled )
				return false;

			sourceRandomAccess.setPosition( smin );
			targetRandomAccess.setPosition( smin );
			for ( int x = 0; x < width; ++x )
			{
				converter.convert( sourceRandomAccess.get(), targetRandomAccess.get() );
				sourceRandomAccess.fwd( 0 );
				targetRandomAccess.fwd( 0 );
			}
			++smin[ 1 ];
		}

		lastFrameRenderNanoTime = stopWatch.nanoTime();

		final boolean success = !canceled;
		valid |= success;
		return success;
	}
}
