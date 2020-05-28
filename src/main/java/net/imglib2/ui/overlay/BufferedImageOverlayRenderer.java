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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformListener;

/**
 * {@link OverlayRenderer} drawing a {@link BufferedImage}, scaled to fill the
 * canvas. It can be used as a {@link RenderTarget}, such that the
 * {@link BufferedImage} to draw is set by a renderer.
 *
 * @author Tobias Pietzsch
 *
 * TODO: REVISE JAVADOC
 */
public class BufferedImageOverlayRenderer implements OverlayRenderer, RenderTarget
{

	/**
	 * The {@link BufferedImage} that is actually drawn on the canvas. Depending
	 * on {@link Defaults#discardAlpha} this is either the {@link BufferedImage}
	 * obtained from screen image, or screen image's buffer re-wrapped using a RGB
	 * color model.
	 */
	private BufferedImage bufferedImage;

	/**
	 * A {@link BufferedImage} that has been previously
	 * {@link #setBufferedImage(BufferedImage) set} for painting. Whenever a new
	 * image is set, this is stored here and marked {@link #pending}. Whenever
	 * an image is painted and a new image is pending, the new image is painted
	 * to the screen. Before doing this, the image previously used for painting
	 * is swapped into {@link #pendingImage}. This is used for double-buffering.
	 */
	private BufferedImage pendingImage;

	private AffineTransform3D pendingTransform;

	private AffineTransform3D paintedTransform;

	/**
	 * These listeners will be notified about the transform that is associated
	 * to the currently rendered image. This is intended for example for
	 * {@link OverlayRenderer}s that need to exactly match the transform of
	 * their overlaid content to the transform of the image.
	 */
	private final CopyOnWriteArrayList< TransformListener< AffineTransform3D > > paintedTransformListeners;

	/**
	 * Whether an image is pending.
	 */
	private boolean pending;

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
		bufferedImage = null;
		pendingImage = null;
		pending = false;
		width = 0;
		height = 0;
		pendingTransform = new AffineTransform3D();
		paintedTransform = new AffineTransform3D();
		paintedTransformListeners = new CopyOnWriteArrayList<>();
	}

	/**
	 * Set the {@link BufferedImage} that is to be drawn on the canvas.
	 *
	 * @param img
	 *            image to draw (may be null).
	 */
	// TODO REMOVE?
	@Override
	public synchronized BufferedImage setBufferedImage( final BufferedImage img )
	{
		final BufferedImage tmp = pendingImage;
		pendingImage = img;
		pending = true;
		return tmp;
	}

	@Override
	public synchronized BufferedImage setBufferedImageAndTransform( final BufferedImage img, final AffineTransform3D transform )
	{
		pendingTransform.set( transform );
		return setBufferedImage( img );
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
		boolean notifyTransformListeners = false;
		synchronized ( this )
		{
			if ( pending )
			{
				final BufferedImage tmp = bufferedImage;
				bufferedImage = pendingImage;
				paintedTransform.set( pendingTransform );
				pendingImage = tmp;
				pending = false;
				notifyTransformListeners = true;
			}
		}
		if ( bufferedImage != null )
		{
//			final StopWatch watch = new StopWatch();
//			watch.start();
//			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
			g.drawImage( bufferedImage, 0, 0, getWidth(), getHeight(), null );
			if ( notifyTransformListeners )
				for ( final TransformListener< AffineTransform3D > listener : paintedTransformListeners )
					listener.transformChanged( paintedTransform );
//			System.out.println( String.format( "g.drawImage() :%4d ms", watch.nanoTime() / 1000000 ) );
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		this.width = width;
		this.height = height;
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 */
	@Override
	public void addTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		addTransformListener( listener, Integer.MAX_VALUE );
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 * @param index
	 *            position in the list of listeners at which to insert this one.
	 */
	@Override
	public void addTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		synchronized ( paintedTransformListeners )
		{
			final int s = paintedTransformListeners.size();
			paintedTransformListeners.add( clamp( index, 0, s ), listener );
			listener.transformChanged( paintedTransform );
		}
	}

	private static int clamp( int value, int min, int max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	/**
	 * Remove a {@link TransformListener}.
	 *
	 * @param listener
	 *            the transform listener to remove.
	 */
	@Override
	public void removeTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		synchronized ( paintedTransformListeners )
		{
			paintedTransformListeners.remove( listener );
		}
	}

	/**
	 * DON'T USE THIS.
	 * <p>
	 * This is a work around for JDK bug
	 * https://bugs.openjdk.java.net/browse/JDK-8029147 which leads to
	 * ViewerPanel not being garbage-collected when ViewerFrame is closed. So
	 * instead we need to manually let go of resources...
	 */
	public /*TODO don't make public, move to render package instead */ void kill()
	{
		bufferedImage = null;
		pendingImage = null;
	}
}
