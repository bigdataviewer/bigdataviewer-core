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
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.converter.Converter;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.operators.SetZero;

/**
 * {@link VolatileProjector} for a hierarchy of {@link Volatile} inputs.
 * After each {@link #map()} call, the projector has a {@link #isValid() state}
 * that signalizes whether all projected pixels were perfect.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
public class VolatileHierarchyProjector< A extends Volatile< ? >, B extends SetZero > extends AbstractVolatileHierarchyProjector< A, B >
{
	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target )
	{
		this( sources, converter, target, new byte[ ( int ) ( target.dimension( 0 ) * target.dimension( 1 ) ) ] );
	}

	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final byte[] maskArray )
	{
		super( sources, converter, target, maskArray );




		System.out.println( "VolatileHierarchyProjector.VolatileHierarchyProjector" );
		final List< BlockSupplier< ? > > blks = new ArrayList<>();
		for ( RandomAccessible< A > source : sources )
		{
			blks.add( getBlockSupplierIfPossible( ( RandomAccessible ) source ) );
		}
		System.out.println( "blks = " + blks );
		final BlockSupplier< ARGBType > blk = ProjectHierarchy.of( ( List ) blks, maskArray );
		System.out.println( "blk = " + blk );
	}

	static < A extends NativeType< A > > BlockSupplier< A > getBlockSupplierIfPossible(
			final RandomAccessible< A > source )
	{
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
		return BlockSupplier.of( s3 )
				.andThen( Transform.affine( transformFromSource, interpolation ) );
	}

	@Override
	int processLine(final RandomAccess< A > sourceRandomAccess, final RandomAccess< B > targetRandomAccess, final byte resolutionIndex, final int width, final int y )
	{
		int numInvalidPixels = 0;
		final int mi = y * width;
		for ( int x = 0; x < width; ++x )
		{
			if ( mask[ mi + x ] > resolutionIndex )
			{
				final A a = sourceRandomAccess.get();
				final boolean v = a.isValid();
				if ( v )
				{
					converter.convert( a, targetRandomAccess.get() );
					mask[ mi + x ] = resolutionIndex;
				}
				else
					++numInvalidPixels;
			}
			sourceRandomAccess.fwd( 0 );
			targetRandomAccess.fwd( 0 );
		}
		return numInvalidPixels;
	}
}
