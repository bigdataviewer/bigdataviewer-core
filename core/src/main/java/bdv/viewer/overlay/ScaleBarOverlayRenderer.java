package bdv.viewer.overlay;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;

public class ScaleBarOverlayRenderer
{
	private final Font font = new Font( "SansSerif", Font.PLAIN, 12 );

	private final int targetScaleBarLength = 100;

	private final int subdivPerPowerOfTen = 4;

	double scaleBarLength = 100;

	double scale = 0;

	String unit = "px";

	public synchronized void paint( final Graphics2D g )
	{
		final DecimalFormat format = new DecimalFormat("0.####");
		final String scaleBarText = format.format( scale ) + " " + unit;

		// draw scalebar
		final int x = 20;
		final int y = ( int ) g.getClipBounds().getHeight() - 30;
		g.fillRect( x, y, ( int ) scaleBarLength, 10 );

		// draw label
		final FontRenderContext frc = g.getFontRenderContext();
		final TextLayout layout = new TextLayout( scaleBarText, font, frc );
		final Rectangle2D bounds = layout.getBounds();
		final float tx = ( float ) ( 20 + ( scaleBarLength - bounds.getMaxX() ) / 2 );
		final float ty = y - 5;
		layout.draw( g, tx, ty );
	}

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
				final AffineTransform3D transform = new AffineTransform3D();
				state.getViewerTransform( transform );

				final int t = state.getCurrentTimepoint();
				transform.concatenate( spimSource.getSourceTransform( t, 0 ) );
				final double sizeOfOnePixel = 1.0 / Affine3DHelpers.extractScale( transform, 0 );

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

				final String[] mUnits = { "nm", "µm", "mm", "m", "km" };
				final String scaleUnit = "mm";

				int shifts = ( int ) Math.floor( ( Math.log10( scale ) + 1 ) / 3 );
				int scaleUnitIndex = -1;
				for ( int i = 0; i < mUnits.length; ++i )
					if ( mUnits[ i ].equals( scaleUnit ) )
					{
						scaleUnitIndex = i;
						break;
					}
				int shiftedIndex = scaleUnitIndex + shifts;
				if ( shiftedIndex < 0 )
				{
					shifts = -scaleUnitIndex;
					shiftedIndex = 0;
				}
				else if ( shiftedIndex >= mUnits.length )
				{
					shifts = mUnits.length - 1 - scaleUnitIndex;
					shiftedIndex = mUnits.length - 1;
				}

				scale = scale / Math.pow( 1000, shifts );
				unit = mUnits[ shiftedIndex ];
			}
		}
	}
}
