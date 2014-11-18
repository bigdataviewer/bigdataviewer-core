package bdv.viewer.animate;

import java.awt.Graphics2D;

public interface OverlayAnimator
{
	public abstract void paint( final Graphics2D g, final long time );

	/**
	 * Returns true if the animation is complete and the animator can be
	 * removed.
	 *
	 * @return whether the animation is complete and the animator can be
	 *         removed.
	 */
	public boolean isComplete();

	/**
	 * Returns true if the animator requires an immediate repaint to continue
	 * the animation.
	 *
	 * @return whetherhe animator requires an immediate repaint.
	 */
	public boolean requiresRepaint();
}
