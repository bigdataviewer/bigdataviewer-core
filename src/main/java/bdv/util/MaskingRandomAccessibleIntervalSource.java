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

import bdv.viewer.Interpolation;
import bdv.viewer.MaskUtils;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.mask.Masked;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class MaskingRandomAccessibleIntervalSource< T extends NumericType< T >, M extends RealType< M > > extends AbstractSource< T >
{
	private final RandomAccessibleInterval< T > source;

	private final RandomAccessibleInterval< M > mask;

	private final RealRandomAccessible< T >[] interpolatedSources;

	private final AffineTransform3D sourceTransform;

	public MaskingRandomAccessibleIntervalSource(
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< M > mask,
			final T type,
			final String name )
	{
		this( img, mask, type, new AffineTransform3D(), name, true );
	}

	public MaskingRandomAccessibleIntervalSource(
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< M > mask,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name )
	{
		this( img, mask, type, sourceTransform, name, true );
	}

	public MaskingRandomAccessibleIntervalSource(
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< M > mask,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxCulling )
	{
		super( type, name, new DefaultVoxelDimensions( img.numDimensions() ), doBoundingBoxCulling );
		this.source = img;
		this.mask = mask;
		this.sourceTransform = sourceTransform;
		interpolatedSources = new RealRandomAccessible[ Interpolation.values().length ];
		for ( final Interpolation method : Interpolation.values() )
			interpolatedSources[ method.ordinal() ] = Views.interpolate( Views.extendZero( source ), interpolators.get( method ) );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return source;
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return interpolatedSources[ method.ordinal() ];
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}

	@Override
	public RandomAccessibleInterval< ? extends Masked< T > > getMaskedSource( final int t, final int level )
	{
		return MaskUtils.mask( source, mask );
	}
}
