/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.viewer.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.List;

import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.util.Affine3DHelpers;
import bdv.util.Prefs;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;

public class ScaleBarOverlayRenderer
{
	private final Font font = new Font( "SansSerif", Font.PLAIN, 12 );

	private final DecimalFormat format = new DecimalFormat("0.####");

	private final Color color = new Color( Prefs.scaleBarColor(), true );

	private final Color bgcolor = new Color( Prefs.scaleBarBgColor(), true );

	private final AffineTransform3D transform = new AffineTransform3D();

	private final AffineTransform3D sourceTransform = new AffineTransform3D();

	/**
	 * Try to keep the scale bar as close to this length (in pixels) as possible.
	 */
	private final int targetScaleBarLength = 100;

	/**
	 * For finding the value to display on the scalebar: into how many parts is
	 * each power of ten divided? For example, 4 means the following are
	 * possible values:
	 * <em>..., 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10, ...</em>
	 */
	private final int subdivPerPowerOfTen = 4;

	private double scaleBarLength;

	private double scale;

	private String unit;

	private boolean drawScaleBar;

	public synchronized void paint( final Graphics2D g )
	{
		if ( drawScaleBar )
		{
			final String scaleBarText = format.format( scale ) + " " + unit;

			// scalebar position
			final int x = 20;
			final int y = ( int ) g.getClipBounds().getHeight() - 30;

			// label position
			final FontRenderContext frc = g.getFontRenderContext();
			final TextLayout layout = new TextLayout( scaleBarText, font, frc );
			final Rectangle2D bounds = layout.getBounds();
			final float tx = ( float ) ( 20 + ( scaleBarLength - bounds.getMaxX() ) / 2 );
			final float ty = y - 5;

			// draw background
			g.setColor( bgcolor );
			g.fillRect( x - 7, ( int ) ( ty - bounds.getHeight() - 3 ), ( int ) scaleBarLength + 14, ( int ) bounds.getHeight() + 25 );

			// draw scalebar
			g.setColor( color );
			g.fillRect( x, y, ( int ) scaleBarLength, 10 );

			// draw label
			layout.draw( g, tx, ty );
		}
	}

	private static final String[] lengthUnits = { "nm", "µm", "mm", "m", "km" };

	/**
	 * Update data to show in the overlay.
	 */
	public synchronized void setViewerState( final ViewerState state )
	{
		synchronized ( state )
		{
			final List< SourceState< ? > > sources = state.getSources();
			if ( ! sources.isEmpty() )
			{
				final Source< ? > spimSource = sources.get( state.getCurrentSource() ).getSpimSource();
				final VoxelDimensions voxelDimensions = spimSource.getVoxelDimensions();
				if ( voxelDimensions == null )
				{
					drawScaleBar = false;
					return;
				}
				drawScaleBar = true;

				state.getViewerTransform( transform );

				final int t = state.getCurrentTimepoint();
				spimSource.getSourceTransform( t, 0, sourceTransform );
				transform.concatenate( sourceTransform );
				final double sizeOfOnePixel = voxelDimensions.dimension( 0 ) / Affine3DHelpers.extractScale( transform, 0 );

				// find good scaleBarLength and corresponding scale value
				final double sT = targetScaleBarLength * sizeOfOnePixel;
				final double pot = Math.floor( Math.log10( sT ) );
				final double l2 =  sT / Math.pow( 10, pot );
				final int fracs = ( int ) ( 0.1 * l2 * subdivPerPowerOfTen );
				final double scale1 = ( fracs > 0 ) ? Math.pow( 10, pot + 1 ) * fracs / subdivPerPowerOfTen : Math.pow( 10, pot );
				final double scale2 = ( fracs == 3 ) ? Math.pow( 10, pot + 1 ) : Math.pow( 10, pot + 1 ) * ( fracs + 1 ) / subdivPerPowerOfTen;

				final double lB1 = scale1 / sizeOfOnePixel;
				final double lB2 = scale2 / sizeOfOnePixel;

				if ( Math.abs( lB1 - targetScaleBarLength ) < Math.abs( lB2 - targetScaleBarLength ) )
				{
					scale = scale1;
					scaleBarLength = lB1;
				}
				else
				{
					scale = scale2;
					scaleBarLength = lB2;
				}

				// If unit is a known unit (such as nm) then try to modify scale
				// and unit such that the displayed string is short.
				// For example, replace "0.021 µm" by "21 nm".
				String scaleUnit = voxelDimensions.unit();
				if ( "um".equals( scaleUnit ) )
					scaleUnit = "µm";
				int scaleUnitIndex = -1;
				for ( int i = 0; i < lengthUnits.length; ++i )
					if ( lengthUnits[ i ].equals( scaleUnit ) )
					{
						scaleUnitIndex = i;
						break;
					}
				if ( scaleUnitIndex >= 0 )
				{
					int shifts = ( int ) Math.floor( ( Math.log10( scale ) + 1 ) / 3 );
					int shiftedIndex = scaleUnitIndex + shifts;
					if ( shiftedIndex < 0 )
					{
						shifts = -scaleUnitIndex;
						shiftedIndex = 0;
					}
					else if ( shiftedIndex >= lengthUnits.length )
					{
						shifts = lengthUnits.length - 1 - scaleUnitIndex;
						shiftedIndex = lengthUnits.length - 1;
					}

					scale = scale / Math.pow( 1000, shifts );
					unit = lengthUnits[ shiftedIndex ];
				}
				else
				{
					unit = scaleUnit;
				}
			}
		}
	}
}
