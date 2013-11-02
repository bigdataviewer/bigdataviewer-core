package viewer.render;

import net.imglib2.display.Volatile;
import net.imglib2.ui.InterruptibleProjector;

public interface VolatileProjector extends InterruptibleProjector
{
	/**
	 * Render the target image.
	 *
	 * @param clearUntouchedTargetPixels
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	public boolean map( boolean clearUntouchedTargetPixels );

	/**
	 * @return true if all mapped pixels were {@link Volatile#isValid() valid}.
	 */
	public boolean isValid();
}
