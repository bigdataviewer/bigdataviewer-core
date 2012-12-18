package viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.TransformListener3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import viewer.MultiBoxOverlay.IntervalAndTransform;
import viewer.display.AccumulateARGB;
import viewer.display.InterruptibleRenderer;
import viewer.hdf5.MipMapDefinition;

public class SpimViewer implements ScreenImageRenderer, TransformListener3D, PainterThread.Paintable
{
	/**
	 * {@link SourceAndConverter} with some attached properties needed for rendering.
	 */
	protected class SourceDisplay< T extends NumericType< T > > extends SourceAndConverter< T > implements IntervalAndTransform
	{
		/**
		 * Transformation from {@link #source} to viewer (scaled
		 * {@link #screenImage}) . This is a concatenation of
		 * {@link SpimAngleSource#getSourceTransform(long) source transform} and
		 * the {@link #viewerTransform interactive viewer transform}.
		 */
		final protected AffineTransform3D sourceToViewer;

		/**
		 * Transformation from {@link #source} to {@link #screenImage}. This is
		 * a concatenation of {@link SpimAngleSource#getSourceTransform(long)
		 * source transform}, the {@link #viewerTransform interactive viewer
		 * transform}, and the {@link #screenScaleTransform viewer-to-screen
		 * transform}
		 */
		final protected AffineTransform3D sourceToScreen;

		private boolean isActive;

		public SourceDisplay( final SpimAngleSource< T > source, final Converter< T, ARGBType > converter )
		{
			super( source, converter );
			sourceToViewer = new AffineTransform3D();
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

		/**
		 * whether this source is currently visible in {@link #screenImage}.
		 */
		@Override
		public boolean isVisible()
		{
			return singleSourceMode ? ( currentSource < sources.size() && sources.get( currentSource ) == this ) : isActive;
		}

		@Override
		public Interval getSourceInterval()
		{
			return getSource( currentTimepoint, currentMipMapLevel );
		}

		@Override
		public AffineTransform3D getSourceToScreen()
		{
			return sourceToViewer;
		}
	}

	final protected ArrayList< SourceDisplay< ? > > sources;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected InterruptibleRenderer< ?, ARGBType > projector;

	/**
	 * Used to render the image for on-screen display.
	 */
	protected ARGBScreenImage screenImage;

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

	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform3D viewerTransform;

	/**
	 * The scale transformation from viewer to {@link #screenImage}.
	 */
	final protected AffineTransform3D screenScaleTransform;

	/**
	 * Canvas used for displaying the rendered {@link #screenImage}.
	 */
	final protected InteractiveDisplay3DCanvas display;

	/**
	 * Thread that triggers repainting of the display.
	 */
	final protected PainterThread painterThread;

	final protected JFrame frame;

	final protected JSlider sliderTime;

	/**
	 * number of available timepoints.
	 */
	final protected int numTimePoints;




	/**
	 * which timepoint is currently shown.
	 */
	protected int currentTimepoint = 0;

	/**
	 * which mipmap level is currently shown.
	 */
	protected int currentMipMapLevel = 0;

	/**
	 * TODO
	 */
	final protected double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125, 0.0625 };
//	final protected double[] screenScales = new double[] { 1, 0.0625 };

	protected int maxScreenScaleIndex = screenScales.length - 1;

	/**
	 * TODO
	 */
	protected int currentScreenScaleIndex = maxScreenScaleIndex;

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
	public SpimViewer( final int width, final int height, final Collection< SourceAndConverter< ? > > sources, final int numTimePoints)
	{
		this.sources = new ArrayList< SourceDisplay< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( createSourceDisplay( source ) );
		this.numTimePoints = numTimePoints;
		projector = null;
		painterThread = new PainterThread( this );
		viewerTransform = new AffineTransform3D();
		screenScaleTransform = new AffineTransform3D();

		box = new MultiBoxOverlay();
		boxInterval = Intervals.createMinSize( 10, 10, 160, 120 );

		display = new InteractiveDisplay3DCanvas( width, height, this, this );

		final SourceSwitcher sourceSwitcher = new SourceSwitcher();

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, numTimePoints - 1, 0 );
		sliderTime.addKeyListener( display.getTransformEventHandler() );
		sliderTime.addKeyListener( sourceSwitcher );
		sliderTime.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( e.getSource().equals( sliderTime ) )
					updateTimepoint( sliderTime.getValue() );
			}
		} );

		final JSlider sliderMipmap = new JSlider( JSlider.VERTICAL, 0, MipMapDefinition.numLevels - 1, 0 );
		sliderMipmap.addKeyListener( display.getTransformEventHandler() );
		sliderMipmap.addKeyListener( sourceSwitcher );
		sliderMipmap.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( e.getSource().equals( sliderMipmap ) )
					updateMipMapLevel( sliderMipmap.getValue() );
			}
		} );

		final GraphicsConfiguration gc = getSuitableGraphicsConfiguration( ARGBScreenImage.ARGB_COLOR_MODEL );
		frame = new JFrame( "multi-angle viewer", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		content.add( sliderTime, BorderLayout.SOUTH );
		content.add( sliderMipmap, BorderLayout.EAST );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.addKeyListener( display.getTransformEventHandler() );
		frame.addKeyListener( sourceSwitcher );
		frame.setVisible( true );

		painterThread.start();
	}

	/**
	 * TODO helper
	 * @param soc
	 * @return
	 */
	protected < T extends NumericType< T > > SourceDisplay< T > createSourceDisplay( final SourceAndConverter< T > soc )
	{
		return new SourceDisplay< T >( soc.source, soc.converter );
	}

	/**
	 * TODO helper
	 * @param colorModel
	 * @return
	 */
	protected static GraphicsConfiguration getSuitableGraphicsConfiguration( final ColorModel colorModel )
	{
		final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final GraphicsConfiguration defaultGc = device.getDefaultConfiguration();
		if ( defaultGc.getColorModel( Transparency.TRANSLUCENT ).equals( colorModel ) )
			return defaultGc;

		for ( final GraphicsConfiguration gc : device.getConfigurations() )
			if ( gc.getColorModel( Transparency.TRANSLUCENT ).equals( colorModel ) )
				return gc;

		return defaultGc;
	}

	/**
	 * TODO helper
	 * Whether to discard the {@link #screenImage} alpha components when drawing.
	 */
	static final protected boolean discardAlpha = true;

	/**
	 * TODO helper
	 */
	static final protected ColorModel RGB_COLOR_MODEL = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);

	/**
	 * TODO helper
	 * @param screenImage
	 * @return
	 */
	static final protected  BufferedImage getBufferedImage( final ARGBScreenImage screenImage )
	{
		if ( discardAlpha )
		{
			final BufferedImage si = screenImage.image();
			final SampleModel sampleModel = RGB_COLOR_MODEL.createCompatibleWritableRaster( 1, 1 ).getSampleModel().createCompatibleSampleModel( si.getWidth(), si.getHeight() );
			final DataBuffer dataBuffer = si.getRaster().getDataBuffer();
			final WritableRaster rgbRaster = Raster.createWritableRaster( sampleModel, dataBuffer, null );
			return new BufferedImage( RGB_COLOR_MODEL, rgbRaster, false, null );
		}
		else
			return screenImage.image();
	}


	protected ARGBScreenImage[] screenImages = null;
	protected AffineTransform3D[] screenScaleTransforms = null;


	@Override
	public void paint()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( screenImages == null || screenImages[ 0 ].dimension( 0 ) != componentW || screenImages[ 0 ].dimension( 1 ) != componentH )
		{
			screenImages = new ARGBScreenImage[ screenScales.length ];
			screenScaleTransforms = new AffineTransform3D[ screenScales.length ];
			for ( int i = 0; i < screenScales.length; ++i )
			{
				final double screenToViewerScale = screenScales[ i ];
				final int w = ( int ) ( screenToViewerScale * componentW );
				final int h = ( int ) ( screenToViewerScale * componentH );
				screenImages[ i ] = new ARGBScreenImage( w, h );
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
		screenImage = screenImages[ currentScreenScaleIndex ];
		screenScaleTransform.set( screenScaleTransforms[ currentScreenScaleIndex ] );

		synchronized( viewerTransform )
		{
			for ( final SourceDisplay< ? > source : sources )
				source.sourceToViewer.set( viewerTransform );
		}
		for ( final SourceDisplay< ? > source : sources )
		{
			source.sourceToViewer.concatenate( source.getSourceTransform( currentTimepoint, currentMipMapLevel ) );
			source.sourceToScreen.set( screenScaleTransform );
			source.sourceToScreen.concatenate( source.sourceToViewer );
		}
		final InterruptibleRenderer< ?, ARGBType > p = createProjector();
		projector = p;
		if( p.map( screenImage ) )
		{
			final long rendertime = p.getLastFrameRenderTime() / 1000000;
			if ( currentScreenScaleIndex == maxScreenScaleIndex )
			{
				if ( rendertime > 50 && maxScreenScaleIndex < screenScales.length - 1 )
					maxScreenScaleIndex++;
				else if ( rendertime < 30 && maxScreenScaleIndex > 0 )
					maxScreenScaleIndex--;
			}
			System.out.println( "rendertime = " + rendertime );
			System.out.println( "maxScreenScaleIndex = " + maxScreenScaleIndex );
			display.TMPsetBufferedImage( getBufferedImage( screenImage ) );
			System.out.println( "scale = " + currentScreenScaleIndex + "   mipmap = " + currentMipMapLevel );
			if ( currentScreenScaleIndex > 0 || currentMipMapLevel > 0 )
			{
				final double s = TMPGETSOURCERESOLUTION();
				if ( s > 0.5 && currentMipMapLevel > 0 )
					updateMipMapLevel( currentMipMapLevel - 1 );
				else if ( currentScreenScaleIndex > 0 )
					requestRepaint( currentScreenScaleIndex - 1 );
				else
					updateMipMapLevel( currentMipMapLevel - 1 );
			}
		}
		display.repaint();
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public synchronized void requestRepaint( final int screenScaleIndex )
	{
		if( currentScreenScaleIndex < maxScreenScaleIndex && projector != null )
			projector.cancel();
		currentScreenScaleIndex = screenScaleIndex;
		painterThread.requestRepaint();
	}

	double TMPGETSOURCERESOLUTION()
	{
		double pixelSize = Double.MAX_VALUE;
		for ( final SourceDisplay< ? > source : sources )
		{
			if ( source.isVisible() )
			{
				final AffineTransform3D t = source.sourceToScreen;
				final double[] zero = new double[] { 0, 0, 0 };
				final double[] one = new double[] { 1, 1, 1 };
				final double[] tzero = new double[ 3 ];
				final double[] tone = new double[ 3 ];
				final double[] diff = new double[ 3 ];
				t.apply( zero, tzero );
				t.apply( one, tone );
				for ( int d = 0; d < 3; ++d )
					diff[ d ] = Math.abs( tone[ d ] - tzero[ d ] );
				System.out.println( "(sourceToScreen) diff = " + Util.printCoordinates( diff ) );
				final double s = Math.min( diff[ 0 ], diff[ 1 ] );
				if ( s < pixelSize )
					pixelSize = s;
			}
		}
		return pixelSize;
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( !sources.isEmpty() )
		{
			box.paint( ( Graphics2D ) g, sources, virtualScreenInterval, boxInterval );

			final SourceDisplay< ? > source = sources.get( currentSource );
			g.setFont( new Font( "SansSerif", Font.PLAIN, 12 ) );
			g.drawString( source.getName(), ( int ) g.getClipBounds().getWidth() / 2, 10 );
			g.drawString( String.format( "t = %d", currentTimepoint ), ( int ) g.getClipBounds().getWidth() - 50, 10 );
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized( viewerTransform )
		{
			viewerTransform.set( transform );
		}
		requestRepaint( maxScreenScaleIndex );
	}

	protected void updateTimepoint( final int timepoint )
	{
		if ( currentTimepoint != timepoint )
		{
			currentTimepoint = timepoint;
			currentMipMapLevel = MipMapDefinition.numLevels - 1;
			requestRepaint( maxScreenScaleIndex );
		}
	}

	public void updateMipMapLevel( final int level )
	{
		System.out.println( "mip map level " + level );
		currentMipMapLevel = level;
		requestRepaint( currentScreenScaleIndex );
	}

	/**
	 * Returns a list of all currently visible sources.
	 *
	 * @return list of all currently visible sources.
	 */
	protected ArrayList< SourceDisplay< ? > > getVisibleSources()
	{
		final ArrayList< SourceDisplay< ? > > visibleSources = new ArrayList< SourceDisplay< ? > >();
		for ( final SourceDisplay< ? > source : sources )
			if ( source.isVisible() )
				visibleSources.add( source );
		return visibleSources;
	}

	/**
	 * TODO helper
	 * @param source
	 * @return
	 */
	protected < T extends NumericType< T > > AffineRandomAccessible< T, AffineGet > getTransformedSource( final SourceDisplay< T > source )
	{
		final RandomAccessibleInterval< T > img = source.getSource( currentTimepoint, currentMipMapLevel );
		final T template = source.getType().createVariable();
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
		final AffineRandomAccessible< T, AffineGet > mapping = RealViews.affine(
				Views.interpolate( Views.extendValue( img, template ), interpolatorFactory ),
				source.sourceToScreen );
		return mapping;
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

	protected InterruptibleRenderer< ?, ARGBType > createProjector()
	{
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

	protected void toggleInterpolation()
	{
		if ( interpolation == Interpolation.NEARESTNEIGHBOR )
			interpolation = Interpolation.NLINEAR;
		else interpolation = Interpolation.NEARESTNEIGHBOR;
		requestRepaint( maxScreenScaleIndex );
	}

	public void toggleSingleSourceMode()
	{
		singleSourceMode = !singleSourceMode;
		requestRepaint( maxScreenScaleIndex );
	}

	public void toggleVisibility( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < sources.size() )
		{
			final SourceDisplay< ? > source = sources.get( sourceIndex );
			source.setActive( !source.isActive() );
			requestRepaint( maxScreenScaleIndex );
		}
	}

	/**
	 * Set the index of the source to display.
	 */
	public void setCurrentSource( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < sources.size() )
		{
			currentSource = sourceIndex;
			requestRepaint( maxScreenScaleIndex );
		}
	}

	protected void selectOrToggleSource( final int sourceIndex, final boolean toggle )
	{
		if( toggle )
			toggleVisibility( sourceIndex );
		else
			setCurrentSource( sourceIndex );
	}

	protected class SourceSwitcher implements KeyListener
	{
		@Override
		public void keyTyped( final KeyEvent e )
		{}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final int modifiers = e.getModifiersEx();
			final boolean toggle = ( modifiers & KeyEvent.SHIFT_DOWN_MASK ) != 0;

			if ( e.getKeyCode() == KeyEvent.VK_I )
				toggleInterpolation();
			if ( e.getKeyCode() == KeyEvent.VK_F )
				toggleSingleSourceMode();
			else if ( e.getKeyCode() == KeyEvent.VK_1 )
				selectOrToggleSource( 0, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_2 )
				selectOrToggleSource( 1, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_3 )
				selectOrToggleSource( 2, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_4 )
				selectOrToggleSource( 3, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_5 )
				selectOrToggleSource( 4, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_6 )
				selectOrToggleSource( 5, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_7 )
				selectOrToggleSource( 6, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_8 )
				selectOrToggleSource( 7, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_9 )
				selectOrToggleSource( 8, toggle );
			else if ( e.getKeyCode() == KeyEvent.VK_0 )
				selectOrToggleSource( 9, toggle );
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}
	}
}
