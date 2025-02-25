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

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class OverlayExample3D
{

	public static void main( final String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final Random random = new Random();

		final ArrayImg< ARGBType, IntArray > img = ArrayImgs.argbs( 100, 100, 100 );
		img.forEach( t -> t.set( random.nextInt() & 0xFF003F00 ) );
		final Bdv bdv3D = BdvFunctions.show( img, "greens" );

		final ArrayList< RealPoint > points = new ArrayList<>();
		for ( int i = 0; i < 500; ++i )
			points.add( new RealPoint( random.nextInt( 100 ), random.nextInt( 100 ), random.nextInt( 100 ) ) );

		final BdvOverlay overlay = new BdvOverlay()
		{
			@Override
			protected void draw( final Graphics2D g )
			{
				final AffineTransform3D t = new AffineTransform3D();
				getCurrentTransform3D( t );

				final double[] lPos = new double[ 3 ];
				final double[] gPos = new double[ 3 ];
				for ( final RealPoint p : points )
				{
					p.localize( lPos );
					t.apply( lPos, gPos );
					final int size = getSize( gPos[ 2 ] );
					final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
					final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
					g.setColor( getColor( gPos[ 2 ] ) );
					g.fillOval( x, y, size, size );
				}
			}

			private Color getColor( final double depth )
			{
				int alpha = 255 - ( int ) Math.round( Math.abs( depth ) );

				if ( alpha < 64 )
					alpha = 64;

				final int r = ARGBType.red( info.getColor().get() );
				final int g = ARGBType.green( info.getColor().get() );
				final int b = ARGBType.blue( info.getColor().get() );
				return new Color( r , g, b, alpha );
			}

			private int getSize( final double depth )
			{
				return ( int ) Math.max( 1, 10 - 0.1 * Math.round( Math.abs( depth ) ) );
			}
		};

		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv3D ) );
		// TODO: add BdvOptions.closeAfterRemovingLastSource()
	}
}
