package viewer.render.labelling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import viewer.display.LabelingTypeARGBConverter;
import viewer.render.Interpolation;
import viewer.render.Source;

/**
 * A {@link Source} that wraps a {@link NativeImgLabeling}, build from another
 * source and copying its dimension and transform at the lowest level only (=0).
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2013
 * @author Tobias Pietzsch
 * @author Stephan Preibisch
 * 
 */
public class LabelingSource< T > implements Source< ARGBType >
{
	private int currentTimepoint;

	private NativeImgLabeling< Integer, IntType > currentSource;

	private final Source< T > imgSource;

	private final String name;

	private final LabelingTypeARGBConverter< Integer > converter;

	final protected static int numInterpolationMethods = 2;

	final protected static int iNearestNeighborMethod = 0;

	final protected static int iNLinearMethod = 1;

	final protected InterpolatorFactory< ARGBType, RandomAccessible< ARGBType >>[] interpolatorFactories;

	private volatile int oldAlpha = -1;

	private int currentAlpha;

	/**
	 * The map that stores the labeling for each time point. Its values are all
	 * <code>null</code> until the target time-point is initialized.
	 */
	private final Map< Integer, NativeImgLabeling< Integer, IntType >> labelings;

	private final AffineTransform3D currentTranform = new AffineTransform3D();

	private final int level;

	@SuppressWarnings( "unchecked" )
	public LabelingSource( final Source< T > imgSource, final int level )
	{
		this.imgSource = imgSource;
		this.level = level;
		name = imgSource.getName() + " annotations";
		converter = new LabelingTypeARGBConverter< Integer >( new HashMap< List< Integer >, ARGBType >() );
		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< ARGBType >();
		interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< ARGBType >();

		labelings = new HashMap< Integer, NativeImgLabeling< Integer, IntType >>();

		loadTimepoint( 0 );
	}

	public LabelingSource( final Source< T > imgSource )
	{
		this( imgSource, 0 );
	}

	@Override
	public boolean isPresent( final int t )
	{
		return imgSource.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getSource( final int t, final int level )
	{
		if ( t != currentTimepoint )
		{
			loadTimepoint( t );
		}
		if ( currentAlpha != oldAlpha )
		{
			updateColorTable();
		}
		return Converters.convert( ( RandomAccessibleInterval< LabelingType< Integer >> ) currentSource, converter, new ARGBType() );
	}

	@Override
	public RealRandomAccessible< ARGBType > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return Views.interpolate( Views.extendValue( getSource( t, level ), new ARGBType() ), interpolatorFactories[ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ] );
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

	/**
	 * Returns the mipmap level of the img source this labeling is built upon.
	 * 
	 * @return the mipmap level.
	 */
	public int getLevel()
	{
		return level;
	}

	/**
	 * Exposes the {@link Source} this labeling source is built on.
	 * 
	 * @return a {@link Source}.
	 */
	public Source< T > getImgSource()
	{
		return imgSource;
	}

	private void loadTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
		if ( isPresent( timepoint ) )
		{

			currentTranform.set( imgSource.getSourceTransform( timepoint, level ) );

			NativeImgLabeling< Integer, IntType > labeling = labelings.get( Integer.valueOf( timepoint ) );
			if ( null == labeling )
			{
				final Dimensions sourceDimensions = imgSource.getSource( timepoint, level );
				labeling = newLabeling( sourceDimensions );
				labelings.put( Integer.valueOf( timepoint ), labeling );
			}
			currentSource = labeling;
			updateColorTable();
		}
		else
		{
			currentSource = null;
		}
	}

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		if ( currentTimepoint != t )
		{
			loadTimepoint( t );
		}
		return currentTranform;
	}

	@Override
	public ARGBType getType()
	{
		return new ARGBType();
	}

	@Override
	public String getName()
	{
		return name;
	}

	public NativeImgLabeling< Integer, IntType > getLabeling( final int t )
	{
		NativeImgLabeling< Integer, IntType > target;
		if ( isPresent( t ) )
		{
			target = labelings.get( Integer.valueOf( t ) );
			if ( null == target )
			{
				final Dimensions sourceDimensions = imgSource.getSource( t, level );
				target = newLabeling( sourceDimensions );
				labelings.put( Integer.valueOf( t ), target );
			}
		}
		else
		{
			target = null;
		}
		return target;
	}

	/**
	 * Sets the transparency (alpha value) for this source.
	 * 
	 * @param alpha
	 *            an <code>int</code>, ranging from 0 to 255.
	 */
	public void setAlpha( final int alpha )
	{
		this.currentAlpha = alpha;
	}

	/**
	 * Returns the current transparency (alpha value) for this source.
	 * 
	 * @return an <code>int</code>, ranging from 0 to 255.
	 */
	public int getAlpha()
	{
		return currentAlpha;
	}

	public void updateColorTable()
	{
		final int a = currentAlpha;
		final HashMap< List< Integer >, ARGBType > colorTable = new HashMap< List< Integer >, ARGBType >();
		final LabelingMapping< Integer > mapping = currentSource.getMapping();
		final int numLists = mapping.numLists();
		final Random random = new Random( 1 );
		for ( int i = 0; i < numLists; ++i )
		{
			final List< Integer > list = mapping.listAtIndex( i );
			final int r = random.nextInt( 256 );
			final int g = random.nextInt( 256 );
			final int b = random.nextInt( 256 );
			colorTable.put( list, new ARGBType( ARGBType.rgba( r, g, b, a ) ) );
		}
		colorTable.put( mapping.emptyList(), new ARGBType( 0 ) );
		converter.setColorTable( colorTable );
		oldAlpha = a;
	}

	private static final NativeImgLabeling< Integer, IntType > newLabeling( final Dimensions dimensions )
	{
		final NtreeImgFactory< IntType > factory = new NtreeImgFactory< IntType >();
		final Img< IntType > img = factory.create( dimensions, new IntType() );
		final NativeImgLabeling< Integer, IntType > labeling = new NativeImgLabeling< Integer, IntType >( img );
		return labeling;
	}

}
