package net.imglib.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import net.imglib.ui.component.InteractiveDisplay2DCanvas;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.TransformListener2D;
import viewer.GuiHelpers;

public class InteractiveRealViewer2Db< T > implements TransformListener2D, PainterThread.Paintable
{
	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform2D viewerTransform;

	/**
	 * Canvas used for displaying the rendered {@link #screenImages screen image}.
	 */
	final protected InteractiveDisplay2DCanvas display;

	/**
	 * Thread that triggers repainting of the display.
	 */
	final protected PainterThread painterThread;

	final protected SimpleRenderer imageRenderer;

	final protected JFrame frame;

	final protected SimpleSource< T > source;

	public InteractiveRealViewer2Db( final int width, final int height, final RealRandomAccessible< T > source, final AffineTransform2D sourceTransform, final Converter< T, ARGBType > converter )
	{
		this( width, height, new SimpleSource< T >() {
			@Override
			public RealRandomAccessible< T > getInterpolatedSource()
			{
				return source;
			}

			@Override
			public AffineTransform2D getSourceTransform()
			{
				return sourceTransform;
			}

			@Override
			public Converter< T, ARGBType > getConverter()
			{
				return converter;
			}
		} );
	}

	public InteractiveRealViewer2Db( final int width, final int height, final SimpleSource< T > source )
	{
		painterThread = new PainterThread( this );
		viewerTransform = new AffineTransform2D();
		display = new InteractiveDisplay2DCanvas( width, height, null, this );

		final boolean doubleBuffered = true;
		final int numRenderingThreads = 3;
		imageRenderer = new SimpleRenderer( display, painterThread, doubleBuffered, numRenderingThreads );

//		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( ARGBScreenImage.ARGB_COLOR_MODEL );
		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( GuiHelpers.RGB_COLOR_MODEL );
		frame = new JFrame( "ImgLib2", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				painterThread.interrupt();
			}
		} );
		frame.setVisible( true );

		this.source = source;

		painterThread.start();

	}

	@Override
	public void paint()
	{
		imageRenderer.paint( source, viewerTransform );
		display.repaint();
	}


	@Override
	public void transformChanged( final AffineTransform2D transform )
	{
		viewerTransform.set( transform );
		requestRepaint();
	}

	public void requestRepaint()
	{
		imageRenderer.requestRepaint();
	}
}
