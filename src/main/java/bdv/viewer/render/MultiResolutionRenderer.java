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

import bdv.cache.CacheControl;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.MipmapTransforms;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import bdv.viewer.render.MipmapOrdering.Level;
import bdv.viewer.render.MipmapOrdering.MipmapHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.cache.iotiming.CacheIoTiming;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;
import net.imglib2.util.Fraction;

/**
 * A renderer that uses a coarse-to-fine rendering scheme. First, a
 * small target image at a fraction of the canvas resolution is
 * rendered. Then, increasingly larger images are rendered, until the full
 * canvas resolution is reached.
 * <p>
 * When drawing the low-resolution target images to the screen, they
 * will be scaled up by Java2D (or JavaFX, etc) to the full canvas size, which is relatively
 * fast. Rendering the small, low-resolution images is usually very fast, such
 * that the display is very interactive while the user changes the viewing
 * transformation for example. When the transformation remains fixed for a
 * longer period, higher-resolution details are filled in successively.
 * <p>
 * The renderer allocates a {@code BufferedImage} for each of a predefined set
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
 * Double buffering means that three {@code BufferedImage BufferedImages} are
 * created for every screen scale. After rendering the first one of them and
 * setting it to the {@link RenderTarget}, next time, rendering goes to the
 * second one, then to the third. The {@link RenderTarget} will always have a
 * complete image, which is not rendered to while it is potentially drawn to the
 * screen. When setting an image to the {@link RenderTarget}, the
 * {@link RenderTarget} will release one of the previously set images to be
 * rendered again. Thus, rendering will not interfere with painting the
 * {@code BufferedImage} to the canvas.
 * <p>
 * The renderer supports rendering of {@link Volatile} sources. In each
 * rendering pass, all currently valid data for the best fitting mipmap level
 * and all coarser levels is rendered to a {@link #renderImages temporary image}
 * for each visible source. Then the temporary images are combined to the final
 * image for display. The number of passes required until all data is valid
 * might differ between visible sources.
 * <p>
 * Rendering timing is tied to a {@link CacheControl} control for IO budgeting, etc.
 *
 * @author Tobias Pietzsch
 */
public class MultiResolutionRenderer
{
	static class RenderImage extends ArrayImg< ARGBType, IntArray >
	{
		final private int[] data;

		public RenderImage( final int width, final int height )
		{
			this( width, height, new int[ width * height ] );
		}

		/**
		 * Create an image with {@code data}. Writing to the {@code data}
		 * array will update the image.
		 */
		public RenderImage( final int width, final int height, final int[] data )
		{
			super( new IntArray( data ), new long[]{ width, height }, new Fraction() );
			setLinkedType( new ARGBType( this ) );
			this.data = data;
		}

		/**
		 * The underlying array holding the data.
		 */
		public int[] getData()
		{
			return data;
		}
	}

	/**
	 * Receiver for the {@code BufferedImage BufferedImages} that we render.
	 */
	private final RenderTarget display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	private final PainterThread painterThread;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * source data to {@code ©screenImages}.
	 */
	private VolatileProjector projector;

	/**
	 * The index of the screen scale of the {@link #projector current projector}.
	 */
	private int currentScreenScaleIndex;

	/**
	 * Used to render an individual source. One image per screen resolution and
	 * visible source. First index is screen scale, second index is index in
	 * list of visible sources.
	 */
	private RenderImage[][] renderImages;

	/**
	 * Storage for mask images of {@link VolatileHierarchyProjector}.
	 * One array per visible source. (First) index is index in list of visible sources.
	 */
	private byte[][] renderMaskArrays;

	private final int[] screenW;

	private final int[] screenH;

	/**
	 * Scale factors from the {@link #display viewer canvas} to the
	 * {@code screenImages}.
	 *
	 * A scale factor of 1 means 1 pixel in the screen image is displayed as 1
	 * pixel on the canvas, a scale factor of 0.5 means 1 pixel in the screen
	 * image is displayed as 2 pixel on the canvas, etc.
	 */
	private final double[] screenScales;

	/**
	 * The scale transformation from viewer to screen image.
	 * Each transformations corresponds to a {@link #screenScales screen
	 * scale}.
	 */
	private AffineTransform3D[] screenScaleTransforms;

	/**
	 * If the rendering time (in nanoseconds) for the (currently) highest scaled
	 * screen image is above this threshold, increase the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 * Similarly, if the rendering time for the (currently) second-highest
	 * scaled screen image is below this threshold, decrease the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 */
	private final long targetRenderNanos;

	/**
	 * The index of the (coarsest) screen scale with which to start rendering.
	 * Once this level is painted, rendering proceeds to lower screen scales
	 * until index 0 (full resolution) has been reached. While rendering, the
	 * maxScreenScaleIndex is adapted such that it is the highest index for
	 * which rendering in {@link #targetRenderNanos} nanoseconds is still
	 * possible.
	 */
	private int maxScreenScaleIndex;

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
	 * How many threads to use for rendering.
	 */
	private final int numRenderingThreads;

	/**
	 * {@link ExecutorService} used for rendering.
	 */
	private final ExecutorService renderingExecutorService;

	/**
	 * TODO
	 */
	private final AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory;

	/**
	 * Controls IO budgeting and fetcher queue.
	 */
	private final CacheControl cacheControl;

	/**
	 * Whether volatile versions of sources should be used if available.
	 */
	private final boolean useVolatileIfAvailable;

	/**
	 * Whether a repaint was {@link #requestRepaint() requested}. This will
	 * cause {@link CacheControl#prepareNextFrame()}.
	 */
	private boolean newFrameRequest;

	/**
	 * The timepoint for which last a projector was
	 * {@link #createProjector(ViewerState, RandomAccessibleInterval) created}.
	 */
	private int previousTimepoint;

	// TODO: should be settable
	private long[] iobudget = new long[] { 100l * 1000000l,  10l * 1000000l };

	// TODO: should be settable
	private boolean prefetchCells = true;

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
	 * @param doubleBuffered
	 *            Whether to use double buffered rendering.
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
			final RenderTarget display,
			final PainterThread painterThread,
			final double[] screenScales,
			final long targetRenderNanos,
			final boolean doubleBuffered, // TODO: remove
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
		this.screenScales = screenScales.clone();
		renderImages = new RenderImage[ screenScales.length ][ 0 ];
		renderMaskArrays = new byte[ 0 ][];
		screenW = new int[ screenScales.length ];
		screenH = new int[ screenScales.length ];
		screenScaleTransforms = new AffineTransform3D[ screenScales.length ];

		this.targetRenderNanos = targetRenderNanos;

		maxScreenScaleIndex = screenScales.length - 1;
		requestedScreenScaleIndex = maxScreenScaleIndex;
		renderingMayBeCancelled = true;
		this.numRenderingThreads = numRenderingThreads;
		this.renderingExecutorService = renderingExecutorService;
		this.useVolatileIfAvailable = useVolatileIfAvailable;
		this.accumulateProjectorFactory = accumulateProjectorFactory;
		this.cacheControl = cacheControl;
		newFrameRequest = false;
		previousTimepoint = -1;
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@code screenImages} and {@link #screenScaleTransforms} accordingly.
	 *
	 * @return whether the size was changed.
	 */
	private synchronized boolean checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		final int newTargetW = ( int ) Math.ceil( componentW * screenScales[ 0 ] );
		final int newTargetH = ( int ) Math.ceil( componentH * screenScales[ 0 ] );
		if ( newTargetW != screenW[ 0 ] || newTargetH != screenH[ 0 ] )
		{
			for ( int i = 0; i < screenScales.length; ++i )
			{
				final double screenToViewerScale = screenScales[ i ];
				screenW[ i ] = ( int ) Math.ceil( screenToViewerScale * componentW );
				screenH[ i ] = ( int ) Math.ceil( screenToViewerScale * componentH );
				final AffineTransform3D scale = new AffineTransform3D();
				scale.set( screenToViewerScale, 0, 0 );
				scale.set( screenToViewerScale, 1, 1 );
				scale.set( 0.5 * screenToViewerScale - 0.5, 0, 3 );
				scale.set( 0.5 * screenToViewerScale - 0.5, 1, 3 );
				screenScaleTransforms[ i ] = scale;
			}

			return true;
		}
		return false;
	}

	private boolean checkRenewRenderImages( final int numVisibleSources )
	{
		final int n = numVisibleSources > 1 ? numVisibleSources : 0;
		if ( n != renderImages[ 0 ].length ||
				( n != 0 &&
					( renderImages[ 0 ][ 0 ].dimension( 0 ) != screenW[ 0 ] ||
					  renderImages[ 0 ][ 0 ].dimension( 1 ) != screenH[ 0 ] ) ) )
		{
			renderImages = new RenderImage[ screenScales.length ][ n ];
			for ( int i = 0; i < screenScales.length; ++i )
			{
				for ( int j = 0; j < n; ++j )
				{
					renderImages[ i ][ j ] = ( i == 0 ) ?
						new RenderImage( screenW[ i ], screenH[ i ] ) :
						new RenderImage( screenW[ i ], screenH[ i ], renderImages[ 0 ][ j ].getData() );
				}
			}
			return true;
		}
		return false;
	}

	private boolean checkRenewMaskArrays( final int numVisibleSources )
	{
		final int size = screenW[ 0 ] * screenH[ 0 ];
		if ( numVisibleSources != renderMaskArrays.length ||
				( numVisibleSources != 0 &&	( renderMaskArrays[ 0 ].length < size ) ) )
		{
			renderMaskArrays = new byte[ numVisibleSources ][];
			for ( int j = 0; j < numVisibleSources; ++j )
				renderMaskArrays[ j ] = new byte[ size ];
			return true;
		}
		return false;
	}

	private final AffineTransform3D currentProjectorTransform = new AffineTransform3D();

	/**
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale}.
	 */
	public boolean paint( final ViewerState viewerState )
	{
		if ( display.getWidth() <= 0 || display.getHeight() <= 0 )
			return false;

		final boolean resized = checkResize();

		final RenderResult renderResult;

		final boolean createProjector;

		synchronized ( this )
		{
			// Rendering may be cancelled unless we are rendering at coarsest
			// screen scale and coarsest mipmap level.
			renderingMayBeCancelled = ( requestedScreenScaleIndex < maxScreenScaleIndex );

			if ( newFrameRequest )
				cacheControl.prepareNextFrame();
			createProjector = newFrameRequest || resized || ( requestedScreenScaleIndex != currentScreenScaleIndex );
			newFrameRequest = false;

			if ( createProjector )
			{
				currentScreenScaleIndex = requestedScreenScaleIndex;

				renderResult = display.getReusableRenderResult();
				renderResult.init( screenW[ currentScreenScaleIndex ], screenH[ currentScreenScaleIndex ] );
			}
			else
			{
				renderResult = null;
			}

			requestedScreenScaleIndex = 0;
		}

		// the projector that paints to the screenImage.
		final VolatileProjector p;

		if ( createProjector )
		{
			synchronized ( viewerState )
			{
				final int numVisibleSources = viewerState.getVisibleAndPresentSources().size();
				checkRenewRenderImages( numVisibleSources );
				checkRenewMaskArrays( numVisibleSources );
				p = createProjector( viewerState, renderResult.getScreenImage() );
			}
			synchronized ( this )
			{
				projector = p;
			}
		}
		else
		{
			synchronized ( this )
			{
				p = projector;
			}
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
					renderResult.getViewerTransform().set( currentProjectorTransform );
					renderResult.setScaleFactor( screenScales[ currentScreenScaleIndex ] );
					display.setRenderResult( renderResult );

					if ( currentScreenScaleIndex == maxScreenScaleIndex )
					{
						if ( rendertime > targetRenderNanos && maxScreenScaleIndex < screenScales.length - 1 )
							maxScreenScaleIndex++;
						else if ( rendertime < targetRenderNanos / 3 && maxScreenScaleIndex > 0 )
							maxScreenScaleIndex--;
					}
					else if ( currentScreenScaleIndex == maxScreenScaleIndex - 1 )
					{
						if ( rendertime < targetRenderNanos && maxScreenScaleIndex > 0 )
							maxScreenScaleIndex--;
					}
//					System.out.println( String.format( "rendering:%4d ms", rendertime / 1000000 ) );
//					System.out.println( "scale = " + currentScreenScaleIndex );
//					System.out.println( "maxScreenScaleIndex = " + maxScreenScaleIndex + "  (" + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 0 ) + " x " + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 1 ) + ")" );
				}

				if ( currentScreenScaleIndex > 0 )
					requestRepaint( currentScreenScaleIndex - 1 );
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
					requestRepaint( currentScreenScaleIndex );
				}
			}
		}

		return success;
	}

	/**
	 * Request a repaint of the display from the painter thread, with maximum
	 * screen scale index and mipmap level.
	 */
	public synchronized void requestRepaint()
	{
		newFrameRequest = true;
		requestRepaint( maxScreenScaleIndex );
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint} has
	 * completed).
	 */
	public synchronized void requestRepaint( final int screenScaleIndex )
	{
		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		if ( screenScaleIndex > requestedScreenScaleIndex )
			requestedScreenScaleIndex = screenScaleIndex;
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
		if ( display instanceof BufferedImageOverlayRenderer )
			( ( BufferedImageOverlayRenderer ) display ).kill();
		projector = null;
		Arrays.fill( renderImages, null );
		Arrays.fill( renderMaskArrays, null );
	}

	private VolatileProjector createProjector(
			final ViewerState viewerState,
			final RandomAccessibleInterval< ARGBType > screenImage )
	{
		/*
		 * This shouldn't be necessary, with
		 * CacheHints.LoadingStrategy==VOLATILE
		 */
//		CacheIoTiming.getIoTimeBudget().clear(); // clear time budget such that prefetching doesn't wait for loading blocks.
		final ArrayList< SourceAndConverter< ? > > visibleSources = new ArrayList<>( viewerState.getVisibleAndPresentSources() );
		visibleSources.sort( viewerState.sourceOrder() );
		VolatileProjector projector;
		if ( visibleSources.isEmpty() )
			projector = new EmptyProjector<>( screenImage );
		else if ( visibleSources.size() == 1 )
		{
			projector = createSingleSourceProjector( viewerState, visibleSources.get( 0 ), screenImage, renderMaskArrays[ 0 ] );
		}
		else
		{
			final ArrayList< VolatileProjector > sourceProjectors = new ArrayList<>();
			final ArrayList< RenderImage > sourceImages = new ArrayList<>();
			int j = 0;
			for ( SourceAndConverter< ? > source : visibleSources )
			{
				final RenderImage renderImage = renderImages[ currentScreenScaleIndex ][ j ];
				final byte[] maskArray = renderMaskArrays[ j ];
				++j;
				final VolatileProjector p = createSingleSourceProjector(
						viewerState, source,
						renderImage, maskArray );
				sourceProjectors.add( p );
				sourceImages.add( renderImage );
			}
			projector = accumulateProjectorFactory.createProjector( sourceProjectors, visibleSources, sourceImages, screenImage, numRenderingThreads, renderingExecutorService );
		}
		previousTimepoint = viewerState.getCurrentTimepoint();
		viewerState.getViewerTransform( currentProjectorTransform );
		CacheIoTiming.getIoTimeBudget().reset( iobudget );
		return projector;
	}

	private < T > VolatileProjector createSingleSourceProjector(
			final ViewerState viewerState,
			final SourceAndConverter< T > source,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final byte[] maskArray )
	{
		if ( useVolatileIfAvailable )
		{
			if ( source.asVolatile() != null )
				return createSingleSourceVolatileProjector( viewerState, source.asVolatile(), screenImage, maskArray );
			else if ( source.getSpimSource().getType() instanceof Volatile )
			{
				@SuppressWarnings( "unchecked" )
				final SourceAndConverter< ? extends Volatile< ? > > vsource = ( SourceAndConverter< ? extends Volatile< ? > > ) source;
				return createSingleSourceVolatileProjector( viewerState, vsource, screenImage, maskArray );
			}
		}

		final AffineTransform3D screenScaleTransform = screenScaleTransforms[ currentScreenScaleIndex ];
		final int bestLevel = getBestMipMapLevel( viewerState, source, screenScaleTransform );
		return new SimpleVolatileProjector<>(
				getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, bestLevel, null ),
				source.getConverter(), screenImage, numRenderingThreads, renderingExecutorService );
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 * 		screen scale, transforms screen coordinates to viewer coordinates.
	 *
	 * @return mipmap level
	 */
	private int getBestMipMapLevel(
			final ViewerState viewerState,
			final SourceAndConverter< ? > source,
			final AffineTransform3D screenScaleTransform )
	{
			final AffineTransform3D screenTransform = viewerState.getViewerTransform();
			screenTransform.preConcatenate( screenScaleTransform ); // TODO: REUSE? screenTransform is the same for every source...
			return MipmapTransforms.getBestMipMapLevel( screenTransform, source.getSpimSource(), viewerState.getCurrentTimepoint() );
	}

	private < T extends Volatile< ? > > VolatileProjector createSingleSourceVolatileProjector(
			final ViewerState viewerState,
			final SourceAndConverter< T > source,
			final RandomAccessibleInterval< ARGBType > screenImage,
			final byte[] maskArray )
	{
		final AffineTransform3D screenScaleTransform = screenScaleTransforms[ currentScreenScaleIndex ];
		final ArrayList< RandomAccessible< T > > renderList = new ArrayList<>();
		final Source< T > spimSource = source.getSpimSource();
		final int t = viewerState.getCurrentTimepoint();

		final MipmapOrdering ordering = spimSource instanceof MipmapOrdering ?
			( MipmapOrdering ) spimSource : new DefaultMipmapOrdering( spimSource );

		final AffineTransform3D screenTransform = viewerState.getViewerTransform();
		screenTransform.preConcatenate( screenScaleTransform ); // TODO: REUSE? screenTransform is the same for every source...
		final MipmapHints hints = ordering.getMipmapHints( screenTransform, t, previousTimepoint );
		final List< Level > levels = hints.getLevels();

		if ( prefetchCells )
		{
			Collections.sort( levels, MipmapOrdering.prefetchOrderComparator );
			for ( final Level l : levels )
			{
				final CacheHints cacheHints = l.getPrefetchCacheHints();
				if ( cacheHints == null || cacheHints.getLoadingStrategy() != LoadingStrategy.DONTLOAD )
					prefetch( viewerState, spimSource, screenScaleTransform, l.getMipmapLevel(), cacheHints, screenImage );
			}
		}

		Collections.sort( levels, MipmapOrdering.renderOrderComparator );
		for ( final Level l : levels )
			renderList.add( getTransformedSource( viewerState, spimSource, screenScaleTransform, l.getMipmapLevel(), l.getRenderCacheHints() ) );

		if ( hints.renewHintsAfterPaintingOnce() )
			newFrameRequest = true;

		return new VolatileHierarchyProjector<>( renderList, source.getConverter(), screenImage, maskArray, numRenderingThreads, renderingExecutorService );
	}

	private static < T > RandomAccessible< T > getTransformedSource(
			final ViewerState viewerState,
			final Source< T > source,
			final AffineTransform3D screenScaleTransform,
			final int mipmapIndex,
			final CacheHints cacheHints )
	{
		final int timepoint = viewerState.getCurrentTimepoint();

		final RandomAccessibleInterval< T > img = source.getSource( timepoint, mipmapIndex );
		if ( img instanceof VolatileCachedCellImg )
			( ( VolatileCachedCellImg< ?, ? > ) img ).setCacheHints( cacheHints );

		final Interpolation interpolation = viewerState.getInterpolation();
		final RealRandomAccessible< T > ipimg = source.getInterpolatedSource( timepoint, mipmapIndex, interpolation );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
		sourceToScreen.concatenate( sourceTransform );
		sourceToScreen.preConcatenate( screenScaleTransform );

		return RealViews.affine( ipimg, sourceToScreen );
	}

	private static < T > void prefetch(
			final ViewerState viewerState,
			final Source< T > source,
			final AffineTransform3D screenScaleTransform,
			final int mipmapIndex,
			final CacheHints prefetchCacheHints,
			final Dimensions screenInterval )
	{
		final int timepoint = viewerState.getCurrentTimepoint();
		final RandomAccessibleInterval< T > img = source.getSource( timepoint, mipmapIndex );
		if ( img instanceof VolatileCachedCellImg )
		{
			final VolatileCachedCellImg< ?, ? > cellImg = ( VolatileCachedCellImg< ?, ? > ) img;

			CacheHints hints = prefetchCacheHints;
			if ( hints == null )
			{
				final CacheHints d = cellImg.getDefaultCacheHints();
				hints = new CacheHints( LoadingStrategy.VOLATILE, d.getQueuePriority(), false );
			}
			cellImg.setCacheHints( hints );
			final int[] cellDimensions = new int[ 3 ];
			cellImg.getCellGrid().cellDimensions( cellDimensions );
			final long[] dimensions = new long[ 3 ];
			cellImg.dimensions( dimensions );
			final RandomAccess< ? > cellsRandomAccess = cellImg.getCells().randomAccess();

			final Interpolation interpolation = viewerState.getInterpolation();

			final AffineTransform3D sourceToScreen = new AffineTransform3D();
			viewerState.getViewerTransform( sourceToScreen );
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			source.getSourceTransform( timepoint, mipmapIndex, sourceTransform );
			sourceToScreen.concatenate( sourceTransform );
			sourceToScreen.preConcatenate( screenScaleTransform );

			Prefetcher.fetchCells( sourceToScreen, cellDimensions, dimensions, screenInterval, interpolation, cellsRandomAccess );
		}
	}
}
