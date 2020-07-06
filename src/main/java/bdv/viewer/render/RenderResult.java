package bdv.viewer.render;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

/**
 * TODO javadoc
 */
public interface RenderResult
{
	/**
	 * TODO javadoc
	 */
	void init( int width, int height );

	/**
	 * TODO javadoc
	 * Get the image to render to.
	 *
	 * @return
	 */
	// TODO: rename getTargetImage() ???
	RandomAccessibleInterval< ARGBType > getScreenImage();

	/**
	 * TODO javadoc
	 * Get the viewer transform used to render image.
	 * This is with respect to the screen resolution (doesn't include scaling).
	 */
	AffineTransform3D getViewerTransform();

	/**
	 * TODO javadoc
	 */
	double getScaleFactor();

	/**
	 * TODO javadoc
	 */
	void setScaleFactor( double scaleFactor );

	/**
	 * Fill in {@code interval} with data from {@code patch},
	 * scaled by the relative scale between this {@code RenderResult} and {@code patch},
	 * and shifted such that {@code (0,0)} of the {@code patch} is placed at {@code (ox,oy)} of this {@code RenderResult}
	 * <p>
	 * Note that only data in {@code interval} will be modified, although the scaled and shifted {@code patch} might fall partially outside.
	 */
	void patch( final RenderResult patch, final Interval interval, final double ox, final double oy );

	void setUpdated();
}
