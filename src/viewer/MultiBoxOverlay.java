package viewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.Multi3DViewer.SourceDisplay;

/**
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MultiBoxOverlay
{
	final Color activeBackColor = new Color( 0x00994499 );// Color.MAGENTA;

	final Color activeFrontColor = Color.GREEN;

	final Color inactiveBackColor = Color.DARK_GRAY;

	final Color inactiveFrontColor = Color.LIGHT_GRAY;

	final Color canvasColor = new Color( 0xb0bbbbbb, true );

	/**
	 * distance from the eye to the projection plane z=0.
	 */
	private double depth = 10.0;

	/**
	 * scale the 2D projection of the overlay box by this factor.
	 */
	private double scale = 0.1;

	final private double[] origin = new double[ 3 ];

	/**
	 * This paints the box overlay with perspective and scale set such that it
	 * fits approximately into the specified screen area.
	 *
	 * @param graphics
	 *            graphics context to paint to.
	 * @param sources
	 *            source intervals (3D boxes) to be is shown.
	 * @param currentSource
	 *            index of the currently active source or -1, in which case the
	 *            {@link SourceDisplay#isVisible()} flags are used.
	 * @param targetInterval
	 *            target interval (2D box) into which a slice of sourceInterval
	 *            is projected.
	 * @param boxScreen
	 *            (approximate) area of the screen which to fill with the box
	 *            visualisation.
	 */
	public void paint( final Graphics2D graphics, final ArrayList< SourceDisplay< ? > > sources, final int currentSource, final Interval targetInterval, final Interval boxScreen )
	{
//		assert ( sourceInterval.numDimensions() >= 3 );
		assert ( targetInterval.numDimensions() >= 2 );

		if ( sources.isEmpty() )
			return;

		final double perspective = 3;
		final double screenBoxRatio = 0.75;

		long maxSourceSize = 0;
		for ( final SourceDisplay< ? > source : sources )
			maxSourceSize = Math.max( maxSourceSize, Math.max( Math.max( source.interval.dimension( 0 ), source.interval.dimension( 1 ) ), source.interval.dimension( 2 ) ) );
		final long sourceSize = maxSourceSize;
		final long targetSize = Math.max( targetInterval.dimension( 0 ), targetInterval.dimension( 1 ) );

		final AffineTransform3D transform = sources.get( 0 ).sourceToScreen;
		final double vx = transform.get( 0, 0 );
		final double vy = transform.get( 1, 0 );
		final double vz = transform.get( 2, 0 );
		final double transformScale = Math.sqrt( vx*vx + vy*vy + vz*vz );
		setDepth( perspective * sourceSize * transformScale );

		final double bw = screenBoxRatio * boxScreen.dimension( 0 );
		final double bh = screenBoxRatio * boxScreen.dimension( 1 );
		scale = Math.min( bw / targetInterval.dimension( 0 ), bh / targetInterval.dimension( 1 ) );

		final double tsScale = transformScale * sourceSize / targetSize;
		if ( tsScale > 1.0 )
			scale /= tsScale;

		final long x = boxScreen.min( 0 ) + boxScreen.dimension( 0 ) / 2;
		final long y = boxScreen.min( 1 ) + boxScreen.dimension( 1 ) / 2;

		final AffineTransform t = graphics.getTransform();
		final AffineTransform translate = new AffineTransform( 1, 0, 0, 1, x, y );
		translate.preConcatenate( t );
		graphics.setTransform( translate );
		paint( graphics, sources, currentSource, targetInterval );
		graphics.setTransform( t );
	}

	public void setScale( final double scale )
	{
		this.scale = scale;
	}

	public void setDepth( final double depth )
	{
		this.depth = depth;
		origin[ 2 ] = -depth;
	}

	/**
	 *
	 * @param p point to project
	 * @return X coordinate of projected point
	 */
	private double perspectiveX( final double[] p )
	{
		return scale * ( p[ 0 ] - origin[ 0 ] ) / ( p[ 2 ] - origin[ 2 ] ) * depth;
	}

	/**
	 *
	 * @param p point to project
	 * @return Y coordinate of projected point
	 */
	private double perspectiveY( final double[] p )
	{
		return scale * ( p[ 1 ] - origin[ 1 ] ) / ( p[ 2 ] - origin[ 2 ] ) * depth;
	}

	private void splitEdge( final double[] a, final double[] b, final GeneralPath before, final GeneralPath behind )
	{
		final double[] t = new double[ 3 ];
		if ( a[ 2 ] <= 0 )
		{
			before.moveTo( perspectiveX( a ), perspectiveY( a ) );
			if ( b[ 2 ] <= 0 )
				before.lineTo( perspectiveX( b ), perspectiveY( b ) );
			else
			{
				final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
				t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
				t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
				before.lineTo( perspectiveX( t ), perspectiveY( t ) );
				behind.moveTo( perspectiveX( t ), perspectiveY( t ) );
				behind.lineTo( perspectiveX( b ), perspectiveY( b ) );
			}
		}
		else
		{
			behind.moveTo( perspectiveX( a ), perspectiveY( a ) );
			if ( b[ 2 ] > 0 )
				behind.lineTo( perspectiveX( b ), perspectiveY( b ) );
			else
			{
				final double d = a[ 2 ] / ( a[ 2 ] - b[ 2 ] );
				t[ 0 ] = ( b[ 0 ] - a[ 0 ] ) * d + a[ 0 ];
				t[ 1 ] = ( b[ 1 ] - a[ 1 ] ) * d + a[ 1 ];
				behind.lineTo( perspectiveX( t ), perspectiveY( t ) );
				before.moveTo( perspectiveX( t ), perspectiveY( t ) );
				before.lineTo( perspectiveX( b ), perspectiveY( b ) );
			}
		}
	}

	private void renderCanvas( final Interval targetInterval,  final GeneralPath canvas )
	{
		final double tX0 = targetInterval.min( 0 );
		final double tX1 = targetInterval.max( 0 );
		final double tY0 = targetInterval.min( 1 );
		final double tY1 = targetInterval.max( 1 );

		final double[] c000 = new double[] { tX0, tY0, 0 };
		final double[] c100 = new double[] { tX1, tY0, 0 };
		final double[] c010 = new double[] { tX0, tY1, 0 };
		final double[] c110 = new double[] { tX1, tY1, 0 };

		canvas.moveTo( perspectiveX( c000 ), perspectiveY( c000 ) );
		canvas.lineTo( perspectiveX( c100 ), perspectiveY( c100 ) );
		canvas.lineTo( perspectiveX( c110 ), perspectiveY( c110 ) );
		canvas.lineTo( perspectiveX( c010 ), perspectiveY( c010 ) );
		canvas.closePath();
	}

	private void renderBox( final Interval sourceInterval, final AffineTransform3D transform, final GeneralPath front, final GeneralPath back )
	{
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );

		final double[] p000 = new double[] { sX0, sY0, sZ0 };
		final double[] p100 = new double[] { sX1, sY0, sZ0 };
		final double[] p010 = new double[] { sX0, sY1, sZ0 };
		final double[] p110 = new double[] { sX1, sY1, sZ0 };
		final double[] p001 = new double[] { sX0, sY0, sZ1 };
		final double[] p101 = new double[] { sX1, sY0, sZ1 };
		final double[] p011 = new double[] { sX0, sY1, sZ1 };
		final double[] p111 = new double[] { sX1, sY1, sZ1 };

		final double[] q000 = new double[ 3 ];
		final double[] q100 = new double[ 3 ];
		final double[] q010 = new double[ 3 ];
		final double[] q110 = new double[ 3 ];
		final double[] q001 = new double[ 3 ];
		final double[] q101 = new double[ 3 ];
		final double[] q011 = new double[ 3 ];
		final double[] q111 = new double[ 3 ];

		transform.apply( p000, q000 );
		transform.apply( p100, q100 );
		transform.apply( p010, q010 );
		transform.apply( p110, q110 );
		transform.apply( p001, q001 );
		transform.apply( p101, q101 );
		transform.apply( p011, q011 );
		transform.apply( p111, q111 );

		splitEdge( q000, q100, front, back );
		splitEdge( q100, q110, front, back );
		splitEdge( q110, q010, front, back );
		splitEdge( q010, q000, front, back );

		splitEdge( q001, q101, front, back );
		splitEdge( q101, q111, front, back );
		splitEdge( q111, q011, front, back );
		splitEdge( q011, q001, front, back );

		splitEdge( q000, q001, front, back );
		splitEdge( q100, q101, front, back );
		splitEdge( q110, q111, front, back );
		splitEdge( q010, q011, front, back );
	}


	/**
	 *
	 * @param graphics
	 *            graphics context to paint to.
	 * @param sourceInterval
	 *            source interval (3D box) that is shown.
	 * @param targetInterval
	 *            target interval (2D box) into which a slice of sourceInterval
	 *            is projected.
	 * @param transform
	 *            transform from source to target.
	 */
	private void paint( final Graphics2D graphics, final ArrayList< SourceDisplay< ? > > sources, final int currentSource, final Interval targetInterval )
	{
		origin[ 0 ] = targetInterval.min( 0 ) + targetInterval.dimension( 0 ) / 2;
		origin[ 1 ] = targetInterval.min( 1 ) + targetInterval.dimension( 1 ) / 2;

		final GeneralPath canvas = new GeneralPath();
		renderCanvas( targetInterval, canvas );

		final GeneralPath activeFront = new GeneralPath();
		final GeneralPath activeBack = new GeneralPath();
		final GeneralPath inactiveFront = new GeneralPath();
		final GeneralPath inactiveBack = new GeneralPath();

		for ( int i = 0; i < sources.size(); ++i )
		{
			final SourceDisplay< ? > source = sources.get( i );
			if( currentSource == i || ( currentSource == -1 && source.isVisible() ) )
				renderBox( source.interval, source.sourceToScreen, activeFront, activeBack );
			else
				renderBox( source.interval, source.sourceToScreen, inactiveFront, inactiveBack );
		}

		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		graphics.setPaint( inactiveBackColor );
		graphics.draw( inactiveBack );
		graphics.setPaint( activeBackColor );
		graphics.draw( activeBack );
		graphics.setPaint( canvasColor );
		graphics.fill( canvas );
		graphics.setPaint( inactiveFrontColor );
		graphics.draw( inactiveFront );
		graphics.setPaint( activeFrontColor );
		graphics.draw( activeFront );

		final SourceDisplay< ? > source = sources.get( 0 );
		final double sX0 = source.interval.min( 0 );
		final double sY0 = source.interval.min( 1 );
		final double sZ0 = source.interval.min( 2 );

		final double[] px = new double[] { sX0 + source.interval.dimension( 0 ) / 2, sY0, sZ0 };
		final double[] py = new double[] { sX0, sY0 + source.interval.dimension( 1 ) / 2, sZ0 };
		final double[] pz = new double[] { sX0, sY0, sZ0 + source.interval.dimension( 2 ) / 2 };

		final double[] qx = new double[ 3 ];
		final double[] qy = new double[ 3 ];
		final double[] qz = new double[ 3 ];

		source.sourceToScreen.apply( px, qx );
		source.sourceToScreen.apply( py, qy );
		source.sourceToScreen.apply( pz, qz );

		graphics.setPaint( Color.WHITE );
		graphics.setFont( new Font( "SansSerif", Font.PLAIN, 8 ) );
		graphics.drawString( "x", ( float ) perspectiveX( qx ), ( float ) perspectiveY( qx ) - 2 );
		graphics.drawString( "y", ( float ) perspectiveX( qy ), ( float ) perspectiveY( qy ) - 2 );
		graphics.drawString( "z", ( float ) perspectiveX( qz ), ( float ) perspectiveY( qz ) - 2 );
	}

}
