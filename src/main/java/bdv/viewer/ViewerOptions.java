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
package bdv.viewer;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.TransformEventHandlerFactory;
import bdv.viewer.animate.MessageOverlayAnimator;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.MultiResolutionRenderer;

/**
 * Optional parameters for {@link ViewerPanel}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ViewerOptions
{
	public final Values values = new Values();

	/**
	 * Create default {@link ViewerOptions}.
	 * @return default {@link ViewerOptions}.
	 */
	public static ViewerOptions options()
	{
		return new ViewerOptions();
	}

	/**
	 * Set width of {@link ViewerPanel} canvas.
	 */
	public ViewerOptions width( final int w )
	{
		values.width = w;
		return this;
	}

	/**
	 * Set height of {@link ViewerPanel} canvas.
	 */
	public ViewerOptions height( final int h )
	{
		values.height = h;
		return this;
	}

	/**
	 * Set the number and scale factors for scaled screen images.
	 *
	 * @param s
	 *            Scale factors from the viewer canvas to screen images of
	 *            different resolutions. A scale factor of 1 means 1 pixel in
	 *            the screen image is displayed as 1 pixel on the canvas, a
	 *            scale factor of 0.5 means 1 pixel in the screen image is
	 *            displayed as 2 pixel on the canvas, etc.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions screenScales( final double[] s )
	{
		values.screenScales = s;
		return this;
	}

	/**
	 * Set target rendering time in nanoseconds.
	 *
	 * @param t
	 *            Target rendering time in nanoseconds. The rendering time for
	 *            the coarsest rendered scale should be below this threshold.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions targetRenderNanos( final long t )
	{
		values.targetRenderNanos = t;
		return this;
	}

	/**
	 * Set whether to used double buffered rendering.
	 *
	 * @param d
	 *            Whether to use double buffered rendering.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions doubleBuffered( final boolean d )
	{
		values.doubleBuffered = d;
		return this;
	}

	/**
	 * Set how many threads to use for rendering.
	 *
	 * @param n
	 *            How many threads to use for rendering.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions numRenderingThreads( final int n )
	{
		values.numRenderingThreads = n;
		return this;
	}

	/**
	 * Set whether volatile versions of sources should be used if available.
	 *
	 * @param v
	 *            whether volatile versions of sources should be used if
	 *            available.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions useVolatileIfAvailable( final boolean v )
	{
		values.useVolatileIfAvailable = v;
		return this;
	}

	public ViewerOptions msgOverlay( final MessageOverlayAnimator o )
	{
		values.msgOverlay = o;
		return this;
	}

	public ViewerOptions transformEventHandlerFactory( final TransformEventHandlerFactory< AffineTransform3D > f )
	{
		values.transformEventHandlerFactory = f;
		return this;
	}

	/**
	 * Set the factory for creating {@link AccumulateProjector}. This can be
	 * used to customize how sources are combined.
	 *
	 * @param f
	 *            factory for creating {@link AccumulateProjector}.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions accumulateProjectorFactory( final AccumulateProjectorFactory< ARGBType > f )
	{
		values.accumulateProjectorFactory = f;
		return this;
	}

	/**
	 * Read-only {@link ViewerOptions} values.
	 */
	public static class Values
	{
		private int width = 800;

		private int height = 600;

		private double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

		private long targetRenderNanos = 30 * 1000000l;

		private boolean doubleBuffered = true;

		private int numRenderingThreads = 3;

		private boolean useVolatileIfAvailable = true;

		private MessageOverlayAnimator msgOverlay = new MessageOverlayAnimator( 800 );

		private TransformEventHandlerFactory< AffineTransform3D > transformEventHandlerFactory = TransformEventHandler3D.factory();

		private AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory = AccumulateProjectorARGB.factory;

		public ViewerOptions optionsFromValues()
		{
			return new ViewerOptions().
				width( width ).
				height( height ).
				screenScales( screenScales ).
				targetRenderNanos( targetRenderNanos ).
				doubleBuffered( doubleBuffered ).
				numRenderingThreads( numRenderingThreads ).
				useVolatileIfAvailable( useVolatileIfAvailable ).
				msgOverlay( msgOverlay ).
				transformEventHandlerFactory( transformEventHandlerFactory ).
				accumulateProjectorFactory( accumulateProjectorFactory );
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}

		public double[] getScreenScales()
		{
			return screenScales;
		}

		public long getTargetRenderNanos()
		{
			return targetRenderNanos;
		}

		public boolean isDoubleBuffered()
		{
			return doubleBuffered;
		}

		public int getNumRenderingThreads()
		{
			return numRenderingThreads;
		}

		public boolean isUseVolatileIfAvailable()
		{
			return useVolatileIfAvailable;
		}

		public MessageOverlayAnimator getMsgOverlay()
		{
			return msgOverlay;
		}

		public TransformEventHandlerFactory< AffineTransform3D > getTransformEventHandlerFactory()
		{
			return transformEventHandlerFactory;
		}

		public AccumulateProjectorFactory< ARGBType > getAccumulateProjectorFactory()
		{
			return accumulateProjectorFactory;
		}
	}
}
