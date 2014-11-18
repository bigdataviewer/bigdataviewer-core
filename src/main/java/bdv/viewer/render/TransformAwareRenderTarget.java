package bdv.viewer.render;

import java.awt.image.BufferedImage;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformListener;

public interface TransformAwareRenderTarget extends RenderTarget
{
	/**
	 * Set the {@link BufferedImage} that is to be drawn on the canvas, and the
	 * transform with which this image was created.
	 *
	 * @param img
	 *            image to draw (may be null).
	 */
	public BufferedImage setBufferedImageAndTransform( final BufferedImage img, final AffineTransform3D transform );

	public void addTransformListener( final TransformListener< AffineTransform3D > listener );

	public void addTransformListener( final TransformListener< AffineTransform3D > listener, final int index );

	public void removeTransformListener( final TransformListener< AffineTransform3D > listener );
}
