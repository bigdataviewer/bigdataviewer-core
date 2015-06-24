package bdv.tools.boundingbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import bdv.viewer.overlay.RenderBoxHelper;

public class BoundingBoxOverlay implements OverlayRenderer, TransformListener< AffineTransform3D >
{
	public static interface BoundingBoxOverlaySource
	{
		public Interval getInterval();

		public void getIntervalTransform( AffineTransform3D transform );
	}

	private final Color backColor = new Color( 0x00994499 );// Color.MAGENTA;

	private final Color frontColor = Color.GREEN;

	private final BoundingBoxOverlaySource bbSource;

	private final AffineTransform3D viewerTransform;

	private final AffineTransform3D transform;

	private final RenderBoxHelper renderBoxHelper;

	private int canvasWidth;

	private int canvasHeight;

	public BoundingBoxOverlay( final Interval interval )
	{
		this( new BoundingBoxOverlaySource()
		{
			@Override
			public Interval getInterval()
			{
				return interval;
			}

			@Override
			public void getIntervalTransform( final AffineTransform3D transform )
			{
				transform.identity();
			}
		} );
	}

	public BoundingBoxOverlay( final BoundingBoxOverlaySource bbSource )
	{
		this.bbSource = bbSource;
		this.viewerTransform = new AffineTransform3D();
		this.transform = new AffineTransform3D();
		this.renderBoxHelper = new RenderBoxHelper();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final Graphics2D graphics = ( Graphics2D ) g;

		final GeneralPath front = new GeneralPath();
		final GeneralPath back = new GeneralPath();

		final Interval interval = bbSource.getInterval();
		final double perspective = 3;
		final double sourceSize = Math.max( Math.max( interval.dimension( 0 ), interval.dimension( 1 ) ), interval.dimension( 2 ) );
		final double ox = canvasWidth / 2;
		final double oy = canvasHeight / 2;
		bbSource.getIntervalTransform( transform );
		transform.preConcatenate( viewerTransform );
		renderBoxHelper.setDepth( perspective * sourceSize );
		renderBoxHelper.setOrigin( ox, oy );
		renderBoxHelper.setScale( 1 );
		renderBoxHelper.renderBox( interval, transform, front, back );

		final AffineTransform t = graphics.getTransform();
		final AffineTransform translate = new AffineTransform( 1, 0, 0, 1, ox, oy );
		translate.preConcatenate( t );
		graphics.setTransform( translate );
		graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		graphics.setPaint( backColor );
		graphics.draw( back );
		graphics.setPaint( frontColor );
		graphics.draw( front );
		graphics.setTransform( t );
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.canvasWidth = width;
		this.canvasHeight = height;
	}

	@Override
	public void transformChanged( final AffineTransform3D t )
	{
		viewerTransform.set( t );
	}
}
