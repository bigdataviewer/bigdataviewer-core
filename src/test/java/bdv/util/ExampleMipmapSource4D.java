/*-
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

import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.function.BiConsumer;
import java.util.stream.DoubleStream;

import mpicbg.spim.data.sequence.FinalVoxelDimensions;

public class ExampleMipmapSource4D
{
	public static void main( String[] args )
	{
		final BiConsumer< Localizable, DoubleType > f = ( p, v ) -> {

			final double xc = p.getDoublePosition( 0 ) - 100;
			final double yc = p.getDoublePosition( 1 ) - 100;
			final double zc = p.getDoublePosition( 2 ) - 100;
			final double t = p.getDoublePosition( 3 );
			final double s = 20 + ( 20 * t );
			v.set( Math.sqrt( xc * xc / 2 + yc * yc / 16 + zc * zc / 64 ) / s );
		};

		final IntervalView< DoubleType > img = Views.interval(
				new FunctionRandomAccessible<>( 4, f, DoubleType::new ),
				new FinalInterval( 200, 200, 200, 20 ) );


		// set to true to make the scale transforms not match the downsampling factors
		// its can be useful to confirm that bdv really sees the multiple scales
		final boolean scalesTooBig = false;

		final int N = 3;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< DoubleType >[] imgs = new RandomAccessibleInterval[ N ];
		final AffineTransform3D[] transforms = new AffineTransform3D[ N ];
		for ( int i = 0; i < N; i++ )
		{
			transforms[ i ] = new AffineTransform3D();
			if ( scalesTooBig )
				transforms[ i ].scale( Math.pow( 4, i ) );
			else
				transforms[ i ].scale( Math.pow( 2, i ) );

			if ( i == 0 )
				imgs[ i ] = img;
			else
				imgs[ i ] = Views.subsample( img, 2 * i, 2 * i, 2 * i, 1 );
		}

		final RandomAccessibleIntervalMipmapSource4D< DoubleType > raiSource4D = new RandomAccessibleIntervalMipmapSource4D<>(
				imgs,
				Util.getTypeFromInterval( img ),
				transforms,
				new FinalVoxelDimensions( "um", 1, 1, 1 ),
				"4d rai source", true );

		final BdvStackSource< DoubleType > bdv = BdvFunctions.show( raiSource4D, 5 );
		bdv.setDisplayRange( 0, 3 );
	}
}
