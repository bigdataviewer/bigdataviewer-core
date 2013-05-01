package net.imglib.ui.component;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import net.imglib.ui.OverlayRenderer;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.ui.TransformEventHandler2D;
import net.imglib2.ui.TransformListener2D;

public class InteractiveDisplay2DCanvas extends JComponent
{
	private static final long serialVersionUID = 2242249370342430878L;

	/**
	 * Mouse/Keyboard handler that manipulates the viewer transformation.
	 */
	final protected TransformEventHandler2D handler;

	final protected OverlayRenderer renderer;

	final protected TransformListener2D renderTransformListener;

	/**
	 * The {@link BufferedImage} that is actually drawn on the canvas. Depending
	 * on {@link #discardAlpha} this is either the {@link BufferedImage}
	 * obtained from {@link #screenImage}, or {@link #screenImage}s buffer
	 * re-wrapped using a RGB color model.
	 */
	protected BufferedImage bufferedImage;

	public InteractiveDisplay2DCanvas( final int width, final int height, final OverlayRenderer renderer, final TransformListener2D renderTransformListener )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );
		setFocusable( true );

		this.bufferedImage = null;
		this.renderer = renderer;
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

			AffineTransform2D tmp = new AffineTransform2D();

			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();

				tmp.set( handler.getTransform() );
				tmp.set( tmp.get( 0, 2 ) - oldW/2, 0, 2 );
				tmp.set( tmp.get( 1, 2 ) - oldH/2, 1, 2 );
				tmp.scale( ( double ) w / oldW );
				tmp.set( tmp.get( 0, 2 ) + w/2, 0, 2 );
				tmp.set( tmp.get( 1, 2 ) + h/2, 1, 2 );
				handler.setTransform( tmp );
				handler.setWindowCenter( w / 2, h / 2 );
				renderTransformListener.transformChanged( tmp );
				enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );

				oldW = w;
				oldH = h;
			}
		} );

		addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				requestFocusInWindow();
			}
		} );

		handler = new TransformEventHandler2D( renderTransformListener );
		handler.setWindowCenter( width / 2, height / 2 );
		addHandler( handler );
	}

	/**
	 * The {@link BufferedImage} that is to be drawn on the canvas.
	 *
	 * @param bufferedImage image to draw.
	 */
	public synchronized void setBufferedImage( final BufferedImage bufferedImage )
	{
		this.bufferedImage = bufferedImage;
	}


	/**
	 * Add new event handler.
	 */
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
		final BufferedImage bi;
		synchronized ( this )
		{
			bi = bufferedImage;
		}
		if ( bi != null )
		{
//			( (Graphics2D ) g).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			g.drawImage( bi, 0, 0, getWidth(), getHeight(), null );
			if ( renderer != null )
				renderer.drawOverlays( g );
		}
	}

	public TransformEventHandler2D getTransformEventHandler()
	{
		return handler;
	}
}
