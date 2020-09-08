/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;

import bdv.cache.CacheControl;
import bdv.util.MovingAverage;
import bdv.viewer.RequestRepaint;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import bdv.viewer.render.ScreenScales.IntervalRenderData;
import bdv.viewer.render.ScreenScales.ScreenScale;

/**
 * A renderer that uses a coarse-to-fine rendering scheme. First, a small target
 * image at a fraction of the canvas resolution is rendered. Then, increasingly
 * larger images are rendered, until the full canvas resolution is reached.
 * <p>
 * When drawing the low-resolution target images to the screen, they will be
 * scaled up by Java2D (or JavaFX, etc) to the full canvas size, which is
 * relatively fast. Rendering the small, low-resolution images is usually very
 * fast, such that the display is very interactive while the user changes the
 * viewing transformation for example. When the transformation remains fixed for
 * a longer period, higher-resolution details are filled in successively.
 * <p>
 * The renderer allocates a {@code RenderResult} for each of a predefined set of
 * <em>screen scales</em> (a screen scale of 1 means that 1 pixel in the screen
 * image is displayed as 1 pixel on the canvas, a screen scale of 0.5 means 1
 * pixel in the screen image is displayed as 2 pixel on the canvas, etc.)
 * <p>
 * At any time, one of these screen scales is selected as the <em>highest screen
 * scale</em>. Rendering starts with this highest screen scale and then proceeds
 * to lower screen scales (higher resolution images). Unless the highest screen
 * scale is currently rendering, {@link #requestRepaint() repaint request} will
 * cancel rendering, such that display remains interactive.
 * <p>
 * The renderer tries to maintain a per-frame rendering time close to
 * {@code targetRenderNanos} nanoseconds. The current highest screen scale is
 * chosen to match this time based on per-output-pixel time measured in previous
 * frames.
 * <p>
 * The renderer uses multiple threads (if desired).
 * <p>
 * The renderer supports rendering of {@link Volatile} sources. In each
 * rendering pass, all currently valid data for the best fitting mipmap level
 * and all coarser levels is rendered to a temporary image for each visible
 * source. Then the temporary images are combined to the final image for
 * display. The number of passes required until all data is valid might differ
 * between visible sources.
 * <p>
 * Rendering timing is tied to a {@link CacheControl} control for IO budgeting,
 * etc.
 *
 * @author Tobias Pietzsch
 */
public class MultiResolutionRenderer
{
	/**
	 * Receiver for the {@code BufferedImage BufferedImages} that we render.
	 */
	private final RenderTarget< ? > display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	private final RequestRepaint painterThread;

	/**
	 * Creates projectors for rendering current {@code ViewerState} to a
	 * {@code screenImage}.
	 */
	private final ProjectorFactory projectorFactory;

	/**
	 * Controls IO budgeting and fetcher queue.
	 */
	private final CacheControl cacheControl;

	// TODO: should be settable
	private final long[] iobudget = new long[] { 100l * 1000000l,  10l * 1000000l };

	/**
	 * Maintains current sizes and transforms at every screen scale level.
	 * Records interval rendering requests.
	 */
	private final ScreenScales screenScales;

	/**
	 * Maintains arrays for intermediate per-source render images and masks.
	 */
	private final RenderStorage renderStorage;

	/**
	 * Estimate of the time it takes to render one screen pixel from one source,
	 * in nanoseconds.
	 */
	private final MovingAverage renderNanosPerPixelAndSource;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * source data to {@code ©screenImages}. {@code projector.cancel()} can be
	 * used to cancel the ongoing rendering operation.
	 */
	private VolatileProjector projector;

	/**
	 * Whether the current rendering operation may be cancelled (to start a new
	 * one). Rendering may be cancelled unless we are rendering at the
	 * (estimated) coarsest screen scale meeting the rendering time threshold.
	 */
	private boolean renderingMayBeCancelled;

	/**
	 * Snapshot of the ViewerState that is currently being rendered.
	 * A new snapshot is taken in the first {@code paint()} pass after a (full frame) {@link #requestRepaint()}
	 */
	private ViewerState currentViewerState;

	/**
	 * The sources that are actually visible on screen currently. This means
	 * that the sources both are visible in the {@link #currentViewerState} (via
	 * {@link ViewerState#getVisibleAndPresentSources()
	 * getVisibleAndPresentSources}) and, when transformed to viewer
	 * coordinates, overlap the screen area ({@link #display}).
	 */
	private final List< SourceAndConverter< ? > > currentVisibleSourcesOnScreen;


	/**
	 * The last successfully rendered (not cancelled) full frame result.
	 * This result is by full frame refinement passes and/or interval rendering passes.
	 */
	private RenderResult currentRenderResult;

	/**
	 * If {@code true}, then we are painting intervals currently.
	 * If {@code false}, then we are painting full frames.
	 */
	private boolean intervalMode;

	/*
	 *
	 * === FULL FRAME RENDERING ===
	 *
	 */

	/**
	 * Screen scale of the last successful (not cancelled) rendering pass in
	 * full-frame mode.
	 */
	private int currentScreenScaleIndex;

	/**
	 * The index of the screen scale which should be rendered next in full-frame
	 * mode.
	 */
	private int requestedScreenScaleIndex;

	/**
	 * Whether a full frame repaint was {@link #requestRepaint() requested}.
	 * Supersedes {@link #newIntervalRequest}.
	 */
	private boolean newFrameRequest;

	/*
	 *
	 * === INTERVAL RENDERING ===
	 *
	 */

	/**
	 * Screen scale of the last successful (not cancelled) rendering pass in
	 * interval mode.
	 */
	private int currentIntervalScaleIndex;

	/**
	 * The index of the screen scale which should be rendered next in interval
	 * mode.
	 */
	private int requestedIntervalScaleIndex;

	/**
	 * Whether repainting of an interval was {@link #requestRepaint(Interval)
	 * requested}. The union of all pending intervals are recorded in
	 * {@link #screenScales}. Pending interval requests are obsoleted by full
	 * frame repaints requests ({@link #newFrameRequest}).
	 */
	private boolean newIntervalRequest;

	/**
	 * Re-used for all interval rendering.
	 */
	private final RenderResult intervalResult;

	/**
	 * Currently rendering interval. This is pulled from {@link #screenScales}
	 * at the start of {@code paint()}, which clears requested intervals at
	 * {@link #currentIntervalScaleIndex} or coarser. (This ensures that new
	 * interval requests arriving during rendering are not missed. If the
	 * requested intervals would be cleared after rendering, this might happen.
	 * Instead we re-request the pulled intervals, if rendering fails.)
	 */
	private IntervalRenderData intervalRenderData;

	/**
	 * @param display
	 *            The canvas that will display the images we render.
	 * @param painterThread
	 *            Thread that triggers repainting of the display. Requests for
	 *            repainting are send there.
	 * @param screenScaleFactors
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
	public MultiResolutionRenderer(
			final RenderTarget< ? > display,
			final RequestRepaint painterThread,
			final double[] screenScaleFactors,
			final long targetRenderNanos,
			final int numRenderingThreads,
			final ExecutorService renderingExecutorService,
			final boolean useVolatileIfAvailable,
			final AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory,
			final CacheControl cacheControl )
	{
		this.display = display;
		this.painterThread = painterThread;
		projector = null;
		currentScreenScaleIndex = -1;
		currentVisibleSourcesOnScreen = new ArrayList<>();
		screenScales = new ScreenScales( screenScaleFactors, targetRenderNanos );
		renderStorage = new RenderStorage();

		renderNanosPerPixelAndSource = new MovingAverage( 3 );
		renderNanosPerPixelAndSource.init( 500 );

		requestedScreenScaleIndex = screenScales.size() - 1;
		renderingMayBeCancelled = false;
		this.cacheControl = cacheControl;
		newFrameRequest = false;

		intervalResult = display.createRenderResult();

		projectorFactory = new ProjectorFactory(
				numRenderingThreads,
				renderingExecutorService,
				useVolatileIfAvailable,
				accumulateProjectorFactory );
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint} has completed).
	 */
	public synchronized void requestRepaint()
	{
		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		newFrameRequest = true;
		painterThread.requestRepaint();
	}

	/**
	 * Request a repaint of the given {@code interval} of the display from the
	 * painter thread. The painter thread will trigger a {@link #paint} as soon
	 * as possible (that is, immediately or after the currently running
	 * {@link #paint} has completed).
	 */
	public synchronized void requestRepaint( final Interval interval )
	{
		if ( !intervalMode && !renderingMayBeCancelled )
		{
			/*
			 * We are currently rendering a full frame at the coarsest
			 * resolution. There is no point in painting an interval now. Just
			 * request a new full frame.
			 */
			newFrameRequest = true;
		}
		else
		{
			if ( renderingMayBeCancelled && projector != null )
				projector.cancel();
			screenScales.requestInterval( interval );
			newIntervalRequest = true;
		}
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
		projector = null;
		currentViewerState = null;
		currentRenderResult = null;
		currentVisibleSourcesOnScreen.clear();
		renderStorage.clear();
	}

	/**
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale}.
	 */
	public boolean paint( final ViewerState viewerState )
	{
		final int screenW = display.getWidth();
		final int screenH = display.getHeight();
		if ( screenW <= 0 || screenH <= 0 )
			return false;

		final boolean newFrame;
		final boolean newInterval;
		final boolean prepareNextFrame;
		final boolean createProjector;
		synchronized ( this )
		{
			final boolean resized = screenScales.checkResize( screenW, screenH );

			newFrame = newFrameRequest || resized;
			if ( newFrame )
			{
				intervalMode = false;
				screenScales.clearRequestedIntervals();
			}

			newInterval = newIntervalRequest && !newFrame;
			if ( newInterval )
			{
				intervalMode = true;
				final int numSources = currentVisibleSourcesOnScreen.size();
				final double renderNanosPerPixel = renderNanosPerPixelAndSource.getAverage() * numSources;
				requestedIntervalScaleIndex = screenScales.suggestIntervalScreenScale( renderNanosPerPixel, currentScreenScaleIndex );
			}

			prepareNextFrame = newFrame || newInterval;
			renderingMayBeCancelled = !prepareNextFrame;

			if ( intervalMode )
			{
				createProjector = newInterval || ( requestedIntervalScaleIndex != currentIntervalScaleIndex );
				if ( createProjector )
					intervalRenderData = screenScales.pullIntervalRenderData( requestedIntervalScaleIndex, currentScreenScaleIndex );
			}
			else
				createProjector = newFrame || ( requestedScreenScaleIndex != currentScreenScaleIndex );

			newFrameRequest = false;
			newIntervalRequest = false;
		}

		if ( prepareNextFrame )
			cacheControl.prepareNextFrame();

		if ( newFrame )
		{
			currentViewerState = viewerState.snapshot();
			VisibilityUtils.computeVisibleSourcesOnScreen( currentViewerState, screenScales.get( 0 ), currentVisibleSourcesOnScreen );
			final int numSources = currentVisibleSourcesOnScreen.size();
			final double renderNanosPerPixel = renderNanosPerPixelAndSource.getAverage() * numSources;
			requestedScreenScaleIndex = screenScales.suggestScreenScale( renderNanosPerPixel );
		}

		if ( !intervalMode && requestedScreenScaleIndex < 0 )
			return true;

		return intervalMode
				? paintInterval( createProjector )
				: paintFullFrame( createProjector );
	}

	private boolean paintFullFrame( final boolean createProjector )
	{
		// the projector that paints to the screenImage.
		final VolatileProjector p;

		// holds new RenderResult, in case that a new projector is created in full frame mode
		RenderResult renderResult = null;

		// whether to request a newFrame, in case that a new projector is created in full frame mode
		boolean requestNewFrameIfIncomplete = false;

		synchronized ( this )
		{
			if ( createProjector )
			{
				final ScreenScale screenScale = screenScales.get( requestedScreenScaleIndex );

				renderResult = display.getReusableRenderResult();
				renderResult.init( screenScale.width(), screenScale.height() );
				renderResult.setScaleFactor( screenScale.scale() );
				currentViewerState.getViewerTransform( renderResult.getViewerTransform() );

				renderStorage.checkRenewData( screenScales.get( 0 ).width(), screenScales.get( 0 ).height(), currentVisibleSourcesOnScreen.size() );
				projector = createProjector( currentViewerState, currentVisibleSourcesOnScreen, requestedScreenScaleIndex, renderResult.getTargetImage(), 0, 0 );
				requestNewFrameIfIncomplete = projectorFactory.requestNewFrameIfIncomplete();
			}
			p = projector;
		}

		// try rendering
		final boolean success = p.map( createProjector );
		final long rendertime = p.getLastFrameRenderNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				currentScreenScaleIndex = requestedScreenScaleIndex;
				if ( createProjector )
				{
					renderResult.setUpdated();
					( ( RenderTarget ) display ).setRenderResult( renderResult );
					currentRenderResult = renderResult;
					recordRenderTime( renderResult, rendertime );
				}
				else
					currentRenderResult.setUpdated();

				if ( !p.isValid() && requestNewFrameIfIncomplete )
					requestRepaint();
				else if ( p.isValid() && currentScreenScaleIndex == 0 )
					// indicate that rendering is complete
					requestedScreenScaleIndex = -1;
				else
					iterateRepaint( Math.max( 0, currentScreenScaleIndex - 1 ) );
			}
		}

		return success;
	}

	private boolean paintInterval( final boolean createProjector )
	{
		// the projector that paints to the screenImage.
		final VolatileProjector p;

		synchronized ( this )
		{
			if ( createProjector )
			{
				intervalResult.init( intervalRenderData.width(), intervalRenderData.height() );
				intervalResult.setScaleFactor( intervalRenderData.scale() );
				projector = createProjector( currentViewerState, currentVisibleSourcesOnScreen, requestedIntervalScaleIndex, intervalResult.getTargetImage(), intervalRenderData.offsetX(), intervalRenderData.offsetY() );
			}
			p = projector;
		}

		// try rendering
		final boolean success = p.map( createProjector );
		final long rendertime = p.getLastFrameRenderNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				currentIntervalScaleIndex = requestedIntervalScaleIndex;
				currentRenderResult.patch( intervalResult, intervalRenderData.targetInterval(), intervalRenderData.tx(), intervalRenderData.ty() );

				if ( createProjector )
					recordRenderTime( intervalResult, rendertime );

				if ( currentIntervalScaleIndex > currentScreenScaleIndex )
					iterateRepaintInterval( currentIntervalScaleIndex - 1 );
				else if ( p.isValid() )
				{
					// if full frame rendering was not yet complete
					if ( requestedScreenScaleIndex >= 0 )
					{
						// go back to full frame rendering
						intervalMode = false;
						if ( requestedScreenScaleIndex == currentScreenScaleIndex )
							++currentScreenScaleIndex;
						painterThread.requestRepaint();
					}
				}
				else
					iterateRepaintInterval( currentIntervalScaleIndex );
			}
			// if rendering was cancelled...
			else
				intervalRenderData.reRequest();
		}

		return success;
	}

	private void recordRenderTime( final RenderResult result, final long renderNanos )
	{
		final int numSources = currentVisibleSourcesOnScreen.size();
		final int numRenderPixels = ( int ) Intervals.numElements( result.getTargetImage() ) * numSources;
		if ( numRenderPixels >= 4096 )
			renderNanosPerPixelAndSource.add( renderNanos / ( double ) numRenderPixels );
	}

	/**
	 * Request iterated repaint at the specified {@code screenScaleIndex}. This
	 * is used to repaint the {@code currentViewerState} in a loop, until
	 * everything is painted at highest resolution from valid data (or until
	 * painting is interrupted by a new request}.
	 */
	private void iterateRepaint( final int screenScaleIndex )
	{
		if ( screenScaleIndex == currentScreenScaleIndex )
			usleep();
		requestedScreenScaleIndex = screenScaleIndex;
		painterThread.requestRepaint();
	}

	/**
	 * Request iterated repaint at the specified {@code intervalScaleIndex}.
	 * This is used to repaint the current interval in a loop, until everything
	 * is painted at highest resolution from valid data (or until painting is
	 * interrupted by a new request}.
	 */
	private void iterateRepaintInterval( final int intervalScaleIndex )
	{
		if ( intervalScaleIndex == currentIntervalScaleIndex )
		{
			intervalRenderData.reRequest();
			usleep();
		}
		requestedIntervalScaleIndex = intervalScaleIndex;
		painterThread.requestRepaint();
	}

	/**
	 * Wait for 1ms so that fetcher threads get a chance to do work.
	 */
	private void usleep()
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
	}

	private VolatileProjector createProjector(
			final ViewerState viewerState,
			final List< SourceAndConverter< ? > > visibleSourcesOnScreen,
			final int screenScaleIndex,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final int offsetX,
			final int offsetY )
	{
		final AffineTransform3D screenScaleTransform = screenScales.get( screenScaleIndex ).scaleTransform();
		final AffineTransform3D screenTransform = viewerState.getViewerTransform();
		screenTransform.preConcatenate( screenScaleTransform );
		screenTransform.translate( -offsetX, -offsetY, 0 );

		final VolatileProjector projector = projectorFactory.createProjector(
				viewerState,
				visibleSourcesOnScreen,
				screenImage,
				screenTransform,
				renderStorage );
		CacheIoTiming.getIoTimeBudget().reset( iobudget );
		return projector;
	}
}
