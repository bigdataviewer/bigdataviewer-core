package viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener3D;

public class SpimViewerCanvas extends JComponent
{
	private static final long serialVersionUID = 6187867732580868714L;

	/**
	 * Mouse/Keyboard handler that manipulates the viewer transformation.
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

	public SpimViewerCanvas( final int width, final int height, final ScreenImageRenderer renderer, final TransformListener3D renderTransformListener )
	{
		super();
		setPreferredSize( new Dimension( width, height ) );

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

			AffineTransform3D tmp = new AffineTransform3D();

			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				final int h = getHeight();

				tmp.set( handler.getTransform() );
				tmp.set( tmp.get( 0, 3 ) - oldW/2, 0, 3 );
				tmp.set( tmp.get( 1, 3 ) - oldH/2, 1, 3 );
				tmp.scale( ( double ) w / oldW );
				tmp.set( tmp.get( 0, 3 ) + w/2, 0, 3 );
				tmp.set( tmp.get( 1, 3 ) + h/2, 1, 3 );
				handler.setTransform( tmp );
				handler.setWindowCenter( w / 2, h / 2 );
				renderTransformListener.transformChanged( tmp );

				oldW = w;
				oldH = h;
			}
		} );

		handler = new TransformEventHandler3D( renderTransformListener );
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
//			final int biw = bi.getWidth();
//			final int bih = bi.getHeight();
//			final int w = getWidth();
//			final int h = getHeight();
//			final boolean b = g.drawImage( bi, 0, 0, w, h, null );
//			System.out.println( String.format( "%d, %d, %d, %d, %b", biw, bih, w, h, b ) );
			g.drawImage( bi, 0, 0, getWidth(), getHeight(), null );
		}
		renderer.drawOverlays( g );
	}

	public TransformEventHandler3D getTransformEventHandler()
	{
		return handler;
	}
}
