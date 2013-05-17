package viewer.util;

import net.imglib2.realtransform.AffineTransform3D;
import viewer.SpimViewer;

/**
 * Mother abstract class for animators that animate the current view in a
 * {@link SpimViewer} instance by modifying the viewer transform. The time unit
 * for animation duration, start time and current time is not specified, or
 * example you can use <b>ms</b> obtained from
 * {@link System#currentTimeMillis()} or a frame number when rendering movies.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>
 */
public abstract class AbstractTransformAnimator extends AbstractAnimator
{
	/**
	 * Create new animator with the given duration. The animation will start
	 * with the first call to {@link #setTime(long)}.
	 *
	 * @param duration
	 *            animation duration (in time units)
	 */
	public AbstractTransformAnimator( final long duration )
	{
		super( duration );
	}

	/**
	 * Returns an {@link AffineTransform3D} that can be used to set the
	 * viewpoint of a {@link SpimViewer} instance, for the time specified.
	 *
	 * @param time
	 *            the target absolute time for which the transform should be
	 *            generated (in time units).
	 * @return viewer transform for the given time.
	 */
	public AffineTransform3D getCurrent( final long time )
	{
		setTime( time );
		return get( ratioComplete() );
	}

	/**
	 * Returns an {@link AffineTransform3D} for the specified completion factor.
	 * For values below 0, that starting transform should be returned. For
	 * values larger than 1, the final transform should be returned. Values
	 * below 0 and 1 should interpolate between the two, depending on the
	 * concrete animation implementation.
	 *
	 * @param t
	 *            the completion factor, ranging from 0 to 1.
	 * @return the viewer transform for the specified completion factor.
	 */
	protected abstract AffineTransform3D get( double t );
}
