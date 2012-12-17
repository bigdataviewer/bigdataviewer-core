package viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.swing.JComponent;

import net.imglib2.display.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.TransformListener3D;

public class InteractiveDisplay3DCanvas extends JComponent implements Paintable, TransformListener3D
{
	private static final long serialVersionUID = 6187867732580868714L;

	MappingThread painter = null;

	/**
	 * Used to render the image for on-screen display.
	 */
	protected ARGBScreenImage screenImage;

	protected double screenToViewerScale = 1.0;

	/**
	 * The transformation interactively set by the user.
	 */
	final protected AffineTransform3D viewerTransform;

	/**
	 * Mouse/Keyboard handler to manipulate {@link #viewerTransform} transformation.
	 */
	final protected TransformEventHandler3D handler;

	final protected ScreenImageRenderer renderer;

	final protected TransformListener3D renderTransformListener;

	/**
	 * The {@link BufferedImage} that is actually drawn on the canvas. Depending
	 * on {@link #discardAlpha} this is either the {@link BufferedImage}
	 * obtained from {@link #screenImage}, or {@link #screenImage}s buffer
	 * re-wrapped using a RGB color model.
	 */
	protected BufferedImage bufferedImage;

	/**
	 * Whether to discard the {@link #screenImage} alpha components when drawing.
	 */
	final protected boolean discardAlpha = true;

	public InteractiveDisplay3DCanvas( final int width, final int height, final ScreenImageRenderer renderer, final TransformListener3D renderTransformListener )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );

		this.screenImage = new ARGBScreenImage( width, height );
		this.bufferedImage = getBufferedImage( screenImage );
		viewerTransform = new AffineTransform3D();
		this.renderer = renderer;
		renderer.screenImageChanged( screenImage, 1, 1 );
		this.renderTransformListener = renderTransformListener;

		addComponentListener( new ComponentListener()
		{
			@Override
			public void componentShown( final ComponentEvent e ) {}

			@Override
			public void componentMoved( final ComponentEvent e ) {}

			@Override
			public void componentHidden( final ComponentEvent e ) {}

			int oldW = width;

			int oldH = height;

			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();
				synchronized( viewerTransform )
				{
					viewerTransform.set( handler.getTransform() );
					viewerTransform.set( viewerTransform.get( 0, 3 ) - oldW/2, 0, 3 );
					viewerTransform.set( viewerTransform.get( 1, 3 ) - oldH/2, 1, 3 );
					viewerTransform.scale( ( double ) w / oldW );
					viewerTransform.set( viewerTransform.get( 0, 3 ) + w/2, 0, 3 );
					viewerTransform.set( viewerTransform.get( 1, 3 ) + h/2, 1, 3 );
					handler.setTransform( viewerTransform );
					handler.setWindowCenter( w / 2, h / 2 );
					renderTransformListener.transformChanged( viewerTransform );
				}
				oldW = w;
				oldH = h;
				requestRepaint();
			}
		} );

		handler = new TransformEventHandler3D( this );
		handler.setWindowCenter( width / 2, height / 2 );
		addHandler( handler );
	}

	static final private ColorModel RGB_COLOR_MODEL = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);

	private BufferedImage getBufferedImage( final ARGBScreenImage screenImage )
	{
		if ( discardAlpha )
		{
			final BufferedImage si = screenImage.image();
			final SampleModel sampleModel = RGB_COLOR_MODEL.createCompatibleWritableRaster( 1, 1 ).getSampleModel().createCompatibleSampleModel( si.getWidth(), si.getHeight() );
			final DataBuffer dataBuffer = si.getRaster().getDataBuffer();
			final WritableRaster rgbRaster = Raster.createWritableRaster( sampleModel, dataBuffer, null );
			return new BufferedImage( RGB_COLOR_MODEL, rgbRaster, false, null );
		}
		else
			return screenImage.image();
	}

	@Override
	public void setPainterThread( final MappingThread painter )
	{
		this.painter = painter;
	}

	@Override
	public void requestRepaint()
	{
		requestRepaint( currentScreenScaleIndex );
	}

	public synchronized void requestRepaint( final int screenScaleIndex )
	{
		if( currentScreenScaleIndex > 0 )
			renderer.cancelDrawing();
		currentScreenScaleIndex = screenScaleIndex;
		painter.repaint();
	}

	@Override
	public void paint()
	{
		screenToViewerScale = screenScales[ currentScreenScaleIndex ];
		System.out.println( "screenToViewerScale = " + screenToViewerScale );
		final int componentW = getWidth();
		final int componentH = getHeight();
		final int w = ( int ) ( screenToViewerScale * componentW );
		final int h = ( int ) ( screenToViewerScale * componentH );
		if ( screenImage.dimension( 0 ) != w || screenImage.dimension( 1 ) != h )
		{
			final double xScale = ( double ) w / componentW;
			final double yScale = ( double ) h / componentH;

			screenImage = new ARGBScreenImage( w, h );
			renderer.screenImageChanged( screenImage, xScale, yScale );
		}
		if( renderer.drawScreenImage() )
		{
			bufferedImage = getBufferedImage( screenImage );
			if ( currentScreenScaleIndex < screenScales.length - 1 )
			{
				++currentScreenScaleIndex;
				requestRepaint();
			}
		}
		repaint();
	}


	protected double[] screenScales = new double[] { 0.125, 0.25, 0.5, 1 };

	protected int currentScreenScaleIndex = 0;

	// TODO: automatic resolution switching
	public void SWITCH_RESOLUTION_TEST( final double scale )
	{
		screenToViewerScale = 1.0 / scale;
		requestRepaint();
	}

	/**
	 * Add new event handler.
	 */
	@Override
	public void addHandler( final Object handler )
	{
		if ( KeyListener.class.isInstance( handler ) )
			addKeyListener( ( KeyListener ) handler );

		if ( MouseMotionListener.class.isInstance( handler ) )
			addMouseMotionListener( ( MouseMotionListener ) handler );

		if ( MouseListener.class.isInstance( handler ) )
			addMouseListener( ( MouseListener ) handler );

		if ( MouseWheelListener.class.isInstance( handler ) )
			addMouseWheelListener( ( MouseWheelListener ) handler );
	}

	@Override
	public void paintComponent( final Graphics g )
	{
		( (Graphics2D ) g).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		g.drawImage( bufferedImage, 0, 0, getWidth(), getHeight(), null );
		renderer.drawOverlays( g );
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized( viewerTransform )
		{
			viewerTransform.set( transform );
		}
		renderTransformListener.transformChanged( transform );
		requestRepaint( 0 );
	}

	public TransformEventHandler3D getTransformEventHandler()
	{
		return handler;
	}
}
