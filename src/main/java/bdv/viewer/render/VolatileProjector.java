/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.viewer.render;

/**
 * Render a 2D target copying pixels from a nD source.
 * <p>
 * Rendering can be interrupted, in which case {@link #map} will return false.
 * Also, the rendering time for the last {@link #map} can be queried.
 *
 * @author Tobias Pietzsch
 * @author Stephan Saalfeld
 */
public interface VolatileProjector
{
	/**
	 * Render the target image.
	 *
	 * @param clearUntouchedTargetPixels
	 * @return true if rendering was completed (all target pixels written).
	 *         false if rendering was interrupted.
	 */
	boolean map( boolean clearUntouchedTargetPixels );

	default boolean map()
	{
		return map( true );
	}

	/**
	 * Abort {@link #map()} if it is currently running.
	 */
	void cancel();

	/**
	 * How many nano-seconds did the last {@link #map()} take.
	 *
	 * @return time needed for rendering the last frame, in nano-seconds.
	 */
	long getLastFrameRenderNanoTime();

	/**
	 * @return true if all mapped pixels were valid.
	 */
	boolean isValid();
}
