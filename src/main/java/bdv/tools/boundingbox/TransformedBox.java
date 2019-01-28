package bdv.tools.boundingbox;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * A transformed box (to display in a {@code BoundingBoxOverlay}).
 * Represented as a {@code RealInterval} that is placed into
 * global coordinate system by an {@code AffineTransform3D}.
 */
public interface TransformedBox
{
	RealInterval getInterval();

	void getTransform( final AffineTransform3D transform );
}
