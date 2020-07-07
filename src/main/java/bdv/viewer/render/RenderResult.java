package bdv.viewer.render;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

/**
 * Provides the {@link MultiResolutionRenderer renderer} with a target image
 * ({@code RandomAccessibleInterval<ARGBType>}) to render to. Provides the
 * {@link RenderTarget} with the rendered image and transform etc necessary to
 * display it.
 */
public interface RenderResult
{
	/**
	 * Allocate storage such that {@link #getScreenImage()} holds an image of
	 * {@code width * height}.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 */
	void init( int width, int height );

	/**
	 * Get the image to render to.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 *
	 * @return the image to render to
	 */
	// TODO: rename getTargetImage() ???
	RandomAccessibleInterval< ARGBType > getScreenImage();

	/**
	 * Get the viewer transform used to render image. This is with respect to
	 * the screen resolution (doesn't include scaling).
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer} to set the
	 * transform.)
	 */
	AffineTransform3D getViewerTransform();

	/**
	 * Get the scale factor from target coordinates to screen resolution.
	 */
	double getScaleFactor();

	/**
	 * Set the scale factor from target coordinates to screen resolution.
	 */
	void setScaleFactor( double scaleFactor );

	/**
	 * Fill in {@code interval} with data from {@code patch}, scaled by the
	 * relative scale between this {@code RenderResult} and {@code patch}, and
	 * shifted such that {@code (0,0)} of the {@code patch} is placed at
	 * {@code (ox,oy)} of this {@code RenderResult}
	 * <p>
	 * Note that only data in {@code interval} will be modified, although the
	 * scaled and shifted {@code patch} might fall partially outside.
	 */
	void patch( final RenderResult patch, final Interval interval, final double ox, final double oy );

	/**
	 * Notify that the {@link #getScreenImage() target image} data was changed.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 */
	void setUpdated();
}
