/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
package net.imglib2.ui.overlay;

import bdv.util.TripleBuffer;
import bdv.util.TripleBuffer.ReadableBuffer;
import bdv.viewer.render.BufferedImageRenderResult;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformListener;
import org.scijava.listeners.Listeners;

/**
 * {@link OverlayRenderer} drawing a {@link BufferedImage}, scaled to fill the
 * canvas. It can be used as a {@link RenderTarget}, such that the
 * {@link BufferedImage} to draw is set by a renderer.
 *
 * @author Tobias Pietzsch
 *
 * TODO: REVISE JAVADOC
 */
public class BufferedImageOverlayRenderer implements OverlayRenderer, RenderTarget< BufferedImageRenderResult >
{
	private final TripleBuffer< BufferedImageRenderResult > tripleBuffer;

	/**
	 * These listeners will be notified about the transform that is associated
	 * to the currently rendered image. This is intended for example for
	 * {@link OverlayRenderer}s that need to exactly match the transform of
	 * their overlaid content to the transform of the image.
	 */
	private final Listeners.List< TransformListener< AffineTransform3D > > paintedTransformListeners;

	private final AffineTransform3D paintedTransform;

	/**
	 * The current canvas width.
	 */
	private volatile int width;

	/**
	 * The current canvas height.
	 */
	private volatile int height;

	public BufferedImageOverlayRenderer()
	{
		tripleBuffer = new TripleBuffer<>( BufferedImageRenderResult::new );
		width = 0;
		height = 0;
		paintedTransform = new AffineTransform3D();
		paintedTransformListeners = new Listeners.SynchronizedList<>( l -> l.transformChanged( paintedTransform ) );
	}

	@Override
	public BufferedImageRenderResult getReusableRenderResult()
	{
		return tripleBuffer.getWritableBuffer();
	}

	@Override
	public BufferedImageRenderResult createRenderResult()
	{
		return new BufferedImageRenderResult();
	}

	/**
	 * Set the {@code RenderResult} that is to be drawn on the canvas.
	 *
	 * @param result
	 *            image to draw (may be null).
	 */
	@Override
	public synchronized void setRenderResult( final BufferedImageRenderResult result )
	{
		tripleBuffer.doneWriting( result );
	}

	@Override
	public int getWidth()
	{
		return width;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		final ReadableBuffer< BufferedImageRenderResult > rb = tripleBuffer.getReadableBuffer();
		final BufferedImageRenderResult result = rb.getBuffer();
		final BufferedImage image = result != null ? result.getBufferedImage() : null;
		if ( image != null )
		{
//			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );

			final double scaleFactor = result.getScaleFactor();
			final int w = Math.max( width, ( int ) ( image.getWidth() / scaleFactor + 0.5 ) );
			final int h = Math.max( height, ( int ) ( image.getHeight() / scaleFactor + 0.5 ) );
			g.drawImage( image, 0, 0, w, h, null );

			if ( rb.isUpdated() )
			{
				paintedTransform.set( result.getViewerTransform() );
				paintedTransformListeners.list.forEach( listener -> listener.transformChanged( paintedTransform ) );
			}
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	/**
	 * Add/remove {@code TransformListener}s to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 */
	public Listeners< TransformListener< AffineTransform3D > > transformListeners()
	{
		return transformListeners();
	}

	/**
	 * DON'T USE THIS.
	 * <p>
	 * This is a work around for JDK bug
	 * https://bugs.openjdk.java.net/browse/JDK-8029147 which leads to
	 * ViewerPanel not being garbage-collected when ViewerFrame is closed. So
	 * instead we need to manually let go of resources...
	 */
	public void kill()
	{
		tripleBuffer.clear();
	}
}
