package net.imglib.ui.component;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JComponent;

import net.imglib.ui.OverlayRenderer;
import net.imglib.ui.TransformEventHandler;
import net.imglib.ui.TransformEventHandlerFactory;
import net.imglib.ui.TransformListener;

public class InteractiveDisplayCanvas< T > extends JComponent implements TransformListener< T >
{
	/**
	 * Mouse/Keyboard handler that manipulates the viewer transformation.
	 */
	final protected TransformEventHandler< T > handler;

	final protected OverlayRenderer renderer;

	final protected ArrayList< TransformListener< T > > transformListeners;

	/**
	 * The {@link BufferedImage} that is actually drawn on the canvas. Depending
	 * on {@link #discardAlpha} this is either the {@link BufferedImage}
	 * obtained from {@link #screenImage}, or {@link #screenImage}s buffer
	 * re-wrapped using a RGB color model.
	 */
	protected BufferedImage bufferedImage;

	public InteractiveDisplayCanvas( final int width, final int height, final OverlayRenderer renderer, final TransformEventHandlerFactory< T > factory )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );
		setFocusable( true );

		this.bufferedImage = null;
		this.renderer = renderer;
		this.transformListeners = new ArrayList< TransformListener< T > >();

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				handler.setCanvasSize( getWidth(), getHeight(), true );
				enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );
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

		handler = factory.create( this );
		handler.setCanvasSize( width, height, false );
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
			renderer.drawOverlays( g );
		}
	}

	@Override
	public void transformChanged( final T transform )
	{
		for ( final TransformListener< T > l : transformListeners )
			l.transformChanged( transform );
	}

	public TransformEventHandler< T > getTransformEventHandler()
	{
		return handler;
	}
}