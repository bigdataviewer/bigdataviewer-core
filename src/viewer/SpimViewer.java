package viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.TransformListener3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import viewer.display.AccumulateARGB;
import viewer.display.InterruptibleRenderer;

public class SpimViewer implements ScreenImageRenderer, TransformListener3D, PainterThread.Paintable
{
	/**
	 * SpimSource with some attached state needed for rendering.
	 */
	protected static class SourceDisplay< T extends NumericType< T > > extends SourceAndConverter< T >
	{
		/**
		 * Current transformation from {@link #source} to {@link #screenImage}. This is
		 * a concatenation of {@link SpimSource#getSourceTransform(long)
		 * source transform}, the {@link #viewerTransform interactive viewer
		 * transform}, and the {@link #screenScaleTransform viewer-to-screen
		 * transform}
		 */
		final protected AffineTransform3D sourceToScreen;

		/**
		 * Whether the source is active (visible in fused mode).
		 */
		protected boolean isActive;

		public SourceDisplay( final SpimSource< T > spimSource, final Converter< T, ARGBType > converter )
		{
			super( spimSource, converter );
			sourceToScreen = new AffineTransform3D();
			isActive = true;
		}

		public boolean isActive()
		{
			return isActive;
		}

		public void setActive( final boolean isActive )
		{
			this.isActive = isActive;
		}
	}

	/**
	 * Is the source currently visible in {@link #screenImage}. A source is
	 * visible if it is active in fused-mode or it is the current source in
	 * single-source-mode.
	 *
	 * @return true, if the source is currently visible.
	 */
	public boolean isSourceVisible( final SourceDisplay< ? > source )
	{
		return singleSourceMode ? ( currentSource < sources.size() && sources.get( currentSource ) == source ) : source.isActive();
	}

	/**
	 * Create a {@link SourceDisplay} from a {@link SourceAndConverter}.
	 */
	protected < T extends NumericType< T > > SourceDisplay< T > createSourceDisplay( final SourceAndConverter< T > soc )
	{
		return new SourceDisplay< T >( soc.getSpimSource(), soc.getConverter() );
	}

	final protected ArrayList< SourceDisplay< ? > > sources;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected InterruptibleRenderer< ?, ARGBType > projector;

	/**
	 * Used to render the image for display. One image per screen resolution.
	 */
	protected ARGBScreenImage[] screenImages = null;

	/**
	 * {@link BufferedImage}s wrapping the data in the {@link #screenImages}.
	 */
	protected BufferedImage[] bufferedImages = null;

	/**
	 * Scale factors from the {@link #display viewer canvas} to the
	 * {@link #screenImages}.
	 *
	 * A scale factor of 1 means 1 pixel in the screen image is displayed as 1
	 * pixel on the canvas, a scale factor of 0.5 means 1 pixel in the screen
	 * image is displayed as 2 pixel on the canvas, etc.
	 */
	final protected double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 }; //, 0.0625 };

	/**
	 * The scale transformation from viewer to {@link #screenImages screen
	 * image}. Each transformations corresponds to a {@link #screenScales screen
	 * scale}.
	 */
	protected AffineTransform3D[] screenScaleTransforms = null;


	/**
	 * Navigation wire-frame cube.
	 */
	final protected MultiBoxOverlay box;

	/**
	 * Screen interval in which to display navigation wire-frame cube.
	 */
	final protected Interval boxInterval;

	/**
	 * scaled screenImage interval for {@link #box} rendering
	 */
	protected Interval virtualScreenInterval;

	final protected ArrayList< IntervalAndTransform > boxSources;



	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform3D viewerTransform;

	/**
	 * Canvas used for displaying the rendered {@link #screenImages screen image}.
	 */
	final protected SpimViewerCanvas display;

	/**
	 * Thread that triggers repainting of the display.
	 */
	final protected PainterThread painterThread;

	final protected JFrame frame;

	final protected JSlider sliderTime;

	final protected MouseCoordinateListener mouseCoordinates;

	/**
	 * number of available timepoints.
	 */
	final protected int numTimePoints;

	/**
	 * index of coarsest mipmap level.
	 */
	final protected int maxMipmapLevel;




	/**
	 * which timepoint is currently shown.
	 */
	protected int currentTimepoint = 0;

	/**
	 * which mipmap level is currently shown.
	 */
	protected int currentMipmapLevel = 0;

	/**
	 * If the rendering time (in nanoseconds) for the (currently) highest scaled
	 * screen image is above this threshold, increase the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 * Similarly, if the rendering time for the (currently) second-highest
	 * scaled screen image is below this threshold, decrease the
	 * {@link #maxScreenScaleIndex index} of the highest screen scale to use.
	 */
	final long targetRenderNanos = 30 * 1000000;

	final long targetIoNanos = 10 * 1000000;

	/**
	 * TODO
	 */
	protected int maxScreenScaleIndex = screenScales.length - 1;

	/**
	 * TODO
	 */
	protected int currentScreenScaleIndex = maxScreenScaleIndex;

	/**
	 * TODO
	 */
	protected int requestedScreenScaleIndex = maxScreenScaleIndex;

	/**
	 * TODO
	 */
	protected int requestedMipmapLevel;

	/**
	 * Interpolation methods.
	 */
	static enum Interpolation
	{
		NEARESTNEIGHBOR,
		NLINEAR
	}

	/**
	 * Which interpolation method is currently used to render the display.
	 */
	protected Interpolation interpolation = Interpolation.NEARESTNEIGHBOR;

	/**
	 * Whether single-source mode is currently used. In "single-source" mode,
	 * only source (SPIM angle) is shown. Otherwise, in "fused" mode, all active
	 * sources are blended.
	 */
	protected boolean singleSourceMode = true;

	/**
	 * in single-source mode: index of the source that is currently shown.
	 */
	protected int currentSource = 0;

	/**
	 *
	 * @param width
	 *            width of the display window
	 * @param height
	 *            height of the display window
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display
	 */
	public SpimViewer( final int width, final int height, final Collection< SourceAndConverter< ? > > sources, final int numTimePoints, final int numMipmapLevels )
	{
		this.sources = new ArrayList< SourceDisplay< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( createSourceDisplay( source ) );
		this.numTimePoints = numTimePoints;
		maxMipmapLevel = numMipmapLevels - 1;
		requestedMipmapLevel = maxMipmapLevel;
		projector = null;
		painterThread = new PainterThread( this );
		viewerTransform = new AffineTransform3D();

		box = new MultiBoxOverlay();
		boxInterval = Intervals.createMinSize( 10, 10, 160, 120 );
		boxSources = new ArrayList< IntervalAndTransform >( sources.size() );
		for ( int i = 0; i < sources.size(); ++i )
			boxSources.add( new IntervalAndTransform() );
		virtualScreenInterval = Intervals.createMinSize( 0, 0, width, height );

		display = new SpimViewerCanvas( width, height, this, this );

		mouseCoordinates = new MouseCoordinateListener() ;
		display.addHandler( mouseCoordinates );

		final SourceSwitcher sourceSwitcher = new SourceSwitcher();
		display.addKeyListener( sourceSwitcher );

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, numTimePoints - 1, 0 );
//		sliderTime.addKeyListener( display.getTransformEventHandler() );
		sliderTime.addKeyListener( sourceSwitcher );
		sliderTime.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( e.getSource().equals( sliderTime ) )
					updateTimepoint( sliderTime.getValue() );
			}
		} );

//		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( ARGBScreenImage.ARGB_COLOR_MODEL );
		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( GuiHelpers.RGB_COLOR_MODEL );
		frame = new JFrame( "multi-angle viewer", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		content.add( sliderTime, BorderLayout.SOUTH );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//		frame.addKeyListener( display.getTransformEventHandler() );
		frame.addKeyListener( sourceSwitcher );
		frame.setVisible( true );

		painterThread.start();
	}

	public void addHandler( final Object handler )
	{
		display.addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
			frame.addKeyListener( ( KeyListener ) handler );
	}

	public void getGlobalMouseCoordinates( final RealPositionable gPos )
	{
		assert gPos.numDimensions() == 3;
		final RealPoint lPos = new RealPoint( 3 );
		mouseCoordinates.getMouseCoordinates( lPos );
		viewerTransform.applyInverse( gPos, lPos );
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages}, {@link #screenScaleTransforms}, and {@link #virtualScreenInterval} accordingly.
	 */
	protected synchronized void checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( screenImages == null || screenImages[ 0 ].dimension( 0 ) != componentW || screenImages[ 0 ].dimension( 1 ) != componentH )
		{
			screenImages = new ARGBScreenImage[ screenScales.length ];
			bufferedImages = new BufferedImage[ screenScales.length ];
			screenScaleTransforms = new AffineTransform3D[ screenScales.length ];
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
			virtualScreenInterval = Intervals.createMinSize( 0, 0, componentW, componentH );
		}
	}

	/**
	 * Update data to show in the box overlay.
	 */
	protected synchronized void updateBoxSources()
	{
		final AffineTransform3D sourceToViewer = new AffineTransform3D();
		for ( int i = 0; i < sources.size(); ++i )
		{
			final SourceDisplay< ? > source = sources.get( i );
			final IntervalAndTransform boxsource = boxSources.get( i );
			sourceToViewer.set( viewerTransform );
			sourceToViewer.concatenate( source.getSpimSource().getSourceTransform( currentTimepoint, 0 ) );
			boxsource.setSourceToViewer( sourceToViewer );
			boxsource.setSourceInterval( source.getSpimSource().getSource( currentTimepoint, 0 ) );
			boxsource.setVisible( isSourceVisible( source ) );
		}
	}

	int goodIoFrames = 0;

	@Override
	public void paint()
	{
		checkResize();
		updateBoxSources();

		final int targetMipmapLevel;
		synchronized( this )
		{
			currentScreenScaleIndex = requestedScreenScaleIndex;
			currentMipmapLevel = requestedMipmapLevel;
			targetMipmapLevel = getBestMipMapLevel( currentScreenScaleIndex );
			if ( targetMipmapLevel > currentMipmapLevel || goodIoFrames > 5 )
				currentMipmapLevel = targetMipmapLevel;
		}

		final InterruptibleRenderer< ?, ARGBType > p = createProjector();
		final ARGBScreenImage screenImage;
		final BufferedImage bufferedImage;
		synchronized( this )
		{
			screenImage = screenImages[ currentScreenScaleIndex ];
			bufferedImage = bufferedImages[ currentScreenScaleIndex ];
			projector = p;
		}

		if( p.map( screenImage ) )
		{
			display.setBufferedImage( bufferedImage );
			final long rendertime = p.getLastFrameRenderNanoTime();
//			final long iotime = p.getLastFrameIoNanoTime();
			synchronized( this )
			{
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
//				System.out.println( String.format( "rendering:%4d ms   io:%4d ms   (total:%4d ms)", rendertime / 1000000, iotime / 1000000, (rendertime + iotime) / 1000000 ) );
//				System.out.println( "scale = " + currentScreenScaleIndex + "   mipmap = " + currentMipmapLevel );

				if ( targetMipmapLevel < currentMipmapLevel )
					requestRepaint( currentScreenScaleIndex, currentMipmapLevel - 1 );
				else if ( currentScreenScaleIndex > 0 )
					requestRepaint( currentScreenScaleIndex - 1, currentMipmapLevel );
			}
		}
		if ( p.getLastFrameIoNanoTime() <= targetIoNanos )
			goodIoFrames++;
		else
			goodIoFrames = 0;
//		System.out.println( "goodIoFrames = " + goodIoFrames );

		synchronized( this )
		{
			if ( currentAnimator != null )
			{
				final TransformEventHandler3D handler = display.getTransformEventHandler();
				final AffineTransform3D transform = currentAnimator.getCurrent();
				handler.setTransform( transform );
				transformChanged( transform );
				if ( currentAnimator.isComplete() )
					currentAnimator = null;
			}
		}

		display.repaint();
	}

	public void requestRepaint()
	{
		requestRepaint( maxScreenScaleIndex, maxMipmapLevel );
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public void requestRepaint( final int screenScaleIndex, final int mipmapLevel )
	{
		synchronized( this )
		{
			if( ( currentScreenScaleIndex < maxScreenScaleIndex || requestedMipmapLevel < maxMipmapLevel ) && projector != null )
				projector.cancel();
			requestedScreenScaleIndex = screenScaleIndex;
			requestedMipmapLevel = mipmapLevel;
		}
		painterThread.requestRepaint();
	}

	/**
	 * TODO: fix doc
	 * Compute the maximum "pixel size" at the given screen scale and mipmap
	 * level. For every source, take a source voxel (0,0,0)-(1,1,1) at the given
	 * mipmap level and transform it to the screen image at the given screen
	 * scale. Take the maximum of the screen extends of the transformed voxel in
	 * any dimension. Do this for every source and take the maximum.
	 *
	 * @param scaleIndex
	 *            screen scale
	 * @param mipmapIndex
	 *            mipmap level
	 * @return pixel size
	 */
	protected double getSourceResolution( final int scaleIndex, final int mipmapIndex )
	{
		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		double pixelSize = 0;
		for ( final SourceDisplay< ? > source : sources )
		{
			if ( isSourceVisible( source ) )
			{
				sourceToScreen.set( viewerTransform );
				sourceToScreen.concatenate( source.getSpimSource().getSourceTransform( currentTimepoint, mipmapIndex ) );
				sourceToScreen.preConcatenate( screenScaleTransforms[ scaleIndex ] );
				final double[] zero = new double[] { 0, 0, 0 };
				final double[] tzero = new double[ 3 ];
				final double[] one = new double[ 3 ];
				final double[] tone = new double[ 3 ];
				final double[] diff = new double[ 2 ];
				sourceToScreen.apply( zero, tzero );
				for ( int i = 0; i < 3; ++i )
				{
					for ( int d = 0; d < 3; ++d )
						one[ d ] = d == i ? 1 : 0;
					sourceToScreen.apply( one, tone );
					LinAlgHelpers.subtract( tone, tzero, tone );
					diff[0] = tone[0];
					diff[1] = tone[1];
					final double l = LinAlgHelpers.length( diff );
					if ( l > pixelSize )
						pixelSize = l;
				}
			}
		}
		return pixelSize;
	}

	/**
	 * Get the mipmap level that best matches the given screen scale.
	 *
	 * @param scaleIndex
	 *            screen scale
	 * @return mipmap level
	 */
	protected int getBestMipMapLevel( final int scaleIndex )
	{
		int targetLevel = maxMipmapLevel;
		for ( int level = maxMipmapLevel; level >= 0; level-- )
		{
			final double r = getSourceResolution( currentScreenScaleIndex, level );
			if ( r >= 1.0 )
				targetLevel = level;
		}
		return targetLevel;
	}


	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( !sources.isEmpty() )
		{
			box.paint( ( Graphics2D ) g, boxSources, virtualScreenInterval, boxInterval );

			final String sourceName;
			final String timepointString;
			synchronized( this )
			{
				final SourceDisplay< ? > source = sources.get( currentSource );
				sourceName = source.getSpimSource().getName();
				timepointString = String.format( "t = %d", currentTimepoint );
			}

			final RealPoint gPos = new RealPoint( 3 );
			getGlobalMouseCoordinates( gPos );
			final String mousePosGlobalString = String.format( "(%6.1f,%6.1f,%6.1f)", gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ), gPos.getDoublePosition( 2 ) );

			g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
			g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() / 2, 12 );
			g.drawString( timepointString, ( int ) g.getClipBounds().getWidth() - 170, 12 );
			g.drawString( mousePosGlobalString, ( int ) g.getClipBounds().getWidth() - 170, 25 );
		}
	}

	@Override
	public synchronized void transformChanged( final AffineTransform3D transform )
	{
		viewerTransform.set( transform );
		requestRepaint( maxScreenScaleIndex, maxMipmapLevel );
	}

	RotationAnimator currentAnimator = null;

	static enum AlignPlane
	{
		XY,
		ZY,
		XZ
	}

	private final static double c = Math.cos( Math.PI / 4 );
	private final static double[] qAlignXY = new double[] { 1,  0,  0, 0 };
	private final static double[] qAlignZY = new double[] { c,  0, -c, 0 };
	private final static double[] qAlignXZ = new double[] { c,  c,  0, 0 };

	protected synchronized void align( final AlignPlane plane )
	{
		final SourceDisplay< ? > source = sources.get( currentSource );
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform( currentTimepoint, currentMipmapLevel );

		final double[] qSource = new double[ 4 ];
		RotationAnimator.extractRotationAnisotropic( sourceTransform, qSource );

		final double[] qTmpSource;
		if ( plane == AlignPlane.XY )
			qTmpSource = qSource;
		else
		{
			qTmpSource = new double[4];
			if ( plane == AlignPlane.ZY )
				LinAlgHelpers.quaternionMultiply( qSource, qAlignZY, qTmpSource );
			else // if ( plane == AlignPlane.XZ )
				LinAlgHelpers.quaternionMultiply( qSource, qAlignXZ, qTmpSource );
		}

		final double[] qTarget = new double[ 4 ];
		LinAlgHelpers.quaternionInvert( qTmpSource, qTarget );

		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();
		currentAnimator = new RotationAnimator( transform, mouseCoordinates.getX(), mouseCoordinates.getY(), qTarget, 300 );
		transformChanged( transform );
	}

	protected synchronized void updateTimepoint( final int timepoint )
	{
		if ( currentTimepoint != timepoint )
		{
			currentTimepoint = timepoint;
			requestRepaint( maxScreenScaleIndex, maxMipmapLevel );
		}
	}

	/**
	 * Returns a list of all currently visible sources.
	 *
	 * @return list of all currently visible sources.
	 */
	protected synchronized ArrayList< SourceDisplay< ? > > getVisibleSources()
	{
		final ArrayList< SourceDisplay< ? > > visibleSources = new ArrayList< SourceDisplay< ? > >();
		for ( final SourceDisplay< ? > source : sources )
			if ( isSourceVisible( source ) )
				visibleSources.add( source );
		return visibleSources;
	}

	/**
	 * TODO helper
	 * @param source
	 * @return
	 */
	protected synchronized < T extends NumericType< T > > RandomAccessible< T > getTransformedSource( final SourceDisplay< T > source )
	{
		final RandomAccessibleInterval< T > img = source.getSpimSource().getSource( currentTimepoint, currentMipmapLevel );
		final T template = source.getSpimSource().getType().createVariable();
		template.setZero();
		final InterpolatorFactory< T, RandomAccessible< T > > interpolatorFactory;
		switch ( interpolation )
		{
		case NEARESTNEIGHBOR:
			interpolatorFactory = new NearestNeighborInterpolatorFactory< T >();
			break;
		case NLINEAR:
		default:
			interpolatorFactory = new NLinearInterpolatorFactory< T >();
			break;
		}
		final RandomAccessible< T > transformed = RealViews.constantAffine(
				Views.interpolate( Views.extendValue( img, template ), interpolatorFactory ),
				source.sourceToScreen );
		return transformed;
	}

	/**
	 * TODO helper
	 * @param source
	 * @return
	 */
	protected < T extends NumericType< T > > RandomAccessible< ARGBType > getConvertedTransformedSource( final SourceDisplay< T > source )
	{
		return Converters.convert( getTransformedSource( source ), source.converter, new ARGBType() );
	}

	/**
	 * TODO helper
	 * @param source
	 * @return
	 */
	protected < T extends NumericType< T > > InterruptibleRenderer< T, ARGBType > createSingleSourceProjector( final SourceDisplay< T > source )
	{
		return new InterruptibleRenderer< T, ARGBType >( getTransformedSource( source ), source.converter );
	}

	protected synchronized InterruptibleRenderer< ?, ARGBType > createProjector()
	{
		final AffineTransform3D screenScaleTransform = screenScaleTransforms[ currentScreenScaleIndex ];
		final AffineTransform3D sourceToViewer = new AffineTransform3D();
		for ( final SourceDisplay< ? > source : sources )
		{
			sourceToViewer.set( viewerTransform );
			sourceToViewer.concatenate( source.getSpimSource().getSourceTransform( currentTimepoint, currentMipmapLevel ) );
			source.sourceToScreen.set( screenScaleTransform );
			source.sourceToScreen.concatenate( sourceToViewer );
		}

		final ArrayList< SourceDisplay< ? > > visibleSources = getVisibleSources();
		if ( visibleSources.isEmpty() )
			return new InterruptibleRenderer< ARGBType, ARGBType >( new ConstantRandomAccessible< ARGBType >( new ARGBType(), 2 ), new TypeIdentity< ARGBType >() );
		else if ( visibleSources.size() == 1 )
			return createSingleSourceProjector( visibleSources.get( 0 ) );
		else
		{
			final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( sources.size() );
			for ( final SourceDisplay< ? > source : visibleSources )
				accessibles.add( getConvertedTransformedSource( source ) );
			return new InterruptibleRenderer< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), new TypeIdentity< ARGBType >() );
		}
	}

	protected synchronized void toggleInterpolation()
	{
		if ( interpolation == Interpolation.NEARESTNEIGHBOR )
			interpolation = Interpolation.NLINEAR;
		else interpolation = Interpolation.NEARESTNEIGHBOR;
		requestRepaint( maxScreenScaleIndex, currentMipmapLevel );
	}

	public synchronized void toggleSingleSourceMode()
	{
		singleSourceMode = !singleSourceMode;
		requestRepaint( maxScreenScaleIndex, currentMipmapLevel );
	}

	public synchronized void toggleVisibility( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < sources.size() )
		{
			final SourceDisplay< ? > source = sources.get( sourceIndex );
			source.setActive( !source.isActive() );
			requestRepaint( maxScreenScaleIndex, currentMipmapLevel );
		}
	}

	/**
	 * Set the index of the source to display.
	 */
	public synchronized void setCurrentSource( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < sources.size() )
		{
			currentSource = sourceIndex;
			requestRepaint( maxScreenScaleIndex, currentMipmapLevel );
		}
	}

	protected class SourceSwitcher implements KeyListener
	{
		@Override
		public void keyTyped( final KeyEvent e )
		{}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final int keyCode = e.getKeyCode();
			final int modifiers = e.getModifiersEx();
			final boolean toggle = ( modifiers & KeyEvent.SHIFT_DOWN_MASK ) != 0;
			final boolean align = toggle;

			if ( keyCode == KeyEvent.VK_I )
				toggleInterpolation();
			else if ( keyCode == KeyEvent.VK_F )
				toggleSingleSourceMode();
			else if ( keyCode == KeyEvent.VK_1 )
				selectOrToggleSource( 0, toggle );
			else if ( keyCode == KeyEvent.VK_2 )
				selectOrToggleSource( 1, toggle );
			else if ( keyCode == KeyEvent.VK_3 )
				selectOrToggleSource( 2, toggle );
			else if ( keyCode == KeyEvent.VK_4 )
				selectOrToggleSource( 3, toggle );
			else if ( keyCode == KeyEvent.VK_5 )
				selectOrToggleSource( 4, toggle );
			else if ( keyCode == KeyEvent.VK_6 )
				selectOrToggleSource( 5, toggle );
			else if ( keyCode == KeyEvent.VK_7 )
				selectOrToggleSource( 6, toggle );
			else if ( keyCode == KeyEvent.VK_8 )
				selectOrToggleSource( 7, toggle );
			else if ( keyCode == KeyEvent.VK_9 )
				selectOrToggleSource( 8, toggle );
			else if ( keyCode == KeyEvent.VK_0 )
				selectOrToggleSource( 9, toggle );
			else if ( keyCode == KeyEvent.VK_Z && align )
				align( AlignPlane.XY );
			else if ( keyCode == KeyEvent.VK_X && align )
				align( AlignPlane.ZY );
			else if ( ( keyCode == KeyEvent.VK_Y || keyCode == KeyEvent.VK_A ) && align )
				align( AlignPlane.XZ );
		}

		protected void selectOrToggleSource( final int sourceIndex, final boolean toggle )
		{
			if( toggle )
				toggleVisibility( sourceIndex );
			else
				setCurrentSource( sourceIndex );
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}
	}

	protected class MouseCoordinateListener implements MouseMotionListener
	{
		private int x;

		private int y;

		public synchronized void getMouseCoordinates( final Positionable p )
		{
			p.setPosition( x, 0 );
			p.setPosition( y, 1 );
		}

		@Override
		public synchronized void mouseDragged( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
		}

		@Override
		public synchronized void mouseMoved( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
			display.repaint();
		}

		public synchronized int getX()
		{
			return x;
		}

		public synchronized int getY()
		{
			return y;
		}
	}
}
