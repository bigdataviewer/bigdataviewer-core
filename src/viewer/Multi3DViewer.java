package viewer;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.XYRandomAccessibleProjector;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.AbstractInteractiveDisplay3D;
import net.imglib2.ui.ScreenImageRenderer;
import net.imglib2.ui.TransformListener3D;
import net.imglib2.ui.swing.SwingInteractiveDisplay3D;

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
		 * transforms {@link #source} into the viewer coordinate system.
		 */
		final protected AffineTransform3D sourceTransform;

		final protected String name;

		public SourceAndConverter( final RandomAccessible< T > source, final Converter< T, ARGBType > converter, final AffineTransform3D sourceTransform, final String name )
		{
			this.source = source;
			this.converter = converter;
			this.sourceTransform = sourceTransform;
			this.name = name;
		}
	}

	protected ArrayList< SourceAndConverter< ? > > sources;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected XYRandomAccessibleProjector< ?, ARGBType > projector;

	/**
	 * render target
	 */
	protected ARGBScreenImage screenImage;

	/**
	 * A transformation to apply to {@link #source} before applying the
	 * interactive viewer {@link #viewerTransform transform}.
	 */
	final protected AffineTransform3D sourceTransform;

	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform3D viewerTransform = new AffineTransform3D();

	/**
	 * Transformation from {@link #source} to {@link #screenImage}. This is a
	 * concatenation of {@link #sourceTransform} and the interactive
	 * viewer {@link #viewerTransform transform}.
	 */
	final protected AffineTransform3D sourceToScreen = new AffineTransform3D();

	/**
	 * Window used for displaying the rendered {@link #screenImage}.
	 */
	final protected AbstractInteractiveDisplay3D display;

	protected int currentSource = 0;

	/**
	 *
	 * @param width
	 *            width of the display window
	 * @param height
	 *            height of the display window
	 * @param source
	 *            the {@link RandomAccessible} to display
	 * @param sourceInterval
	 *            size of the source. This is only for displaying a navigation
	 *            wire-frame cube.
	 * @param converter
	 *            converts {@link #source} type T to ARGBType for display
	 * @param initialTransform
	 *            initial transformation to apply to the {@link #source}
	 * @param yScale
	 *            scale factor for the Y axis, that is, the pixel width/height
	 *            ratio.
	 * @param zScale
	 *            scale factor for the Z axis, that is, the pixel width/depth
	 *            ratio.
	 * @param currentSlice
	 *            which slice to display initially.
	 */
	public Multi3DViewer( final int width, final int height, final Collection< SourceAndConverter< ? > > sources, final Interval sourceInterval )
	{
		this.sources = new ArrayList< SourceAndConverter< ? > >( sources );
		this.sourceTransform = new AffineTransform3D();

//		display = new ImagePlusInteractiveDisplay3D( width, height, sourceInterval, sourceTransform, this, this );
		display = new SwingInteractiveDisplay3D( width, height, sourceInterval, sourceTransform, this, this );
		display.addHandler( new SourceSwitcher() );
		display.startPainter();
	}

	@Override
	public void screenImageChanged( final ARGBScreenImage screenImage )
	{
		this.screenImage = screenImage;
		projector = createProjector( sources.get( currentSource ) );
	}

	@Override
	public void drawScreenImage()
	{
		synchronized( viewerTransform )
		{
			sourceToScreen.set( viewerTransform );
		}
		sourceToScreen.concatenate( sourceTransform );
		projector.map();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( !sources.isEmpty() )
		{
			g.setFont( new Font( "SansSerif", Font.PLAIN, 12 ) );
			g.drawString( sources.get( currentSource ).name, ( int ) screenImage.dimension( 0 ) / 2, 10 );
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
		sources.add( source );
		setCurrentSource( sources.size() - 1 );
	}

	/**
	 * Set the index of the source to display.
	 */
	public void setCurrentSource( final int sourceIndex )
	{
		currentSource = sourceIndex < 0 ? 0 : ( sourceIndex >= sources.size() ? sources.size() - 1 : sourceIndex );
		if ( currentSource >= 0 )
		{
			sourceTransform.set( sources.get( currentSource ).sourceTransform );
			projector = createProjector( sources.get( currentSource ) );
		}
		display.requestRepaint();
	}

	protected < T extends NumericType< T > > XYRandomAccessibleProjector< T, ARGBType > createProjector( final SourceAndConverter< T > source )
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
		final Interpolant< T, RandomAccessible< T > > interpolant = new Interpolant< T, RandomAccessible< T > >( source.source, interpolatorFactory );
		final AffineRandomAccessible< T, AffineGet > mapping = new AffineRandomAccessible< T, AffineGet >( interpolant, sourceToScreen.inverse() );
		return new XYRandomAccessibleProjector< T, ARGBType >( mapping, screenImage, source.converter );
	}

	protected class SourceSwitcher implements KeyListener
	{
		@Override
		public void keyTyped( final KeyEvent e )
		{}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_I )
				toggleInterpolation();
			else if ( e.getKeyCode() == KeyEvent.VK_1 )
				setCurrentSource( 0 );
			else if ( e.getKeyCode() == KeyEvent.VK_2 )
				setCurrentSource( 1 );
			else if ( e.getKeyCode() == KeyEvent.VK_3 )
				setCurrentSource( 2 );
			else if ( e.getKeyCode() == KeyEvent.VK_4 )
				setCurrentSource( 3 );
			else if ( e.getKeyCode() == KeyEvent.VK_5 )
				setCurrentSource( 4 );
			else if ( e.getKeyCode() == KeyEvent.VK_6 )
				setCurrentSource( 5 );
			else if ( e.getKeyCode() == KeyEvent.VK_7 )
				setCurrentSource( 6 );
			else if ( e.getKeyCode() == KeyEvent.VK_8 )
				setCurrentSource( 7 );
			else if ( e.getKeyCode() == KeyEvent.VK_9 )
				setCurrentSource( 8 );
			else if ( e.getKeyCode() == KeyEvent.VK_0 )
				setCurrentSource( 9 );
			else if ( e.getKeyCode() == KeyEvent.VK_B )
				setCurrentSource( currentSource - 1 );
			else if ( e.getKeyCode() == KeyEvent.VK_N )
				setCurrentSource( currentSource + 1 );
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
		if ( currentSource >= sources.size() )
			currentSource = sources.size() - 1;
		if ( currentSource >= 0 )
			projector = createProjector( sources.get( currentSource ) );
		display.requestRepaint();
	}
}
