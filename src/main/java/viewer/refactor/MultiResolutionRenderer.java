package viewer.refactor;

import java.awt.image.BufferedImage;

import net.imglib.ui.PainterThread;
import net.imglib.ui.component.InteractiveDisplay3DCanvas;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import viewer.GuiHelpers;
import viewer.display.InterruptibleRenderer;

public class MultiResolutionRenderer
{
	final protected InteractiveDisplay3DCanvas display;

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
	 * Used to render the image for display. One image per screen resolution.
	 */
	protected ARGBScreenImage[] screenImages;

	/**
	 * {@link BufferedImage}s wrapping the data in the {@link #screenImages}.
	 */
	protected BufferedImage[] bufferedImages;

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
	final protected int maxMipmapLevel;

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
	protected int requestedMipmapLevel;

	/**
	 * Whether the current rendering operation may be cancelled (to start a
	 * new one). Rendering may be cancelled unless we are rendering at
	 * coarsest screen scale and coarsest mipmap level.
	 */
	protected boolean renderingMayBeCancelled;

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
	 * @param numMipmapLevels
	 *            number of available mipmap levels.
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
	 */
	public MultiResolutionRenderer( final InteractiveDisplay3DCanvas display, final PainterThread painterThread, final double[] screenScales, final int numMipmapLevels, final long targetRenderNanos, final long targetIoNanos, final int badIoFrameBlockFrames )
	{
		this.display = display;
		this.painterThread = painterThread;
		projector = null;
		this.screenScales = screenScales.clone();
		screenImages = new ARGBScreenImage[ screenScales.length ];
		bufferedImages = new BufferedImage[ screenScales.length ];
		screenScaleTransforms = new AffineTransform3D[ screenScales.length ];

		maxMipmapLevel = numMipmapLevels - 1;
		this.targetRenderNanos = targetRenderNanos;
		this.targetIoNanos = targetIoNanos;
		this.badIoFrameBlockFrames = badIoFrameBlockFrames;

		maxScreenScaleIndex = screenScales.length - 1;
		requestedScreenScaleIndex = maxScreenScaleIndex;
		requestedMipmapLevel = maxMipmapLevel;
		renderingMayBeCancelled = true;
		goodIoFrames = 0;
	}

	public MultiResolutionRenderer( final InteractiveDisplay3DCanvas display, final PainterThread painterThread, final double[] screenScales, final int numMipmapLevels )
	{
		this( display, painterThread, screenScales, numMipmapLevels, 30 * 1000000, 10 * 1000000, 5 );
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages} and {@link #screenScaleTransforms} accordingly.
	 */
	protected synchronized void checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( screenImages[ 0 ] == null || screenImages[ 0 ].dimension( 0 ) * screenScales[ 0 ] != componentW || screenImages[ 0 ].dimension( 1 )  * screenScales[ 0 ] != componentH )
		{
			for ( int i = 0; i < screenScales.length; ++i )
			{
				final double screenToViewerScale = screenScales[ i ];
				final int w = ( int ) ( screenToViewerScale * componentW );
				final int h = ( int ) ( screenToViewerScale * componentH );
				screenImages[ i ] = new ARGBScreenImage( w, h );
				bufferedImages[ i ] = GuiHelpers.getBufferedImage( screenImages[ i ] );
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
	 * Render image at the {@link #requestedScreenScaleIndex requested screen
	 * scale} and the {@link #requestedMipmapLevel requested mipmap level}.
	 */
	public boolean paint( final SpimViewerState state )
	{
		checkResize();

		// the screen scale at which we will be rendering
		final int currentScreenScaleIndex;

		// the corresponding screen scale transform
		final AffineTransform3D currentScreenScaleTransform;

		// the corresponding ARGBScreenImage (to render to)
		final ARGBScreenImage screenImage;

		// the corresponding BufferedImage (to paint to the canvas)
		final BufferedImage bufferedImage;

		// the mipmap level at which we will be rendering
		final int currentMipmapLevel;

		// the mipmap level that would best suit the current screen scale
		final int targetMipmapLevel;

		// the projector that paints to the screenImage.
		final InterruptibleRenderer< ?, ARGBType > p;

		synchronized( this )
		{
			// Rendering may be cancelled unless we are rendering at coarsest
			// screen scale and coarsest mipmap level.
			renderingMayBeCancelled = ( requestedScreenScaleIndex < maxScreenScaleIndex ) || ( requestedMipmapLevel < maxMipmapLevel );

			boolean setIoTimeLimit = false;
			currentScreenScaleIndex = requestedScreenScaleIndex;
			currentScreenScaleTransform = screenScaleTransforms[ currentScreenScaleIndex ];
			targetMipmapLevel = state.getBestMipMapLevel( currentScreenScaleTransform );
			if ( targetMipmapLevel > requestedMipmapLevel )
			{
				// A coarser mipmap level than the requested is better suited to
				// the current screen scale. Use that one.
				currentMipmapLevel = targetMipmapLevel;
			}
			else if ( targetMipmapLevel < requestedMipmapLevel && goodIoFrames > badIoFrameBlockFrames )
			{
				// More than badIoFrameBlockFrames frames have been rendered
				// without hitting the io time limit. We assume that enough
				// image data is cached to render at the best suited mipmap
				// level.
				currentMipmapLevel = targetMipmapLevel;
				// However, if it turns out that we hit the io time limit on
				// this one, we will cancel and repaint at a coarser level
				setIoTimeLimit = true;
			}
			else
			{
				// The requested mipmap level is optimal. Use it.
				currentMipmapLevel = requestedMipmapLevel;
			}

			p = ScreenImageRenderer.createProjector( state, currentScreenScaleTransform, currentMipmapLevel );
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

			screenImage = screenImages[ currentScreenScaleIndex ];
			bufferedImage = bufferedImages[ currentScreenScaleIndex ];
			projector = p;
		}

		// try rendering
		final boolean success = p.map( screenImage );
		final long rendertime = p.getLastFrameRenderNanoTime();
		final long iotime = p.getLastFrameIoNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				display.setBufferedImage( bufferedImage );
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
//				System.out.println( "maxScreenScaleIndex = " + maxScreenScaleIndex + "  (" + screenImages[ maxScreenScaleIndex ].dimension( 0 ) + " x " + screenImages[ maxScreenScaleIndex ].dimension( 1 ) + ")" );
//				System.out.println( String.format( "rendering:%4d ms   io:%4d ms   (total:%4d ms)", rendertime / 1000000, iotime / 1000000, ( rendertime + iotime ) / 1000000 ) );
//				System.out.println( "scale = " + currentScreenScaleIndex + "   mipmap = " + currentMipmapLevel );
//				System.out.println( "     target mipmap = " + targetMipmapLevel );

				if ( targetMipmapLevel < currentMipmapLevel )
					requestRepaint( currentScreenScaleIndex, currentMipmapLevel - 1 );
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
	public synchronized void requestRepaint( final int screenScaleIndex, final int mipmapLevel )
	{
		if ( renderingMayBeCancelled && projector != null )
			projector.cancel();
		requestedScreenScaleIndex = screenScaleIndex;
		requestedMipmapLevel = mipmapLevel;
		painterThread.requestRepaint();
	}
}
