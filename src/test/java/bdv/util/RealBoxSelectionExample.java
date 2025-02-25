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

import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.Regions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;

public class RealBoxSelectionExample
{
	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Random random = new Random();

		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 100, 100, 50 );
		img.forEach( t -> t.set( random.nextInt( 128 ) ) );

		final AffineTransform3D imageTransform = new AffineTransform3D();
		imageTransform.set( 2, 2, 2 );
		final Bdv bdv = BdvFunctions.show( img, "image", BdvOptions.options().sourceTransform( imageTransform ) );

		final RealInterval initialInterval = Intervals.createMinMaxReal( 30, 30, 30, 80, 80, 80 );
		final RealInterval rangeInterval = Intervals.createMinMaxReal( 0, 0, 0, 100, 100, 100 );
		final AffineTransform3D boxTransform = new AffineTransform3D();
		boxTransform.rotate( 2,  0.5 );
		boxTransform.translate( 30, -20, 0 );

		final TransformedRealBoxSelectionDialog.Result result = BdvFunctions.selectRealBox(
				bdv,
				boxTransform,
				initialInterval,
				rangeInterval,
				BoxSelectionOptions.options()
						.title( "Select box to fill" ) );

		if ( result.isValid() )
		{
			final RealMaskRealInterval imageMask = result.asMask().transform( imageTransform );
			final RealRandomAccessibleRealInterval< BoolType > rrai = Masks.toRealRandomAccessibleRealInterval( imageMask );
			final IntervalView< BoolType > rai = Views.interval( Views.raster( rrai ), Intervals.smallestContainingInterval( rrai ) );
			Regions.sample( Regions.iterable( rai ), Views.extendZero( img ) ).forEach( t -> t.set( 255 ) );
			bdv.getBdvHandle().getViewerPanel().requestRepaint();
		}
	}
}
