package bdv.viewer.render;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.pivovarit.function.ThrowingRunnable;
import com.sun.javafx.tk.PlatformImage;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.util.GuiUtil;

public class RenderResult
{
	private final AffineTransform3D viewerTransform = new AffineTransform3D();

	private int width;
	private int height;

	// TODO: rename
	//  renderData?
	private int[] data = new int[ 0 ];

	// TODO: rename
	//  renderImage, img, image?
	private ARGBScreenImage screenImage;

	private BufferedImage bufferedImage;

	private double scaleFactor;

	private MyJavaFXImage javaFXImage;

	public void init( final int width, final int height )
	{
		if ( this.width == width && this.height == height )
			return;

		this.width = width;
		this.height = height;

		if ( data.length < width * height )
			data = new int[ width * height ];

		screenImage = new ARGBScreenImage( width, height, data );
		bufferedImage = GuiUtil.getBufferedImage( screenImage );

		try
		{
			javaFXImage = new MyJavaFXImage( width, height, data );
		}
		catch ( NoSuchMethodException
				| SecurityException
				| NoSuchFieldException
				| IllegalArgumentException
				| IllegalAccessException
				| InvocationTargetException e )
		{
			e.printStackTrace();
		}
	}

	public Image getJavaFXImage()
	{
		return javaFXImage;
	}

	/**
	 * Get the image to render to.
	 * @return
	 */
	public RandomAccessibleInterval< ARGBType > getScreenImage()
	{
		return screenImage;
	}

	public BufferedImage getBufferedImage()
	{
		return bufferedImage;
	}

	/**
	 * Get the viewer transform used to render image.
	 * This is with respect to the screen resolution (doesn't include scaling).
	 *
	 */
	public AffineTransform3D getViewerTransform()
	{
		return viewerTransform;
	}

	public double getScaleFactor()
	{
		return scaleFactor;
	}

	public void setScaleFactor( final double scaleFactor )
	{
		this.scaleFactor = scaleFactor;
	}

	public void setPixelsDirty()
	{
		this.javaFXImage.callPixelsDirty.run();
	}

	/**
	 * Taken from
	 * https://github.com/saalfeldlab/paintera/blob/master/src/main/java/bdv/fx/viewer/render/BufferExposingWritableImage.java
	 */
	public static class MyJavaFXImage extends WritableImage
	{

		private final Method setWritablePlatformImage;

		private final Method pixelsDirty;

		private final Field serial;

		private final Runnable callPixelsDirty;

		private final com.sun.prism.Image prismImage;

		public MyJavaFXImage( int width, int height, int[] data ) throws NoSuchMethodException,
				SecurityException,
				NoSuchFieldException,
				IllegalArgumentException,
				IllegalAccessException,
				InvocationTargetException
		{
			super( width, height );

			this.setWritablePlatformImage = Image.class.getDeclaredMethod( "setPlatformImage", PlatformImage.class );
			this.setWritablePlatformImage.setAccessible( true );

			this.prismImage = com.sun.prism.Image.fromIntArgbPreData( data, width, height );
			this.setWritablePlatformImage.invoke( this, prismImage );

			this.pixelsDirty = Image.class.getDeclaredMethod( "pixelsDirty" );
			this.pixelsDirty.setAccessible( true );

			this.serial = com.sun.prism.Image.class.getDeclaredField( "serial" );
			this.serial.setAccessible( true );

			this.callPixelsDirty = ThrowingRunnable.unchecked( () -> {
				final int[] serial = ( int[] ) this.serial.get( prismImage );
				serial[ 0 ]++;
				this.pixelsDirty.invoke( this );
			} );
		}

		public void setPixelsDirty()
		{
			this.callPixelsDirty.run();
		}
	}
}
