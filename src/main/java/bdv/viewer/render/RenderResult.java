/*-
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

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

/**
 * Provides the {@link MultiResolutionRenderer renderer} with a target image
 * ({@code RandomAccessibleInterval<ARGBType>}) to render to. Provides the
 * {@link RenderTarget} with the rendered image and transform etc necessary to
 * display it.
 */
public interface RenderResult
{
	/**
	 * Allocate storage such that {@link #getTargetImage()} holds an image of
	 * {@code width * height}.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 */
	void init( int width, int height );

	/**
	 * Get the image to render to.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 *
	 * @return the image to render to
	 */
	RandomAccessibleInterval< ARGBType > getTargetImage();

	/**
	 * Get the viewer transform used to render image. This is with respect to
	 * the screen resolution (doesn't include scaling).
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer} to set the
	 * transform.)
	 */
	AffineTransform3D getViewerTransform();

	/**
	 * Get the scale factor from target coordinates to screen resolution.
	 */
	double getScaleFactor();

	/**
	 * Set the scale factor from target coordinates to screen resolution.
	 */
	void setScaleFactor( double scaleFactor );

	/**
	 * Fill in {@code interval} with data from {@code patch}, scaled by the
	 * relative scale between this {@code RenderResult} and {@code patch}, and
	 * shifted such that {@code (0,0)} of the {@code patch} is placed at
	 * {@code (ox,oy)} of this {@code RenderResult}
	 * <p>
	 * Note that only data in {@code interval} will be modified, although the
	 * scaled and shifted {@code patch} might fall partially outside.
	 */
	void patch( final RenderResult patch, final Interval interval, final double ox, final double oy );

	/**
	 * Notify that the {@link #getTargetImage() target image} data was changed.
	 * <p>
	 * (Called by the {@link MultiResolutionRenderer renderer}.)
	 */
	void setUpdated();
}
