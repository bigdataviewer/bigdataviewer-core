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
package bdv.util;

import java.util.Arrays;

import bdv.viewer.Interpolation;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class RandomAccessibleSource4D< T extends NumericType< T > > extends AbstractSource< T >
{
	private final RandomAccessible< T > source;

	private final Interval interval;

	private final Interval timeSliceInterval;

	protected int currentTimePointIndex;

	private RandomAccessibleInterval< T > currentSource;

	private final RealRandomAccessible< T >[] currentInterpolatedSources;

	private final AffineTransform3D sourceTransform;

	public RandomAccessibleSource4D(
			final RandomAccessible< T > img,
			final Interval interval,
			final T type,
			final String name )
	{
		this( img, interval, type, new AffineTransform3D(), name, false );
	}
	public RandomAccessibleSource4D(
			final RandomAccessible< T > img,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this( img, interval, type, sourceTransform, name, false );
	}

	public RandomAccessibleSource4D(
			final RandomAccessible< T > img,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxCulling )
	{
		super( type, name, doBoundingBoxCulling );
		this.source = img;
		this.interval = interval;
		this.timeSliceInterval = Intervals.createMinMax(
				interval.min( 0 ), interval.min( 1 ), interval.min( 2 ),
				interval.max( 0 ), interval.max( 1 ), interval.max( 2 ) );
		this.sourceTransform = sourceTransform;
		currentInterpolatedSources = new RealRandomAccessible[ Interpolation.values().length ];
		loadTimepoint( 0 );
	}

	private void loadTimepoint( final int timepointIndex )
	{
		currentTimePointIndex = timepointIndex;
		if ( isPresent( timepointIndex ) )
		{
			final T zero = getType().createVariable();
			zero.setZero();
			final RandomAccessible< T > slice = Views.hyperSlice( source, 3, timepointIndex );
			currentSource = Views.interval( slice, timeSliceInterval );
			for ( final Interpolation method : Interpolation.values() )
				currentInterpolatedSources[ method.ordinal() ] = Views.interpolate( slice, interpolators.get( method ) );
		}
		else
		{
			currentSource = null;
			Arrays.fill( currentInterpolatedSources, null );
		}
	}

	@Override
	public boolean isPresent( final int t )
	{
		return interval.min( 3 ) <= t && t <= interval.max( 3 );
	}

	@Override
	public synchronized RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentSource;
	}

	@Override
	public synchronized RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentInterpolatedSources[ method.ordinal() ];
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}
}
