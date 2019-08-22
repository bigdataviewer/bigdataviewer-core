/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import bdv.cache.CacheControl;
import bdv.viewer.RequestRepaint;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.Renderer;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * A {@link Renderer} that uses a coarse-to-fine rendering scheme. First, a
 * small image at a fraction of the canvas resolution is
 * rendered. Then, increasingly larger images are rendered, until the full
 * canvas resolution is reached.
 * <p>
 * When drawing the low-resolution image to the screen, they
 * will be scaled up by Java2D to the full canvas size, which is relatively
 * fast. Rendering the small, low-resolution images is usually very fast, such
 * that the display is very interactive while the user changes the viewing
 * transformation for example. When the transformation remains fixed for a
 * longer period, higher-resolution details are filled in successively.
 * <p>
 * The renderer allocates a image for each of a predefined set
 * of <em>screen scales</em> (a screen scale of 1 means that 1 pixel in the
 * screen image is displayed as 1 pixel on the canvas, a screen scale of 0.5
 * means 1 pixel in the screen image is displayed as 2 pixel on the canvas,
 * etc.)
 * <p>
 * At any time, one of these screen scales is selected as the
 * <em>highest screen scale</em>. Rendering starts with this highest screen
 * scale and then proceeds to lower screen scales (higher resolution images).
 * Unless the highest screen scale is currently rendering,
 * {@link #requestRepaint() repaint request} will cancel rendering, such that
 * display remains interactive.
 * <p>
 * The renderer tries to maintain a per-frame rendering time close to a desired
 * number of <code>targetRenderNanos</code> nanoseconds. If the rendering time
 * (in nanoseconds) for the (currently) highest scaled screen image is above
 * this threshold, a coarser screen scale is chosen as the highest screen scale
 * to use. Similarly, if the rendering time for the (currently) second-highest
 * scaled screen image is below this threshold, this finer screen scale chosen
 * as the highest screen scale to use.
 * <p>
 * The renderer uses multiple threads (if desired) and double-buffering (if
 * desired).
 * <p>
 * Double buffering means that three images are
 * created for every screen scale. After rendering the first one of them and
 * setting it to the {@link net.imglib2.ui.RenderTarget}, next time, rendering goes to the
 * second one, then to the third. The {@link net.imglib2.ui.RenderTarget} will always have a
 * complete image, which is not rendered to while it is potentially drawn to the
 * screen. When setting an image to the {@link net.imglib2.ui.RenderTarget}, the
 * {@link net.imglib2.ui.RenderTarget} will release one of the previously set images to be
 * rendered again. Thus, rendering will not interfere with painting the
 * image to the canvas.
 * <p>
 * The renderer supports rendering of {@link Volatile} sources. In each
 * rendering pass, all currently valid data for the best fitting mipmap level
 * and all coarser levels is rendered to a temporary image}
 * for each visible source. Then the temporary images are combined to the final
 * image for display. The number of passes required until all data is valid
 * might differ between visible sources.
 * <p>
 * Rendering timing is tied to a {@link CacheControl} control for IO budgeting, etc.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class GeneralMultiResolutionRenderer {
	/**
	 * Receiver for the images that we render.
	 */
	private final RenderTarget display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	private final RequestRepaint painterThread;

	private final SingleResolutionRenderer renderer;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * source data to the screen image.
	 */
	private VolatileProjector projector;

	/**
	 * The index of the screen scale of the {@link #projector current projector}.
	 */
	private int currentScreenScaleIndex;

	/**
	 * List of scale factors and associate image buffers
	 */
	private final List<ScreenScale> screenScales;

	/**
	 * The last rendered interval in screen space.
	 */
	private Interval lastRenderedScreenInterval;

	/**
	 * Rendering first starts with a high screen scale. Because that's
	 * fast. This high screen scale is selected such that the
	 * rendering time is below this threshold.
	 */
	private final long targetRenderNanos;

	/**
	 * The index of the screen scale which should be rendered next.
	 */
	private int requestedScreenScaleIndex;

	/**
	 * Whether the current rendering operation may be cancelled (to start a
	 * new one). Rendering may be cancelled unless we are rendering at
	 * coarsest screen scale and coarsest mipmap level.
	 */
	private volatile boolean renderingMayBeCancelled;

	/**
	 * Controls IO budgeting and fetcher queue.
	 */
	private final CacheControl cacheControl;

	/**
	 * Whether a repaint was {@link #requestRepaint(Interval) requested}. This will
	 * // TODO discuss this with Tobias Pietzsch
	 * cause {@link CacheControl#prepareNextFrame()}.
	 */
	private boolean newFrameRequest;

	/**
	 * Size of the display.
	 */
	private Interval screenSize;

	private static final Interval ALL = new FinalInterval(
			new long[] { Long.MIN_VALUE, Long.MIN_VALUE },
			new long[] { Long.MAX_VALUE, Long.MAX_VALUE } );

	/**
	 * @param display
	 *            The canvas that will display the images we render.
	 * @param painterThread
	 *            Thread that triggers repainting of the display. Requests for
	 *            repainting are send there.
	 * @param screenScales
	 *            Scale factors from the viewer canvas to screen images of
	 *            different resolutions. A scale factor of 1 means 1 pixel in
	 *            the screen image is displayed as 1 pixel on the canvas, a
	 *            scale factor of 0.5 means 1 pixel in the screen image is
	 *            displayed as 2 pixel on the canvas, etc.
	 * @param targetRenderNanos
	 *            Target rendering time in nanoseconds. The rendering time for
	 *            the coarsest rendered scale should be below this threshold.
	 * @param numRenderingThreads
	 *            How many threads to use for rendering.
	 * @param renderingExecutorService
	 *            if non-null, this is used for rendering. Note, that it is
	 *            still important to supply the numRenderingThreads parameter,
	 *            because that is used to determine into how many sub-tasks
	 *            rendering is split.
	 * @param useVolatileIfAvailable
	 *            whether volatile versions of sources should be used if
	 *            available.
	 * @param accumulateProjectorFactory
	 *            can be used to customize how sources are combined.
	 * @param cacheControl
	 *            the cache controls IO budgeting and fetcher queue.
	 */
	public GeneralMultiResolutionRenderer(
			final RenderTarget display,
			final RequestRepaint painterThread,
			final double[] screenScales,
			final long targetRenderNanos,
			final int numRenderingThreads,
			final ExecutorService renderingExecutorService,
			final boolean useVolatileIfAvailable,
			final AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory,
			final CacheControl cacheControl)
	{
		this.display = display;
		this.painterThread = painterThread;
		projector = null;
		currentScreenScaleIndex = -1;
		this.screenScales = DoubleStream.of(screenScales).mapToObj(ScreenScale::new).collect( Collectors.toList() );
		this.targetRenderNanos = targetRenderNanos;
		this.screenSize = new FinalInterval(display.getWidth(), display.getHeight());

		requestedScreenScaleIndex = screenScales.length - 1;
		renderingMayBeCancelled = true;
		this.cacheControl = cacheControl;
		newFrameRequest = false;
		this.renderer = new SingleResolutionRenderer(numRenderingThreads, renderingExecutorService, accumulateProjectorFactory, useVolatileIfAvailable);
	}

	/**
	 * Check whether the size of the display component was changed and
	 * set screen scale size accordingly.
	 *
	 * @return whether the size was changed.
	 */
	private synchronized boolean checkResize()
	{
		final Interval newScreen = new FinalInterval( display.getWidth(), display.getHeight() );
		if ( ! Intervals.equals(screenSize, newScreen) ) {
			screenSize = newScreen;
			return true;
		}
		return false;
	}

	/**
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale}.
	 */
	public boolean paint( final RenderState state )
	{
		final boolean resized = checkResize();

		// the projector that paints to the screenImage.
		final VolatileProjector p;

		final boolean createProjector;

		final Interval repaintScreenInterval;

		RenderResult result = null;

		synchronized ( this )
		{
			repaintScreenInterval = screenScales.get(requestedScreenScaleIndex).pullPendingInterval();

			if (repaintScreenInterval == null || ! intersectsScreen(repaintScreenInterval))
				return false;

			final boolean sameAsLastRenderedInterval = lastRenderedScreenInterval != null && Intervals.equals(repaintScreenInterval, lastRenderedScreenInterval);

			// Rendering may be cancelled unless we are rendering at coarsest
			// screen scale and coarsest mipmap level.
			renderingMayBeCancelled = requestedScreenScaleIndex < suggestScreenScale(repaintScreenInterval);

			final boolean clearQueue = newFrameRequest;
			if ( clearQueue )
				cacheControl.prepareNextFrame();
			createProjector = newFrameRequest || resized || ( requestedScreenScaleIndex != currentScreenScaleIndex ) || !sameAsLastRenderedInterval;
			newFrameRequest = false;

			if ( createProjector )
			{
				currentScreenScaleIndex = requestedScreenScaleIndex;
				ScreenScale screenScale = screenScales.get(currentScreenScaleIndex);
				synchronized ( state )
				{
					lastRenderedScreenInterval = repaintScreenInterval;
					result = createRenderResult(state.getViewerTransform().copy(), repaintScreenInterval, screenScale);
					p = createProjectorForInterval(state, screenScale, result);
				}
				projector = p;
			}
			else
			{
				p = projector;
			}

			requestedScreenScaleIndex = 0;
		}

		// try rendering
		final boolean success = p.map( createProjector );
		final long rendertime = p.getLastFrameRenderNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				if ( createProjector )
				{
					display.setRenderResult(result);
					reportRenderingTime( result, rendertime );
				}

				if ( currentScreenScaleIndex > 0 )
					requestRepaint(lastRenderedScreenInterval, currentScreenScaleIndex - 1);
				else if ( !p.isValid() )
				{
					try
					{
						Thread.sleep( 1 );
					}
					catch ( final InterruptedException e )
					{
						// restore interrupted state
						Thread.currentThread().interrupt();
					}
					requestRepaint(lastRenderedScreenInterval, currentScreenScaleIndex);
				}
			}
			else
			{
				// Add the requested interval back into the queue if it was not rendered
				screenScales.get(currentScreenScaleIndex).requestInterval(repaintScreenInterval);
			}

			return success;
		}
	}

	private boolean intersectsScreen(Interval repaintScreenInterval) {
		return ! Intervals.isEmpty( Intervals.intersect(repaintScreenInterval, screenSize) );
	}

	private RenderResult createRenderResult(AffineTransform3D viewerTransform,
			Interval repaintScreenInterval,
			ScreenScale screenScale) {
		boolean complete = Intervals.equals(repaintScreenInterval, ALL);
		if(complete)
			repaintScreenInterval = new FinalInterval( display.getWidth(), display.getHeight());
		Interval scaledScreen = Intervals.largestContainedInterval(scaleInterval(screenSize, screenScale.scaleFactor));
		final RandomAccessibleInterval<ARGBType> bufferedImage = display.createOutputImage(
				(int) scaledScreen.dimension(0), (int) scaledScreen.dimension(1) );
		final RealInterval scaledInterval = scaleInterval(repaintScreenInterval, screenScale.scaleFactor);
		final Interval paddedScaledInterval = getPaddedRenderTargetInterval(bufferedImage, scaledInterval);
		return new RenderResult(bufferedImage, viewerTransform, currentScreenScaleIndex, screenScale.scaleFactor,
				complete, repaintScreenInterval, scaledInterval, paddedScaledInterval
		);
	}

	public void reportRenderingTime(RenderResult result, long timeInNanos) {
		int scaleIndex = result.getScaleIndex();
		if(result.isComplete())
			screenScales.get(scaleIndex).renderTimePerScreenPixel =
					(double) timeInNanos / Intervals.numElements( result.getScreenInterval() );
	}

	private VolatileProjector createProjectorForInterval(RenderState viewerState, ScreenScale screenScale, RenderResult result) {

		Interval interval = result.getPaddedScaledInterval();

		AffineTransform3D viewerTransform = viewerState.getViewerTransform().copy();
		viewerTransform.preConcatenate(screenScale.screenScaleTransforms);
		viewerTransform.translate( - interval.min(0), - interval.min(1), 0 );

		RenderState renderState = new RenderState(viewerTransform, viewerState.getCurrentTimepoint(), viewerState.getSources());

		final RandomAccessibleInterval<ARGBType> renderTargetRoi = Views.zeroMin(
				Views.interval(result.getImage(), interval) );

		VolatileProjector projector = renderer.createProjector(
				renderState,
				renderTargetRoi,
				screenSize
		);
		newFrameRequest |= renderer.isNewFrameRequest();
		return projector;
	}

	private Interval getPaddedRenderTargetInterval(RandomAccessibleInterval<ARGBType> renderTarget, RealInterval renderTargetRealInterval) {
		// apply 1px padding on each side of the render target repaint interval to avoid interpolation artifacts
		Interval padded = Intervals.expand(Intervals.smallestContainingInterval(renderTargetRealInterval), 1);
		return Intervals.intersect(padded, renderTarget);
	}

	private RealInterval scaleInterval(Interval interval, double scale) {
		// TODO: move to intervals class
		// scale the screen repaint request interval into render target coordinates
		int n = interval.numDimensions();
		final double[] min = new double[n];
		final double[] max = new double[n];
		Arrays.setAll(min, d -> interval.min(d) * scale);
		Arrays.setAll(max, d -> interval.max(d) * scale);
		return new FinalRealInterval(min, max);
	}

	public void requestRepaint() {
		requestRepaint(ALL);
	}

	/**
	 * Request a repaint of the display from the painter thread, with maximum
	 * screen scale index and mipmap level.
	 */
	public synchronized void requestRepaint(final Interval interval)
	{
		newFrameRequest = true;
		requestRepaint(interval, suggestScreenScale(interval));
	}

	private int suggestScreenScale(Interval interval) {
		Interval actualInterval = interval == null ? screenSize : Intervals.intersect(interval, screenSize);
		double numPixels = Intervals.numElements(actualInterval);
		for(int i = 0; i < screenScales.size() - 1; i++) {
			double renderTime = numPixels * screenScales.get(i).renderTimePerScreenPixel;
			if (renderTime <= targetRenderNanos)
				return i;
		}
		return screenScales.size() - 1;
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint(RenderState)} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint(RenderState)} has
	 * completed).
	 */
	public synchronized void requestRepaint(Interval interval, final int screenScaleIndex)
	{
		if (interval == null)
			interval = ALL;

		if (Intervals.isEmpty(interval))
			return;

		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		if ( screenScaleIndex > requestedScreenScaleIndex )
			requestedScreenScaleIndex = screenScaleIndex;

		screenScales.get(requestedScreenScaleIndex).requestInterval(interval);

		painterThread.requestRepaint();
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
		if ( display instanceof TransformAwareBufferedImageOverlayRenderer )
			( ( TransformAwareBufferedImageOverlayRenderer ) display ).kill();
		projector = null;
	}

	private static AffineTransform3D screenScaleTransform(double scaleFactor) {
		final AffineTransform3D scale = new AffineTransform3D();
		scale.scale(scaleFactor);
		scale.set( 0.5 * (scaleFactor - 1), 0, 3 );
		scale.set( 0.5 * (scaleFactor - 1), 1, 3 );
		return scale;
	}

	/**
	 * Scale factor and associated image buffers and transformation.
	 */
	private static class ScreenScale {

		/**
		 * Scale factors from the {@link #display viewer canvas} to the
		 * screen image.
		 *
		 * A scale factor of 1 means 1 pixel in the screen image is displayed as 1
		 * pixel on the canvas, a scale factor of 0.5 means 1 pixel in the screen
		 * image is displayed as 2 pixel on the canvas, etc.
		 */
		private final double scaleFactor;

		private final AffineTransform3D screenScaleTransforms;

		/**
		 * Pending repaint requests.
		 */
		private Interval pendingRepaintRequests;

		/**
		 * Estimate of the time it takes to render one screen pixel, in nanoseconds.
		 */
		private double renderTimePerScreenPixel = Double.POSITIVE_INFINITY;

		private ScreenScale(double scaleFactor) {
			this.scaleFactor = scaleFactor;
			screenScaleTransforms = screenScaleTransform(scaleFactor);
		}

		public synchronized void requestInterval(Interval repaintScreenInterval) {
			if (pendingRepaintRequests == null)
				pendingRepaintRequests = repaintScreenInterval;
			else
				pendingRepaintRequests = Intervals.union(pendingRepaintRequests, repaintScreenInterval);
		}

		public synchronized Interval pullPendingInterval() {
			Interval repaintScreenInterval = pendingRepaintRequests;
			pendingRepaintRequests = null;
			return repaintScreenInterval;
		}
	}
}
