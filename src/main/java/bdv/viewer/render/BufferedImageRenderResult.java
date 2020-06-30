package bdv.viewer.render;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.util.GuiUtil;

public class BufferedImageRenderResult implements RenderResult
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

	@Override
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

	@Override
	public RandomAccessibleInterval< ARGBType > getScreenImage()
	{
		return screenImage;
	}

	public BufferedImage getBufferedImage()
	{
		return bufferedImage;
	}

	@Override
	public AffineTransform3D getViewerTransform()
	{
		return viewerTransform;
	}

	@Override
	public double getScaleFactor()
	{
		return scaleFactor;
	}

	@Override
	public void setScaleFactor( final double scaleFactor )
	{
		this.scaleFactor = scaleFactor;
	}



	// interval

	public void compose( final BufferedImageRenderResult intervalResult, final Interval targetInterval, final double tx, final double ty, final double s )
	{
		try
		{
			final AffineTransform transform = new AffineTransform( s, 0, 0, s, tx, ty );
			final AffineTransformOp op = new AffineTransformOp( transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR );
			op.filter( intervalResult.bufferedImage, subImage( targetInterval ) );
//			op.filter( intervalResult.bufferedImage, bufferedImage );
		}
		catch( Throwable e )
		{
			System.out.println( "e = " + e );
		}
	}

	private BufferedImage subImage( final Interval interval )
	{
		return bufferedImage.getSubimage( ( int ) interval.min( 0 ), ( int ) interval.min( 1 ), ( int ) interval.dimension( 0 ), ( int ) interval.dimension( 1 ) );
	}


}
