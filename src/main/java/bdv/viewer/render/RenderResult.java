package bdv.viewer.render;

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
}
