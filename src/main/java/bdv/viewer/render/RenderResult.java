package bdv.viewer.render;

import java.awt.image.BufferedImage;
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

	public void init( final int width, final int height )
	{
		if ( this.width == width && this.height == height )
			return;

		this.width = width;
		this.height = height;

		if ( data.length < width * height )
			data = new int[ width * height ];

		screenImage = new ARGBScreenImage( width, height, data );
		bufferedImage = GuiUtil.getBufferedImage( screenImage );;
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
}
