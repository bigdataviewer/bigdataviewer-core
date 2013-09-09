package viewer.crop;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.Pair;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import org.jdom2.Element;

import viewer.render.Source;

/**
 * This {@link ImgLoader} provides views and transformations into a cropped
 * region of a data-set (provided by list of {@link Source Sources}).
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class CropImgLoader implements ImgLoader
{
	final private ArrayList< Source< UnsignedShortType > > sources;

	final private AffineTransform3D globalToCropTransform;

	final private RealInterval cropInterval;

	final private ArrayList< Integer > timepointMap;

	public CropImgLoader( final ArrayList< Source< UnsignedShortType > > sources, final AffineTransform3D globalToCropTransform, final RealInterval cropInterval, final ArrayList< Integer > timepointMap )
	{
		this.sources = sources;
		this.globalToCropTransform = globalToCropTransform;
		this.cropInterval = cropInterval;
		this.timepointMap = timepointMap;
	}

	/**
	 * not implemented.
	 */
	@Override
	public void init( final Element elem, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	/**
	 * not implemented.
	 */
	@Override
	public Element toXml( final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	/**
	 * not implemented.
	 */
	@Override
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	private Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > cropView( final View view )
	{
		final int setup = view.getSetupIndex();
		final int timepoint = timepointMap.get( view.getTimepointIndex() );
		return crop( globalToCropTransform, cropInterval, sources.get( setup ), timepoint );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > pair = cropView( view );
		return pair.getA();
	}

	public AffineTransform3D getCroppedTransform( final View view )
	{
		final Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > pair = cropView( view );
		return pair.getB();
	}

	/**
	 * Crop a region from a {@link Source}.
	 *
	 * @param globalToCropTransform
	 *            transform from global into crop-local coordinates.
	 * @param interval
	 *            the interval (in crop-local space) to crop.
	 * @param source
	 *            which source to crop from.
	 * @param timepoint
	 *            which timepoint to crop from.
	 * @return a zero-min view of the cropped region and a transform from that
	 *         view into global coordinates.
	 */
	public static < T extends NumericType< T > > Pair< RandomAccessibleInterval< T >, AffineTransform3D > crop( final AffineTransform3D globalToCropTransform, final RealInterval interval, final Source< T > source, final int timepoint )
	{
		final int n = interval.numDimensions();
		final AffineTransform3D cropToGlobal = globalToCropTransform.inverse();

		final AffineTransform3D sourceToGlobal = source.getSourceTransform( timepoint, 0 );
		final AffineTransform3D globalToSource = sourceToGlobal.inverse();
		final AffineTransform3D cropToSource = globalToSource.copy();
		cropToSource.concatenate( cropToGlobal );

		final ArrayList< RealPoint > sourceCorners = new ArrayList< RealPoint >();
		for ( final RealLocalizable corner : getCorners( interval ) )
		{
			final RealPoint sourceCorner = new RealPoint( n );
			cropToSource.apply( corner, sourceCorner );
			sourceCorners.add( sourceCorner );
		}
		final RandomAccessibleInterval< T > sourceImg = source.getSource( timepoint, 0 );
		final Interval cropBoundingBox = roundUp( getBoundingBox( sourceCorners ) );
		Interval sourceInterval = Intervals.intersect( cropBoundingBox, sourceImg );

		final RandomAccessibleInterval< T > croppedSourceImg;
		final AffineTransform3D croppedSourceTransform = new AffineTransform3D();

		if ( Intervals.isEmpty( sourceInterval ) )
		{
			final long[] minsize = new long[ n * 2 ];
			for ( int d = 0; d < n; ++d )
				minsize[ d ] = cropBoundingBox.min( d ) + cropBoundingBox.dimension( d ) / 2;
			Arrays.fill( minsize, n, n * 2, 1 );
			sourceInterval = Intervals.createMinSize( minsize );
		}

		croppedSourceImg = Views.zeroMin( Views.interval( Views.extendBorder( sourceImg ), sourceInterval ) );
		croppedSourceTransform.set(
			1, 0, 0, sourceInterval.min( 0 ),
			0, 1, 0, sourceInterval.min( 1 ),
			0, 0, 1, sourceInterval.min( 2 ) );
		croppedSourceTransform.preConcatenate( sourceToGlobal );

		return new ValuePair< RandomAccessibleInterval<T>, AffineTransform3D >( croppedSourceImg, croppedSourceTransform );
	}

	/**
	 * Expand the given interval to the nearest integer interval.
	 *
	 * @return smallest enclosing integer interval.
	 */
	public static Interval roundUp( final RealInterval realInterval )
	{
		final int n = realInterval.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = ( long ) Math.floor( realInterval.realMin( d ) );
			max[ d ] = ( long ) Math.ceil( realInterval.realMax( d ) );
		}
		return new FinalInterval( min, max );
	}

	/**
	 * get bounding box of a list of points.
	 */
	public static < P extends RealLocalizable > RealInterval getBoundingBox( final List< P > points )
	{
		assert !points.isEmpty();

		final P p0 = points.get( 0 );
		final int n = p0.numDimensions();
		final double[] min = new double[ n ];
		p0.localize( min );
		final double[] max = min.clone();
		for ( final P point : points )
		{
			for ( int d = 0; d < n; ++d )
			{
				final double p = point.getDoublePosition( d );
				if (p < min[ d ])
					min[ d ] = p;
				else if (p > max[ d ])
					max[ d ] = p;
			}
		}
		return new FinalRealInterval( min, max );
	}

	/**
	 * get "corners" of an interval as a list of points.
	 */
	public static List< RealLocalizable > getCorners( final RealInterval interval )
	{
		final ArrayList< RealLocalizable > corners = new ArrayList< RealLocalizable >();
		final int n = interval.numDimensions();
		final int[] tmp = new int[ n ];
		Arrays.fill( tmp, 2 );
		final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( tmp );
		while ( i.hasNext() )
		{
			i.fwd();
			final RealPoint p = new RealPoint( n );
			for ( int d = 0; d < n; ++d )
				p.setPosition( i.getIntPosition( d ) == 0 ? interval.realMin( d ) : interval.realMax( d ), d );
			corners.add( p );
		}
		return corners;
	}
}
