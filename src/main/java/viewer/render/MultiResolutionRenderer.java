package viewer.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.InterruptibleProjector;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.util.GuiUtil;
import viewer.hdf5.img.Hdf5GlobalCellCache;

public class MultiResolutionRenderer
{
	/**
	 * Receiver for the {@link BufferedImage BufferedImages} that we render.
	 */
	final protected RenderTarget display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	final protected PainterThread painterThread;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected InterruptibleProjector projector;

	/**
	 * The index of the screen scale of the {@link #projector current projector}.
	 */
	protected int currentScreenScaleIndex;

	/**
	 * Whether double buffering is used.
	 */
	final protected boolean doubleBuffered;

	/**
	 * Used to render the image for display. Two images per screen resolution
	 * if double buffering is enabled. First index is screen scale, second index is
	 * double-buffer.
	 */
	protected ARGBScreenImage[][] screenImages;

	/**
	 * {@link BufferedImage}s wrapping the data in the {@link #screenImages}.
	 * First index is screen scale, second index is double-buffer.
	 */
	protected BufferedImage[][] bufferedImages;

	/**
	 * Scale factors from the {@link #display viewer canvas} to the
	 * {@link #screenImages}.
	 *
	 * A scale factor of 1 means 1 pixel in the screen image is displayed as 1
	 * pixel on the canvas, a scale factor of 0.5 means 1 pixel in the screen
	 * image is displayed as 2 pixel on the canvas, etc.
	 */
	final protected double[] screenScales;

	/**
	 * The scale transformation from viewer to {@link #screenImages screen
	 * image}. Each transformations corresponds to a {@link #screenScales screen
	 * scale}.
	 */
	protected AffineTransform3D[] screenScaleTransforms;

	/**
	 * If the rendering time (in nanoseconds) for the (currently) highest scaled
	 * screen image is above this threshold, increase the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 * Similarly, if the rendering time for the (currently) second-highest
	 * scaled screen image is below this threshold, decrease the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 */
	final protected long targetRenderNanos;

	/**
	 * The index of the (coarsest) screen scale with which to start rendering.
	 * Once this level is painted, rendering proceeds to lower screen scales
	 * until index 0 (full resolution) has been reached. While rendering, the
	 * maxScreenScaleIndex is adapted such that it is the highest index for
	 * which rendering in {@link #targetRenderNanos} nanoseconds is still
	 * possible.
	 */
	protected int maxScreenScaleIndex;

	/**
	 * The index of the screen scale which should be rendered next.
	 */
	protected int requestedScreenScaleIndex;

	/**
	 * Whether the current rendering operation may be cancelled (to start a
	 * new one). Rendering may be cancelled unless we are rendering at
	 * coarsest screen scale and coarsest mipmap level.
	 */
	protected volatile boolean renderingMayBeCancelled;

	/**
	 * How many threads to use for rendering.
	 */
	final protected int numRenderingThreads;

	final protected Hdf5GlobalCellCache< ? > cache;

	protected boolean newFrameRequest;

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
	 */
	public MultiResolutionRenderer( final RenderTarget display, final PainterThread painterThread, final double[] screenScales, final long targetRenderNanos, final boolean doubleBuffered, final int numRenderingThreads, final Hdf5GlobalCellCache< ? > cache )
	{
		this.display = display;
		this.painterThread = painterThread;
		projector = null;
		currentScreenScaleIndex = -1;
		this.screenScales = screenScales.clone();
		this.doubleBuffered = doubleBuffered;
		screenImages = new ARGBScreenImage[ screenScales.length ][ 2 ];
		bufferedImages = new BufferedImage[ screenScales.length ][ 2 ];
		screenScaleTransforms = new AffineTransform3D[ screenScales.length ];

		this.targetRenderNanos = targetRenderNanos;

		maxScreenScaleIndex = screenScales.length - 1;
		requestedScreenScaleIndex = maxScreenScaleIndex;
		renderingMayBeCancelled = true;
		this.numRenderingThreads = numRenderingThreads;
		this.cache = cache;
		newFrameRequest = false;
	}

	public MultiResolutionRenderer( final RenderTarget display, final PainterThread painterThread, final double[] screenScales, final Hdf5GlobalCellCache< ? > cache )
	{
		this( display, painterThread, screenScales, 30 * 1000000, true, 3, cache );
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages} and {@link #screenScaleTransforms} accordingly.
	 *
	 * @return whether the size was changed.
	 */
	protected synchronized boolean checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( screenImages[ 0 ][ 0 ] == null || screenImages[ 0 ][ 0 ].dimension( 0 ) * screenScales[ 0 ] != componentW || screenImages[ 0 ][ 0 ].dimension( 1 )  * screenScales[ 0 ] != componentH )
		{
			for ( int i = 0; i < screenScales.length; ++i )
			{
				final double screenToViewerScale = screenScales[ i ];
				final int w = ( int ) ( screenToViewerScale * componentW );
				final int h = ( int ) ( screenToViewerScale * componentH );
				for ( int b = 0; b < ( doubleBuffered ? 2 : 1 ); ++b )
				{
					screenImages[ i ][ b ] = new ARGBScreenImage( w, h );
					bufferedImages[ i ][ b ] = GuiUtil.getBufferedImage( screenImages[ i ][ b ] );
				}
				final AffineTransform3D scale = new AffineTransform3D();
				final double xScale = ( double ) w / componentW;
				final double yScale = ( double ) h / componentH;
				scale.set( xScale, 0, 0 );
				scale.set( yScale, 1, 1 );
				scale.set( 0.5 * xScale - 0.5, 0, 3 );
				scale.set( 0.5 * yScale - 0.5, 1, 3 );
				screenScaleTransforms[ i ] = scale;
			}

			return true;
		}
		return false;
	}

	/**
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale} and the {@link #requestedMipmapLevel requested mipmap level}.
	 */
	public boolean paint( final ViewerState state )
	{
		final boolean resized = checkResize();

		// the corresponding ARGBScreenImage (to render to)
		final ARGBScreenImage screenImage;

		// the corresponding BufferedImage (to paint to the canvas)
		final BufferedImage bufferedImage;

		// the projector that paints to the screenImage.
		final VolatileHierarchyProjector< ?, ? > p;

		final boolean clearQueue;

		final boolean createProjector;

		synchronized( this )
		{
			// Rendering may be cancelled unless we are rendering at coarsest
			// screen scale and coarsest mipmap level.
			renderingMayBeCancelled = ( requestedScreenScaleIndex < maxScreenScaleIndex );

			clearQueue = newFrameRequest;

			createProjector = newFrameRequest || resized || ( requestedScreenScaleIndex != currentScreenScaleIndex );

			if ( createProjector )
			{
				currentScreenScaleIndex = requestedScreenScaleIndex;
				screenImage = screenImages[ currentScreenScaleIndex ][ 0 ];
				bufferedImage = bufferedImages[ currentScreenScaleIndex ][ 0 ];
				p = createProjector( state, screenScaleTransforms[ currentScreenScaleIndex ], screenImage );
				projector = p;
			}
			else
			{
				screenImage = null;
				bufferedImage = null;
				p = ( VolatileHierarchyProjector< ?, ? > ) projector;
			}

			newFrameRequest = false;
		}

//		System.out.println( createProjector ? "+++ createProjector" : "--- don't createProjector" );

		// try rendering
		if ( clearQueue )
			cache.clearQueue();
		final boolean success = p.map();
		if ( createProjector )
			p.clearUntouchedTargetPixels();
		final long rendertime = p.getLastFrameRenderNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				if ( createProjector )
				{
					display.setBufferedImage( bufferedImage );
					if ( doubleBuffered )
					{
						screenImages[ currentScreenScaleIndex ][ 0 ] = screenImages[ currentScreenScaleIndex ][ 1 ];
						screenImages[ currentScreenScaleIndex ][ 1 ] = screenImage;
						bufferedImages[ currentScreenScaleIndex ][ 0 ] = bufferedImages[ currentScreenScaleIndex ][ 1 ];
						bufferedImages[ currentScreenScaleIndex ][ 1 ] = bufferedImage;
					}
				}

				if ( currentScreenScaleIndex == maxScreenScaleIndex )
				{
					if ( rendertime > targetRenderNanos && maxScreenScaleIndex < screenScales.length - 1 )
						maxScreenScaleIndex++;
				}
				else if ( currentScreenScaleIndex == maxScreenScaleIndex - 1 )
				{
					if ( rendertime < targetRenderNanos && maxScreenScaleIndex > 0 )
						maxScreenScaleIndex--;
				}
				System.out.println( "maxScreenScaleIndex = " + maxScreenScaleIndex + "  (" + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 0 ) + " x " + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 1 ) + ")" );
				System.out.println( String.format( "rendering:%4d ms", rendertime / 1000000 ) );
				System.out.println( "scale = " + currentScreenScaleIndex );

				if ( currentScreenScaleIndex > 0 )
					requestRepaint( currentScreenScaleIndex - 1 );
				else if ( !p.isValid() )
					requestRepaint( currentScreenScaleIndex );
			}
//			else
//				System.out.println("! success");
		}

		return success;
	}

	/**
	 * Request a repaint of the display from the painter thread, with maximum
	 * screen scale index and mipmap level.
	 */
	public synchronized void requestRepaint()
	{
//		System.out.println("requestRepaint()");
		newFrameRequest = true;
		requestRepaint( maxScreenScaleIndex );
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public synchronized void requestRepaint( final int screenScaleIndex )
	{
		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		requestedScreenScaleIndex = screenScaleIndex;
		painterThread.requestRepaint();
	}

	private VolatileHierarchyProjector< ?, ARGBType > createProjector(
			final ViewerState viewerState,
			final AffineTransform3D screenScaleTransform,
			final ARGBScreenImage screenImage )
	{
		synchronized( viewerState )
		{
			final List< SourceState< ? > > sources = viewerState.getSources();
			final List< Integer > visibleSourceIndices = viewerState.getVisibleSourceIndices();
			if ( visibleSourceIndices.isEmpty() )
				return null; // TODO: handle no visible sources case
			final int i = visibleSourceIndices.get( 0 );
			return createSingleSourceProjector( viewerState, sources.get( i ), i, screenScaleTransform, screenImage );
			// TODO: handle multiple sources
		}
	}

	private < T extends Volatile< ? > > VolatileHierarchyProjector< T, ARGBType > createSingleSourceProjector(
			final ViewerState viewerState,
			final SourceState< T > source,
			final int sourceIndex,
			final AffineTransform3D screenScaleTransform,
			final ARGBScreenImage screenImage )
	{
		final ArrayList< RandomAccessible< T > > levels = new ArrayList< RandomAccessible< T > >();
		final int bestLevel = viewerState.getBestMipMapLevel( screenScaleTransform, sourceIndex );
		final int nLevels = source.getSpimSource().getNumMipmapLevels();
		final Source< T > spimSource = source.getSpimSource();
		for ( int i = bestLevel; i < nLevels; ++i )
			levels.add( getTransformedSource( viewerState, spimSource, screenScaleTransform, i ) );
//		for ( int i = bestLevel - 1; i >= 0; --i )
//			levels.add( getTransformedSource( viewerState, spimSource, screenScaleTransform, i ) );

		return new VolatileHierarchyProjector< T, ARGBType >( levels, source.getConverter(), screenImage, numRenderingThreads );
	}


	/**
	 *
	 * @param screenImage
	 * 			  render target.
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @param mipmapIndex
	 *            mipmap level.
	 */
	public static InterruptibleRenderer< ?, ARGBType > createProjector( final ViewerState viewerState, final AffineTransform3D screenScaleTransform, final int[] mipmapIndex )
	{
		synchronized( viewerState )
		{
			final List< SourceState< ? > > sources = viewerState.getSources();
			final List< Integer > visibleSourceIndices = viewerState.getVisibleSourceIndices();
//			if ( visibleSourceIndices.isEmpty() )
//				return new InterruptibleRenderer< ARGBType, ARGBType >( new ConstantRandomAccessible< ARGBType >( argbtype, 2 ), new TypeIdentity< ARGBType >() );
//			else if ( visibleSourceIndices.size() == 1 )
//			{
				final int i = visibleSourceIndices.get( 0 );
				return createSingleSourceProjector( viewerState, sources.get( i ), screenScaleTransform, mipmapIndex[ i ] );
//			}
//			else
//			{
//				final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( visibleSourceIndices.size() );
//				for ( final int i : visibleSourceIndices )
//					accessibles.add( getConvertedTransformedSource( viewerState, sources.get( i ), screenScaleTransform, mipmapIndex[ i ] ) );
//				return new InterruptibleRenderer< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), new TypeIdentity< ARGBType >() );
//			}
		}
	}

	private final static ARGBType argbtype = new ARGBType();

	private static < T > RandomAccessible< T > getTransformedSource( final ViewerState viewerState, final Source< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		final int timepoint = viewerState.getCurrentTimepoint();
		final Interpolation interpolation = viewerState.getInterpolation();
		final RealRandomAccessible< T > img = source.getInterpolatedSource( timepoint, mipmapIndex, interpolation );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		sourceToScreen.concatenate( source.getSourceTransform( timepoint, mipmapIndex ) );
		sourceToScreen.preConcatenate( screenScaleTransform );

		return RealViews.constantAffine( img, sourceToScreen );
	}

//	private static < T > RandomAccessible< ARGBType > getConvertedTransformedSource( final ViewerState viewerState, final SourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
//	{
//		return Converters.convert( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter(), argbtype );
//	}

	private static < T extends Volatile< ? > > InterruptibleRenderer< T, ARGBType > createSingleSourceProjector( final ViewerState viewerState, final SourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		return new InterruptibleRenderer< T, ARGBType >( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter() );
	}
}
