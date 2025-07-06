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
package bdv.util;

import java.util.Random;

import bdv.viewer.render.AlphaWeightedAccumulateProjectorARGB;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.fluent.RandomAccessibleIntervalView;

public class ExampleMasked3D
{
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

//		final BdvOptions options = Bdv.options();
		final BdvOptions options = Bdv.options().accumulateProjectorFactory( AlphaWeightedAccumulateProjectorARGB.factory );

		final Img< UnsignedIntType > img = ArrayImgs.unsignedInts( 100, 100, 100 );
		final Random random = new Random();
		img.forEach( t -> t.set( random.nextInt( 255 )) );

		final double radius = 50;
		final double b = 2.0;
		final double a = -b / radius;
		final RandomAccessibleInterval< DoubleType > mask = new FunctionRandomAccessible<>(
				3,
				( pos, type ) -> {
					final double x = pos.getDoublePosition( 0 ) - 50;
					final double y = pos.getDoublePosition( 1 ) - 50;
					final double z = pos.getDoublePosition( 2 ) - 50;
					final double dist = Math.sqrt( x * x + y * y + z * z );
					type.set( Math.min( 1, Math.max( 0, a * dist + b ) ) );
				}, DoubleType::new )
				.view().interval( img );

		final RandomAccessibleIntervalView< UnsignedByteType > img2 = new ConstantRandomAccessible<>( new UnsignedByteType( 128 ), 3 )
				.view().interval( img );
		final BdvSource source = BdvFunctions.show( img2, "background", options );
		source.setDisplayRange( 0, 255 );
		source.setColor( new ARGBType( 0xFF4488FF) );

		final Bdv bdv3D = source;
		final BdvSource source2 = BdvFunctions.showMasked( img, mask, "masked", Bdv.options().addTo( bdv3D ) );
		source2.setDisplayRange( 0, 255 );
		source2.setColor( new ARGBType( 0xFF00FF00 ) );


	}
}
