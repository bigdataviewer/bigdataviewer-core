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

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.Projector;
import net.imglib2.display.XYRandomAccessibleProjector;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
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

public class Multi3DViewer implements ScreenImageRenderer, TransformListener3D
{
	public static class SourceAndConverter< T extends NumericType< T > >
	{
		/**
		 * the {@link RandomAccessible} to display
		 */
		final protected RandomAccessible< T > source;

		/**
		 * converts {@link #source} type T to ARGBType for display
		 */
		final protected Converter< T, ARGBType > converter;

		/**
		 * The size of the {@link #source}. This is used for displaying the
		 * navigation wire-frame cube.
		 */
		final protected Interval interval;

		/**
		 * transforms {@link #source} into the viewer coordinate system.
		 */
		final protected AffineTransform3D sourceTransform;

		final protected String name;

		public SourceAndConverter( final RandomAccessible< T > source, final Interval sourceInterval, final Converter< T, ARGBType > converter, final AffineTransform3D sourceTransform, final String name )
		{
			this.source = source;
			this.interval = sourceInterval;
			this.converter = converter;
			this.sourceTransform = sourceTransform;
			this.name = name;
		}
	}

	/**
	 * {@link SourceAndConverter} with some attached properties needed for rendering.
	 */
	protected class SourceDisplay< T extends NumericType< T > > extends SourceAndConverter< T > implements IntervalAndTransform
	{
		/**
		 * Transformation from {@link #source} to {@link #screenImage}. This is a
		 * concatenation of {@link #sourceTransform} and the interactive
		 * viewer {@link #viewerTransform transform}.
		 */
		final protected AffineTransform3D sourceToScreen;

		private boolean isActive;

		public SourceDisplay( final SourceAndConverter< T > source )
		{
			super( source.source, source.interval, source.converter, source.sourceTransform, source.name );
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
			return interval;
		}

		@Override
		public AffineTransform3D getSourceToScreen()
		{
			return sourceToScreen;
		}
	}


	protected ArrayList< SourceDisplay< ? > > sources;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected Projector< ?, ARGBType > projector;

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
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform3D viewerTransform = new AffineTransform3D();

	/**
	 * Canvas used for displaying the rendered {@link #screenImage}.
	 */
	final protected InteractiveDisplay3DCanvas display;

	final protected JFrame frame;

	/**
	 *
	 * @param width
	 *            width of the display window
	 * @param height
	 *            height of the display window
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display
	 */
	public Multi3DViewer( final int width, final int height, final Collection< SourceAndConverter< ? > > sources )
	{
		this.sources = new ArrayList< SourceDisplay< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( createSourceAndDisplay( source ) );

		box = new MultiBoxOverlay();
		boxInterval = Intervals.createMinSize( 10, 10, 160, 120 );

		display = new InteractiveDisplay3DCanvas( width, height, this, this );

		final MappingThread painterThread = new MappingThread();
		painterThread.setPaintable( display );
		display.setPainterThread( painterThread );

		final SourceSwitcher sourceSwitcher = new SourceSwitcher();

		final JSlider slider = new JSlider( JSlider.HORIZONTAL, 0, 700, 0 );
		slider.addKeyListener( display.getTransformEventHandler() );
		slider.addKeyListener( sourceSwitcher );

//		framesPerSecond.addChangeListener(this);

		final GraphicsConfiguration gc = getSuitableGraphicsConfiguration( ARGBScreenImage.ARGB_COLOR_MODEL );
		frame = new JFrame( "multi-angle viewer", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		content.add( slider, BorderLayout.SOUTH );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.addKeyListener( display.getTransformEventHandler() );
		frame.addKeyListener( sourceSwitcher );
		frame.setVisible( true );

		painterThread.start();
	}

	protected < T extends NumericType< T > > SourceDisplay< T > createSourceAndDisplay( final SourceAndConverter< T > soc )
	{
		return new SourceDisplay< T >( soc );
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

	@Override
	public void screenImageChanged( final ARGBScreenImage screenImage )
	{
		this.screenImage = screenImage;
		projector = createProjector();
	}

	@Override
	public void drawScreenImage()
	{
		synchronized( viewerTransform )
		{
			for ( final SourceDisplay< ? > source : sources )
				source.sourceToScreen.set( viewerTransform );
		}
		for ( final SourceDisplay< ? > source : sources )
			source.sourceToScreen.concatenate( source.sourceTransform );
		if( projector != null )
			projector.map();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( !sources.isEmpty() )
		{
			box.paint( ( Graphics2D ) g, sources, screenImage, boxInterval );

			final SourceDisplay< ? > source = sources.get( currentSource );
			g.setFont( new Font( "SansSerif", Font.PLAIN, 12 ) );
			g.drawString( source.name, ( int ) screenImage.dimension( 0 ) / 2, 10 );
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

	/**
	 * Add new source.
	 */
	public void addSource( final SourceAndConverter< ? > source )
	{
		sources.add( createSourceAndDisplay( source ) );
		setCurrentSource( sources.size() - 1 );
	}

	/**
	 * Returns a list of all currently active (visible) sources.
	 *
	 * @return list of all currently active sources.
	 */
	final ArrayList< SourceDisplay< ? > > getActiveSources()
	{
		final ArrayList< SourceDisplay< ? > > activeSources = new ArrayList< SourceDisplay< ? > >();
		for ( final SourceDisplay< ? > source : sources )
			if ( source.isVisible() )
				activeSources.add( source );
		return activeSources;
	}

	protected Projector< ARGBType, ARGBType > createEmptyProjector()
	{
		return new Projector< ARGBType, ARGBType >()
		{
			@Override
			public void map()
			{
				for ( final ARGBType t : screenImage )
					t.setZero();
			}
		};
	}

	protected < T extends NumericType< T > > AffineRandomAccessible< T, AffineGet > getTransformedSource( final SourceDisplay< T > source )
	{
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
				Views.interpolate( source.source, interpolatorFactory ),
				source.sourceToScreen );
		return mapping;
	}

	protected < T extends NumericType< T > > RandomAccessible< ARGBType > getConvertedTransformedSource( final SourceDisplay< T > source )
	{
		return Converters.convert( getTransformedSource( source ), source.converter, new ARGBType() );
	}

	protected < T extends NumericType< T > > XYRandomAccessibleProjector< T, ARGBType > createSingleSourceProjector( final SourceDisplay< T > source )
	{
		return new XYRandomAccessibleProjector< T, ARGBType >( getTransformedSource( source ), screenImage, source.converter );
	}

	protected Projector< ?, ARGBType > createProjector()
	{
		final ArrayList< SourceDisplay< ? > > activeSources = getActiveSources();
		if ( activeSources.isEmpty() )
			return createEmptyProjector();
		else if ( activeSources.size() == 1 )
			return createSingleSourceProjector( activeSources.get( 0 ) );
		else
		{
			final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( sources.size() );
			for ( final SourceDisplay< ? > source : sources )
				if ( source.isVisible() )
					accessibles.add( getConvertedTransformedSource( source ) );
			return new XYRandomAccessibleProjector< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), screenImage, new TypeIdentity< ARGBType >() );
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
			// TODO:
//			else if ( e.getKeyCode() == KeyEvent.VK_B )
//				setCurrentSource( currentSource - 1, selectSingleSource );
//			else if ( e.getKeyCode() == KeyEvent.VK_N )
//				setCurrentSource( currentSource + 1, selectSingleSource );
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
