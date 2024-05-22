/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.util;

import java.util.Arrays;
import java.util.function.Supplier;

import bdv.cache.SharedQueue;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.viewer.Interpolation;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class RandomAccessibleIntervalMipmapSource4D< T extends NumericType< T > > extends AbstractSource< T >
{
	protected final RandomAccessibleInterval< T >[] mipmapSources;

	protected final long minT, maxT;

	protected int currentTimePointIndex;

	private final RandomAccessibleInterval< T >[] currentMipmaps;

	private final RealRandomAccessible< T >[][] currentInterpolatedSources;

	protected final AffineTransform3D[] mipmapTransforms;

	private static boolean assertAllTDimensionsEqual(final Interval[] intervals) {

		final long min = intervals[0].min(3);
		final long max = intervals[0].min(3);

		for (int i = 1; i < intervals.length; ++i)
			if (!(intervals[i].min(3) == min && intervals[i].max(3) == max))
				return false;

		return true;
	}

	public RandomAccessibleIntervalMipmapSource4D(
			final RandomAccessibleInterval< T >[] imgs,
			final T type,
			final AffineTransform3D[] mipmapTransforms,
			final VoxelDimensions voxelDimensions,
			final String name,
			final boolean doBoundingBoxCulling )
	{
		super( type, name, voxelDimensions, doBoundingBoxCulling );
		assert imgs.length == mipmapTransforms.length : "Number of mipmaps and scale factors do not match.";
		assert assertAllTDimensionsEqual(imgs) : "Mipmaps have different numbers of timepoints.";

		this.mipmapSources = imgs;
		currentMipmaps = new RandomAccessibleInterval[ imgs.length ];
		this.mipmapTransforms = mipmapTransforms;

		minT = mipmapSources[0].min(3);
		maxT = mipmapSources[0].max(3);

		currentInterpolatedSources = new RealRandomAccessible[ Interpolation.values().length ][ imgs.length ];
		loadTimepoint( 0 );
	}

	public RandomAccessibleIntervalMipmapSource4D(
			final RandomAccessibleInterval< T >[] imgs,
			final T type,
			final double[][] mipmapScales,
			final VoxelDimensions voxelDimensions,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxCulling )
	{
		super( type, name, voxelDimensions, doBoundingBoxCulling );
		assert imgs.length == mipmapScales.length : "Number of mipmaps and scale factors do not match.";
		assert assertAllTDimensionsEqual(imgs) : "Mipmaps have different numbers of timepoints.";

		this.mipmapSources = imgs;
		currentMipmaps = new RandomAccessibleInterval[ imgs.length ];
		this.mipmapTransforms = new AffineTransform3D[ mipmapScales.length ];
		for ( int s = 0; s < mipmapScales.length; ++s )
		{
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			mipmapTransform.set(
					mipmapScales[ s ][ 0 ], 0, 0, 0.5 * ( mipmapScales[ s ][ 0 ] - 1 ),
					0, mipmapScales[ s ][ 1 ], 0, 0.5 * ( mipmapScales[ s ][ 1 ] - 1 ),
					0, 0, mipmapScales[ s ][ 2 ], 0.5 * ( mipmapScales[ s ][ 2 ] - 1 ) );
			mipmapTransform.preConcatenate(sourceTransform);
			mipmapTransforms[ s ] = mipmapTransform;
		}

		minT = mipmapSources[0].min(3);
		maxT = mipmapSources[0].max(3);

		currentInterpolatedSources = new RealRandomAccessible[ Interpolation.values().length ][ imgs.length ];
		loadTimepoint( 0 );
	}

	public RandomAccessibleIntervalMipmapSource4D(
			final RandomAccessibleInterval< T >[] imgs,
			final T type,
			final double[][] mipmapScales,
			final VoxelDimensions voxelDimensions,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this( imgs, type, mipmapScales, voxelDimensions, sourceTransform, name, true );
	}

	public RandomAccessibleIntervalMipmapSource4D(
			final RandomAccessibleInterval< T >[] imgs,
			final T type,
			final double[][] mipmapScales,
			final VoxelDimensions voxelDimensions,
			final String name )
	{
		this(imgs, type, mipmapScales, voxelDimensions, new AffineTransform3D(), name);
	}

	private void loadTimepoint( final int timepointIndex )
	{
		currentTimePointIndex = timepointIndex;
		if ( isPresent( timepointIndex ) )
		{
			final T zero = getType().createVariable();
			zero.setZero();
			for ( int level = 0; level < currentMipmaps.length; ++level )
			{
				currentMipmaps[level] = Views.hyperSlice(mipmapSources[level], 3, timepointIndex );
				for ( final Interpolation method : Interpolation.values() )
					currentInterpolatedSources[ method.ordinal() ][ level ] =
							Views.interpolate( Views.extendValue( currentMipmaps[level], zero ), interpolators.get( method ) );
			}
		}
		else
		{
			Arrays.fill( currentMipmaps, null );
			Arrays.fill( currentInterpolatedSources, null );
		}
	}

	@Override
	public boolean isPresent( final int t )
	{
		return minT <= t && t <= maxT;
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentMipmaps[ level ];
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentInterpolatedSources[ method.ordinal() ][ level ];
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( mipmapTransforms[ level ] );
	}

	@Override
	public int getNumMipmapLevels()
	{
		return mipmapSources.length;
	}

	public < V extends Volatile< T > & NumericType< V > > VolatileSource< T, V > asVolatile( final V vType, final SharedQueue queue )
	{
		return new VolatileSource<>( this, vType, queue );
	}

	public < V extends Volatile< T > & NumericType< V > > VolatileSource< T, V > asVolatile( final Supplier< V > vTypeSupplier, final SharedQueue queue )
	{
		return new VolatileSource<>( this, vTypeSupplier, queue );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public < V extends Volatile< T > & NumericType< V > > VolatileSource< T, V > asVolatile( final SharedQueue queue )
	{
		final T t = getType();
		if ( t instanceof NativeType )
			return new VolatileSource<>( this, ( V )VolatileTypeMatcher.getVolatileTypeForType( ( NativeType )getType() ), queue );
		else
			throw new UnsupportedOperationException( "This method only works for sources of NativeType." );
	}
}
