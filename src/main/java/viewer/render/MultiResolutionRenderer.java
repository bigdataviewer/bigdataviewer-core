package viewer.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.util.GuiUtil;
import viewer.display.AccumulateARGB;

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
	protected InterruptibleRenderer< ?, ARGBType > projector;

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
	 * TODO
	 */
	final protected long targetIoNanos;

	/**
	 * The index of the coarsest mipmap level.
	 */
	protected int[] maxMipmapLevel;

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
	 * The mipmap level which should be rendered next.
	 */
	protected int[] requestedMipmapLevel;

	/**
	 * Whether the current rendering operation may be cancelled (to start a
	 * new one). Rendering may be cancelled unless we are rendering at
	 * coarsest screen scale and coarsest mipmap level.
	 */
	protected volatile boolean renderingMayBeCancelled;

	/**
	 * If more frames than this have been rendered without hitting the
	 * {@link #targetIoNanos io time limit} then we assume that enough image
	 * data is cached to render at a higher than the
	 * {@link #requestedMipmapLevel requested mipmap level}.
	 */
	final protected int badIoFrameBlockFrames;

	/**
	 * How many consecutive frames have been rendered without hitting the
	 * {@link #targetIoNanos io time limit}.
	 */
	protected int goodIoFrames;

	/**
	 * How many threads to use for rendering.
	 */
	final protected int numRenderingThreads;

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
	 * @param targetIoNanos
	 *            Target io time in nanoseconds. A frame is considered a
	 *            "bad io frame" if the time spent reading data from disk is
	 *            above this threshold.
	 * @param badIoFrameBlockFrames
	 *            If more frames than this have been rendered without hitting a
	 *            "bad io frame" then we assume that enough image data is cached
	 *            to render at the optimal mipmap level (instead of the coarsest
	 *            first).
	 * @param doubleBuffered
	 *            Whether to use double buffered rendering.
	 * @param numRenderingThreads
	 *            How many threads to use for rendering.
	 */
	public MultiResolutionRenderer( final RenderTarget display, final PainterThread painterThread, final double[] screenScales, final long targetRenderNanos, final long targetIoNanos, final int badIoFrameBlockFrames, final boolean doubleBuffered, final int numRenderingThreads )
	{
		this.display = display;
		this.painterThread = painterThread;
		projector = null;
		this.screenScales = screenScales.clone();
		this.doubleBuffered = doubleBuffered;
		screenImages = new ARGBScreenImage[ screenScales.length ][ 2 ];
		bufferedImages = new BufferedImage[ screenScales.length ][ 2 ];
		screenScaleTransforms = new AffineTransform3D[ screenScales.length ];

		maxMipmapLevel = new int[ 0 ];
//		maxMipmapLevel = new int[ numMipmapLevels.length ];
//		for ( int i = 0; i < maxMipmapLevel.length; ++i )
//			maxMipmapLevel[ i ] = numMipmapLevels[ i ] - 1;
		this.targetRenderNanos = targetRenderNanos;
		this.targetIoNanos = targetIoNanos;
		this.badIoFrameBlockFrames = badIoFrameBlockFrames;

		maxScreenScaleIndex = screenScales.length - 1;
		requestedScreenScaleIndex = maxScreenScaleIndex;
		requestedMipmapLevel = maxMipmapLevel.clone();
		renderingMayBeCancelled = true;
		goodIoFrames = 0;
		this.numRenderingThreads = numRenderingThreads;
	}

	public MultiResolutionRenderer( final RenderTarget display, final PainterThread painterThread, final double[] screenScales )
	{
		this( display, painterThread, screenScales, 30 * 1000000, 10 * 1000000, 5, true, 3 );
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages} and {@link #screenScaleTransforms} accordingly.
	 */
	protected synchronized void checkResize()
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
		}
	}

	/**
	 * Check whether the number of sources in the state has
	 * changed and recreate mipmap arrays if necessary.
	 */
	protected synchronized void checkNumSourcesChanged( final ViewerState state )
	{
		final int numSources = state.numSources();
		if ( numSources != maxMipmapLevel.length )
		{
			final List< SourceState< ? >> sources = state.getSources();
			maxMipmapLevel = new int[ numSources ];
			for ( int i = 0; i < maxMipmapLevel.length; ++i )
				maxMipmapLevel[ i ] = sources.get( i ).getSpimSource().getNumMipmapLevels() - 1;
			requestedMipmapLevel = maxMipmapLevel.clone();
		}
	}

	/**
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale} and the {@link #requestedMipmapLevel requested mipmap level}.
	 */
	public boolean paint( final ViewerState state )
	{
		checkResize();
		checkNumSourcesChanged( state );

		final int numSources = state.numSources();

		// the screen scale at which we will be rendering
		final int currentScreenScaleIndex;

		// the corresponding screen scale transform
		final AffineTransform3D currentScreenScaleTransform;

		// the corresponding ARGBScreenImage (to render to)
		final ARGBScreenImage screenImage;

		// the corresponding BufferedImage (to paint to the canvas)
		final BufferedImage bufferedImage;

		// the mipmap level at which we will be rendering
		final int[] currentMipmapLevel = new int[ numSources ];

		// the mipmap level that would best suit the current screen scale
		final int[] targetMipmapLevel = new int[ numSources ];

		// the projector that paints to the screenImage.
		final InterruptibleRenderer< ?, ARGBType > p;

		synchronized( this )
		{
			// Rendering may be cancelled unless we are rendering at coarsest
			// screen scale and coarsest mipmap level.
			renderingMayBeCancelled = ( requestedScreenScaleIndex < maxScreenScaleIndex );
			for ( int i = 0; ( ! renderingMayBeCancelled ) && ( i < numSources ); ++i )
				renderingMayBeCancelled = requestedMipmapLevel[ i ] < maxMipmapLevel[ i ];

			boolean setIoTimeLimit = false;
			currentScreenScaleIndex = requestedScreenScaleIndex;
			currentScreenScaleTransform = screenScaleTransforms[ currentScreenScaleIndex ];
			for ( int i = 0; i < numSources; ++i )
			{
				targetMipmapLevel[ i ] = state.getBestMipMapLevel( currentScreenScaleTransform, i );
				if ( targetMipmapLevel[ i ] > requestedMipmapLevel[ i ] )
				{
					// A coarser mipmap level than the requested is better suited to
					// the current screen scale. Use that one.
					currentMipmapLevel[ i ] = targetMipmapLevel[ i ];
				}
				else if ( targetMipmapLevel[ i ] < requestedMipmapLevel[ i ] && goodIoFrames > badIoFrameBlockFrames )
				{
					// More than badIoFrameBlockFrames frames have been rendered
					// without hitting the io time limit. We assume that enough
					// image data is cached to render at the best suited mipmap
					// level.
					currentMipmapLevel[ i ] = targetMipmapLevel[ i ];
					// However, if it turns out that we hit the io time limit on
					// this one, we will cancel and repaint at a coarser level
					setIoTimeLimit = true;
				}
				else
				{
					// The requested mipmap level is optimal. Use it.
					currentMipmapLevel[ i ] = requestedMipmapLevel[ i ];
				}
			}

			p = createProjector( state, currentScreenScaleTransform, currentMipmapLevel );
			if ( setIoTimeLimit )
			{
				p.setIoTimeOut( targetIoNanos, new Runnable()
				{
					@Override
					public void run()
					{
						goodIoFrames = 0;
						requestRepaint();
					}
				} );
			}

			screenImage = screenImages[ currentScreenScaleIndex ][ 0 ];
			bufferedImage = bufferedImages[ currentScreenScaleIndex ][ 0 ];
			projector = p;
		}

		// try rendering
		final boolean success = p.map( screenImage, numRenderingThreads );
		final long rendertime = p.getLastFrameRenderNanoTime();
		final long iotime = p.getLastFrameIoNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				display.setBufferedImage( bufferedImage );

				if ( doubleBuffered )
				{
					screenImages[ currentScreenScaleIndex ][ 0 ] = screenImages[ currentScreenScaleIndex ][ 1 ];
					screenImages[ currentScreenScaleIndex ][ 1 ] = screenImage;
					bufferedImages[ currentScreenScaleIndex ][ 0 ] = bufferedImages[ currentScreenScaleIndex ][ 1 ];
					bufferedImages[ currentScreenScaleIndex ][ 1 ] = bufferedImage;
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
//				System.out.println( "maxScreenScaleIndex = " + maxScreenScaleIndex + "  (" + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 0 ) + " x " + screenImages[ maxScreenScaleIndex ][ 0 ].dimension( 1 ) + ")" );
//				System.out.println( String.format( "rendering:%4d ms   io:%4d ms   (total:%4d ms)", rendertime / 1000000, iotime / 1000000, ( rendertime + iotime ) / 1000000 ) );
//				System.out.println( "scale = " + currentScreenScaleIndex + "   mipmap = " + Util.printCoordinates( currentMipmapLevel ) );
//				System.out.println( "     target mipmap = " + Util.printCoordinates( targetMipmapLevel ) );

				boolean refineMipmap = false;
				for ( int i = 0; i < numSources; ++i )
					if ( targetMipmapLevel[ i ] < currentMipmapLevel[ i ] )
					{
						currentMipmapLevel[ i ] -= 1;
						refineMipmap = true;
					}
				if ( refineMipmap )
					requestRepaint( currentScreenScaleIndex, currentMipmapLevel );
				else if ( currentScreenScaleIndex > 0 )
					requestRepaint( currentScreenScaleIndex - 1, currentMipmapLevel );
			}
			if ( iotime <= targetIoNanos )
				goodIoFrames++;
			else
				goodIoFrames = 0;
//			System.out.println( "goodIoFrames = " + goodIoFrames );
		}

		return success;
	}

	/**
	 * Request a repaint of the display from the painter thread, with maximum
	 * screen scale index and mipmap level.
	 */
	public synchronized void requestRepaint()
	{
		requestRepaint( maxScreenScaleIndex, maxMipmapLevel );
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public synchronized void requestRepaint( final int screenScaleIndex, final int[] mipmapLevel )
	{
		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		requestedScreenScaleIndex = screenScaleIndex;
		for ( int i = 0; i < requestedMipmapLevel.length; ++i )
			requestedMipmapLevel[ i ] = mipmapLevel[ i ];
		painterThread.requestRepaint();
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
			if ( visibleSourceIndices.isEmpty() )
				return new InterruptibleRenderer< ARGBType, ARGBType >( new ConstantRandomAccessible< ARGBType >( argbtype, 2 ), new TypeIdentity< ARGBType >() );
			else if ( visibleSourceIndices.size() == 1 )
			{
				final int i = visibleSourceIndices.get( 0 );
				return createSingleSourceProjector( viewerState, sources.get( i ), screenScaleTransform, mipmapIndex[ i ] );
			}
			else
			{
				final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( visibleSourceIndices.size() );
				for ( final int i : visibleSourceIndices )
					accessibles.add( getConvertedTransformedSource( viewerState, sources.get( i ), screenScaleTransform, mipmapIndex[ i ] ) );
				return new InterruptibleRenderer< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), new TypeIdentity< ARGBType >() );
			}
		}
	}

	private final static ARGBType argbtype = new ARGBType();

	private static < T extends NumericType< T > > RandomAccessible< T > getTransformedSource( final ViewerState viewerState, final Source< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
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

	private static < T extends NumericType< T > > RandomAccessible< ARGBType > getConvertedTransformedSource( final ViewerState viewerState, final SourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		return Converters.convert( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter(), argbtype );
	}

	private static < T extends NumericType< T > > InterruptibleRenderer< T, ARGBType > createSingleSourceProjector( final ViewerState viewerState, final SourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		return new InterruptibleRenderer< T, ARGBType >( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter() );
	}
}
