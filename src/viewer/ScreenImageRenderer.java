package viewer;

import java.awt.Graphics;

import net.imglib2.display.ARGBScreenImage;

public interface ScreenImageRenderer
{
	/**
	 * This is called, when the {@link #screenImage} was updated.
	 * @param yScale TODO
	 * @param xScale TODO
	 */
	public void screenImageChanged( final ARGBScreenImage screenImage, double xScale, double yScale );

	/**
	 * Render the {@link #screenImage}.
	 *
	 * @return true if the image was completely rendered, false if rendering was
	 *         {@link #cancelDrawing() canceled}.
	 */
	public boolean drawScreenImage();

	/**
	 * Stop rendering the {@link #screenImage} prematurely.
	 */
	public void cancelDrawing();

	/**
	 * Render overlays.
	 */
	public void drawOverlays( final Graphics g );
}
