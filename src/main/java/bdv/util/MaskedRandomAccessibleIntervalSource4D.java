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
import bdv.viewer.MaskUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.mask.Masked;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class MaskedRandomAccessibleIntervalSource4D< M extends Masked< T > & Type< M >, T extends NumericType< T > > extends AbstractSource< T >
{
	private final RandomAccessibleInterval< M > source;

	protected int currentTimePointIndex;

	private RandomAccessibleInterval< M > currentSource;

	private final RealRandomAccessible< M >[] currentInterpolatedSources;

	private final AffineTransform3D sourceTransform;

	public MaskedRandomAccessibleIntervalSource4D(
			final RandomAccessibleInterval< M > img,
			final String name )
	{
		this( img, new AffineTransform3D(), name );
	}

	public MaskedRandomAccessibleIntervalSource4D(
			final RandomAccessibleInterval< M > img,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		super( img.getType().value(), name );
		this.source = img;
		this.sourceTransform = sourceTransform;
		currentInterpolatedSources = new RealRandomAccessible[ Interpolation.values().length ];
		loadTimepoint( 0 );
	}

	private void loadTimepoint( final int timepointIndex )
	{
		currentTimePointIndex = timepointIndex;
		if ( isPresent( timepointIndex ) )
		{
			currentSource = Views.hyperSlice( source, 3, timepointIndex );
			for ( final Interpolation method : Interpolation.values() )
				currentInterpolatedSources[ method.ordinal() ] = MaskUtils.extendAndInterpolateMasked( currentSource, method );
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
		return source.min( 3 ) <= t && t <= source.max( 3 );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return MaskUtils.stripMask( getMaskedSource( t, level ) );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return MaskUtils.stripMask( getInterpolatedMaskedSource( t, level, method ) );
	}

	@Override
	public RandomAccessibleInterval< ? extends Masked< T > > getMaskedSource( final int t, final int level )
	{
		if ( t != currentTimePointIndex )
			loadTimepoint( t );
		return currentSource;
	}

	@Override
	public RealRandomAccessible< ? extends Masked< T > > getInterpolatedMaskedSource( final int t, final int level, final Interpolation method )
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
