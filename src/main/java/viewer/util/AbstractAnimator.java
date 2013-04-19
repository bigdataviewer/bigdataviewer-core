package viewer.util;

import viewer.SpimViewer;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Mother abstract class for animators that can animate the current view
 * in a {@link SpimViewer} instance.
 */
public abstract class AbstractAnimator
{
	/** Expected duration length of the animation, in <b>ms</b>. */ 
	private final long duration;
	
	/** Start time of the animation, in ms. */
	private long startTime;

	/** Boolean flag stating whether the animation started. */
	private boolean started;

	/** Completion factor, ranging from 0 to 1. If >= 1, the animation is done. */
	private double complete;

	public AbstractAnimator( final long duration )
	{
		this.duration = duration;
		started = false;
		complete = 0;
	}

	/** 
	 * Sets the starting time for the animation.
	 * @param time
	 */
	public void setTime( final long time )
	{
		if ( ! started )
		{
			started = true;
			startTime = time;
		}

		complete = ( time - startTime ) / ( double ) duration;
		if ( complete >= 1 )
			complete = 1;
	}

	/**
	 * Returns true if the animation completed.
	 * @return true if the animation completed.
	 */
	public boolean isComplete()
	{
		return complete == 1;
	}

	/**
	 * Returns the completion ratio. It is a double ranging from 0 to 1, o indicating that
	 * the animation just started, 1 indicating that it completed.
	 * @return the completion ratio.
	 */
	public double ratioComplete()
	{
		return complete;
	}
	
	/**
	 * Returns an {@link AffineTransform3D} that can be used to set the viewpoint of 
	 * a {@link SpimViewer} instance, for the time specified.  
	 * @param time  the target absolute time for which the transform should be generated, 
	 * in ms. 
	 * @return  an {@link AffineTransform3D} for a {@link SpimViewer} viewpoint.
	 * @see System#currentTimeMillis()
	 */
	public AffineTransform3D getCurrent( final long time )
	{
		setTime( time );
		return get( ratioComplete() );
	}
	
	/**
	 * Returns an {@link AffineTransform3D} for the specified completion factor.
	 * For values below 0, that starting transform should be returned. For values larger 
	 * than 1, the final transform should be returned. Values below 0 and 1 should interpolate
	 * between the two, depending on the concrete animation implementation.  
	 * @param t  the completion factor, ranging from 0 to 1.
	 * @return  the transform for the specified completion factor.
	 */
	protected abstract AffineTransform3D get(double t);
}
