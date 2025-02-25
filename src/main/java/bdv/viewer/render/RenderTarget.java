/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
 * Receiver for a rendered image (to be drawn onto a canvas later).
 * <p>
 * A renderer will render source data into a {@link RenderResult} and
 * provide this to the {@code RenderTarget}.
 * <p>
 * See {@code BufferedImageOverlayRenderer}, which is both a {@code RenderTarget} and
 * an {@code OverlayRenderer} that draws the {@code RenderResult}.
 *
 * @author Tobias Pietzsch
 */
public interface RenderTarget< R extends RenderResult >
{
	/**
	 * Returns a {@code RenderResult} for rendering to.
	 * This may be a new {@code RenderResult} or a previously {@link #setRenderResult set RenderResult}
	 * that is no longer needed for display.
	 * Note that consecutive {@code getReusableRenderResult()} calls without intermediate
	 * {@code setRenderResult()} may return the same {@code RenderResult}.
	 */
	R getReusableRenderResult();

	/**
	 * Returns a new {@code RenderResult}.
	 */
	R createRenderResult();

	/**
	 * Set the {@link RenderResult} that is to be drawn on the canvas.
	 */
	void setRenderResult( R renderResult );

	/**
	 * Get the current canvas width.
	 *
	 * @return canvas width.
	 */
	int getWidth();

	/**
	 * Get the current canvas height.
	 *
	 * @return canvas height.
	 */
	int getHeight();
}
