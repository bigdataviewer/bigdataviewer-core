package net.imglib.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import net.imglib.ui.component.InteractiveDisplayCanvas;
import net.imglib.ui.util.GuiUtil;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class InteractiveRealViewer< T, A extends AffineSet & AffineGet & Concatenable< AffineGet > > implements TransformListener< A >, PainterThread.Paintable
{
	/**
	 * TODO: RealTransform should extend Type. Then we could simply use set()
	 * and createVariable() instead of having to define this interface.
	 */
	public interface AffineTransformType< A >
	{
		public TransformEventHandlerFactory< A > transformEvenHandlerFactory();

		public A createTransform();

		/**
		 * Set <code>transformToSet</code> to the value of <code>transform</code>.
		 */
		public void set( A transformToSet, A transform  );
	}

	final protected AffineTransformType< A > transformType;

	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected A viewerTransform;

	/**
	 * Canvas used for displaying the rendered {@link #screenImages screen image}.
	 */
	final protected InteractiveDisplayCanvas< A > display;

	/**
	 * Thread that triggers repainting of the display.
	 */
	final protected PainterThread painterThread;

	final protected SimpleRenderer< A > imageRenderer;

	final protected JFrame frame;

	final protected SimpleSource< T, A > source;

	public InteractiveRealViewer( final AffineTransformType< A > transformType, final int width, final int height, final SimpleSource< T, A > source, final boolean doubleBuffered, final int numRenderingThreads )
	{
		this.transformType = transformType;
		painterThread = new PainterThread( this );
		viewerTransform = transformType.createTransform();
		display = new InteractiveDisplayCanvas< A >( width, height, transformType.transformEvenHandlerFactory() );
		display.addTransformListener( this );

		imageRenderer = new SimpleRenderer< A >( display, painterThread, doubleBuffered, numRenderingThreads );

//		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( GuiHelpers.ARGB_COLOR_MODEL );
		final GraphicsConfiguration gc = GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL );
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
	public void transformChanged( final A transform )
	{
		transformType.set( viewerTransform, transform );
		requestRepaint();
	}

	public InteractiveDisplayCanvas< A > getDisplayCanvas()
	{
		return display;
	}

	public void requestRepaint()
	{
		imageRenderer.requestRepaint();
	}


	/**
	 * Default {@link AffineTransformType} implementation for {@link AffineTransform2D}.
	 */
	public static class AffineTransformType2D implements AffineTransformType< AffineTransform2D >
	{
		public static final AffineTransformType2D instance = new AffineTransformType2D();

		@Override
		public TransformEventHandlerFactory< AffineTransform2D > transformEvenHandlerFactory()
		{
			return TransformEventHandler2D.factory();
		}

		@Override
		public AffineTransform2D createTransform()
		{
			return new AffineTransform2D();
		}

		@Override
		public void set( final AffineTransform2D transformToSet, final AffineTransform2D transform )
		{
			transformToSet.set( transform );
		}
	}

	/**
	 * Default {@link AffineTransformType} implementation for {@link AffineTransform2D}.
	 */
	public static class AffineTransformType3D implements AffineTransformType< AffineTransform3D >
	{
		public static final AffineTransformType3D instance = new AffineTransformType3D();

		@Override
		public TransformEventHandlerFactory< AffineTransform3D > transformEvenHandlerFactory()
		{
			return TransformEventHandler3D.factory();
		}

		@Override
		public AffineTransform3D createTransform()
		{
			return new AffineTransform3D();
		}

		@Override
		public void set( final AffineTransform3D transformToSet, final AffineTransform3D transform )
		{
			transformToSet.set( transform );
		}
	}
}
