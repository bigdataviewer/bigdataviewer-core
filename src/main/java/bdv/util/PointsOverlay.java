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

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;

public class PointsOverlay extends BdvOverlay
{
	private List< ? extends RealLocalizable > points;

	private Color col;

	public < T extends RealLocalizable > void setPoints( final List< T > points )
	{
		this.points = points;
	}

	@Override
	protected void draw( final Graphics2D graphics )
	{
		if ( points == null )
			return;

		col = new Color( info.getColor().get() );

		final AffineTransform3D transform = new AffineTransform3D();
		getCurrentTransform3D( transform );
		final double[] lPos = new double[ 3 ];
		final double[] gPos = new double[ 3 ];
		for ( final RealLocalizable p : points )
		{
			p.localize( lPos );
			transform.apply( lPos, gPos );
			final double size = getPointSize( gPos );
			final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
			final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
			final int w = ( int ) size;
			graphics.setColor( getColor( gPos ) );
			graphics.fillOval( x, y, w, w );
		}
	}

	/** screen pixels [x,y,z] **/
	private Color getColor( final double[] gPos )
	{
		int alpha = 255 - ( int ) Math.round( Math.abs( gPos[ 2 ] ) );

		if ( alpha < 64 )
			alpha = 64;

		return new Color( col.getRed(), col.getGreen(), col.getBlue(), alpha );
	}

	private double getPointSize( final double[] gPos )
	{
		if ( Math.abs( gPos[ 2 ] ) < 3 )
			return 5.0;
		else
			return 3.0;
	}
}
