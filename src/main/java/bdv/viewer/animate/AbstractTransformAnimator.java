/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.ViewerFrame;

/**
 * Mother abstract class for animators that animate the current view in a
 * {@link ViewerFrame} instance by modifying the viewer transform. The time unit
 * for animation duration, start time and current time is not specified, or
 * example you can use <b>ms</b> obtained from
 * {@link System#currentTimeMillis()} or a frame number when rendering movies.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt;
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
	 * viewpoint of a {@link ViewerFrame} instance, for the time specified.
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
	public abstract AffineTransform3D get( double t );
}
