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
import java.awt.image.ColorModel;
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
import net.imglib2.ui.ScreenImageRenderer;
import net.imglib2.ui.TransformListener3D;
import net.imglib2.ui.jcomponent.InteractiveDisplay3DCanvas;
import net.imglib2.ui.jcomponent.MappingThread;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import viewer.MultiBoxOverlay.IntervalAndTransform;
import viewer.display.AccumulateARGB;
import viewer.display.InterruptibleRenderer;
import viewer.hdf5.MipMapDefinition;
import viewer.hdf5.img.Hdf5GlobalCellCache;

public class SpimViewer implements ScreenImageRenderer, TransformListener3D
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

		/**
		 * whether this source is currently visible in {@link #screenImage}.
		 */
		@Override
		public boolean isVisible()
		{
			return singleSourceMode ? ( currentSource < sources.size() && sources.get( currentSource ) == this ) : isActive;
		}

		public boolean isActive()
		{
			return isActive;
		}

		public void setActive( final boolean isActive )
		{
			this.isActive = isActive;
		}

		@Override
		public Interval getSourceInterval()
		{
			return getSource( currentTimepoint, currentLevel );
		}

		@Override
		public AffineTransform3D getSourceToScreen()
		{
			return sourceToViewer;
		}
	}

	final protected ArrayList< SourceDisplay< ? > > sources;

	final protected Hdf5GlobalCellCache< ? > cache;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected InterruptibleRenderer< ?, ARGBType > projector;

//	/**
//	 * Current combined source, used to re-paint the display. The combined
//	 * source consists of all active sources, transformed to screen coordinates,
//	 * converted to {@link ARGBType}, and blended into a single
//	 * {@link RandomAccessible}.
//	 */
//	protected RandomAccessible< ARGBType > screenSource;

	/**
	 * render target
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
	final protected AffineTransform3D viewerTransform = new AffineTransform3D();

	/**
	 * Canvas used for displaying the rendered {@link #screenImage}.
	 */
	final protected InteractiveDisplay3DCanvas display;

	final protected JFrame frame;

	final protected JSlider sliderTime;

	final protected int numTimePoints;

	protected int currentTimepoint = 0;

	protected int currentLevel = 0;

	/**
	 *
	 * @param width
	 *            width of the display window
	 * @param height
	 *            height of the display window
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display
	 */
	public SpimViewer( final int width, final int height, final Collection< SourceAndConverter< ? > > sources, final int numTimePoints, final Hdf5GlobalCellCache< ? > cache )
	{
		this.sources = new ArrayList< SourceDisplay< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( createSourceAndDisplay( source ) );
		this.numTimePoints = numTimePoints;
		this.cache = cache;
		projector = createProjector();

		box = new MultiBoxOverlay();
		boxInterval = Intervals.createMinSize( 10, 10, 160, 120 );

		display = new InteractiveDisplay3DCanvas( width, height, this, this );

		final MappingThread painterThread = new MappingThread();
		painterThread.setPaintable( display );
		display.setPainterThread( painterThread );

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

		final JSlider sliderScale = new JSlider( JSlider.HORIZONTAL, 1, 8, 1 );
		sliderScale.addKeyListener( display.getTransformEventHandler() );
		sliderScale.addKeyListener( sourceSwitcher );
		sliderScale.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( e.getSource().equals( sliderScale ) )
					display.SWITCH_RESOLUTION_TEST( sliderScale.getValue() );
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
		content.add( sliderScale, BorderLayout.NORTH );
		content.add( sliderMipmap, BorderLayout.EAST );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.addKeyListener( display.getTransformEventHandler() );
		frame.addKeyListener( sourceSwitcher );
		frame.setVisible( true );

		painterThread.start();
	}

	protected < T extends NumericType< T > > SourceDisplay< T > createSourceAndDisplay( final SourceAndConverter< T > soc )
	{
		return new SourceDisplay< T >( soc.source, soc.converter );
	}

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
	 * TODO
	 * The transformation taking care of scale factor between screenimage and component.
	 */
	final protected AffineTransform3D screenScaleTransform = new AffineTransform3D();

	@Override
	public void screenImageChanged( final ARGBScreenImage screenImage, final double xScale, final double yScale )
	{
		this.screenImage = screenImage;
		screenScaleTransform.set( xScale, 0, 0 );
		screenScaleTransform.set( yScale, 1, 1 );
		screenScaleTransform.set( 0.5 * xScale - 0.5, 0, 3 );
		screenScaleTransform.set( 0.5 * yScale - 0.5, 1, 3 );

		virtualScreenInterval = Intervals.createMinSize( 0, 0, ( long ) ( screenImage.dimension( 0 ) / xScale ), ( long ) ( screenImage.dimension( 1 ) / yScale ) );
	}

	@Override
	public void drawScreenImage()
	{
		synchronized( viewerTransform )
		{
			for ( final SourceDisplay< ? > source : sources )
				source.sourceToViewer.set( viewerTransform );
		}
		for ( final SourceDisplay< ? > source : sources )
		{
			source.sourceToViewer.concatenate( source.getSourceTransform( currentTimepoint, currentLevel ) );
			source.sourceToScreen.set( screenScaleTransform );
			source.sourceToScreen.concatenate( source.sourceToViewer );
		}
		if( projector != null )
			projector.map( screenImage );
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
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		synchronized( viewerTransform )
		{
			viewerTransform.set( transform );
		}
	}

	protected void updateTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		projector = createProjector();
		display.requestRepaint();
	}

	// TODO: automatic mipmap switching
	public void updateMipMapLevel( final int level )
	{
		System.out.println( "mip map level " + level );
		currentLevel = level;
		projector = createProjector();
		display.requestRepaint();
	}

	/**
	 * Returns a list of all currently active (visible) sources.
	 *
	 * @return list of all currently active sources.
	 */
	protected ArrayList< SourceDisplay< ? > > getActiveSources()
	{
		final ArrayList< SourceDisplay< ? > > activeSources = new ArrayList< SourceDisplay< ? > >();
		for ( final SourceDisplay< ? > source : sources )
			if ( source.isVisible() )
				activeSources.add( source );
		return activeSources;
	}

//	protected Projector< ARGBType, ARGBType > createEmptyProjector()
//	{
//		return new Projector< ARGBType, ARGBType >()
//		{
//			@Override
//			public void map()
//			{
//				for ( final ARGBType t : screenImage )
//					t.setZero();
//			}
//		};
//	}

	protected < T extends NumericType< T > > AffineRandomAccessible< T, AffineGet > getTransformedSource( final SourceDisplay< T > source )
	{
		final RandomAccessibleInterval< T > img = source.getSource( currentTimepoint, currentLevel );
		final T template = source.getType().createVariable();
		template.setZero();
		final InterpolatorFactory< T, RandomAccessible< T > > interpolatorFactory;
		switch ( interpolation )
		{
		case 0:
			interpolatorFactory = new NearestNeighborInterpolatorFactory< T >();
			break;
		case 1:
		default:
			interpolatorFactory = new NLinearInterpolatorFactory< T >();
			break;
		}
		final AffineRandomAccessible< T, AffineGet > mapping = RealViews.affine(
				Views.interpolate( Views.extendValue( img, template ), interpolatorFactory ),
				source.sourceToScreen );
		return mapping;
	}

	protected < T extends NumericType< T > > RandomAccessible< ARGBType > getConvertedTransformedSource( final SourceDisplay< T > source )
	{
		return Converters.convert( getTransformedSource( source ), source.converter, new ARGBType() );
	}

	protected < T extends NumericType< T > > InterruptibleRenderer< T, ARGBType > createSingleSourceProjector( final SourceDisplay< T > source )
	{
		return new InterruptibleRenderer< T, ARGBType >( getTransformedSource( source ), source.converter, cache );
	}

	protected InterruptibleRenderer< ?, ARGBType > createProjector()
	{
		final ArrayList< SourceDisplay< ? > > activeSources = getActiveSources();
		if ( activeSources.isEmpty() )
			return new InterruptibleRenderer< ARGBType, ARGBType >( new ConstantRandomAccessible< ARGBType >( new ARGBType(), 2 ), new TypeIdentity< ARGBType >(), cache );
		else if ( activeSources.size() == 1 )
			return createSingleSourceProjector( activeSources.get( 0 ) );
		else
		{
			final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( sources.size() );
			for ( final SourceDisplay< ? > source : activeSources )
				accessibles.add( getConvertedTransformedSource( source ) );
			return new InterruptibleRenderer< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), new TypeIdentity< ARGBType >(), cache );
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

	protected int interpolation = 0;

	protected void toggleInterpolation()
	{
		++interpolation;
		interpolation %= 2;
		projector = createProjector();
		display.requestRepaint();
	}

	protected boolean singleSourceMode = true;

	public void toggleSingleSourceMode()
	{
		singleSourceMode = !singleSourceMode;
		projector = createProjector();
		display.requestRepaint();
	}

	protected int currentSource = 0;

	public void toggleVisibility( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < sources.size() )
		{
			final SourceDisplay< ? > source = sources.get( sourceIndex );
			source.setActive( !source.isActive() );
			if ( !singleSourceMode )
				projector = createProjector();
			display.requestRepaint();
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
			projector = createProjector();
			display.requestRepaint();
		}
	}

	protected void selectOrToggleSource( final int sourceIndex, final boolean toggle )
	{
		if( toggle )
			toggleVisibility( sourceIndex );
		else
			setCurrentSource( sourceIndex );
	}
}
