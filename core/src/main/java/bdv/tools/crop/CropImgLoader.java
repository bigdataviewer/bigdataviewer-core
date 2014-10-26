package bdv.tools.crop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import bdv.util.IntervalBoundingBox;
import bdv.viewer.Source;

/**
 * This {@link ImgLoader} provides views and transformations into a cropped
 * region of a data-set (provided by list of {@link Source Sources}).
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class CropImgLoader implements BasicImgLoader< UnsignedShortType >
{
	private final ArrayList< Source< UnsignedShortType > > sources;

	private final AffineTransform3D globalToCropTransform;

	private final RealInterval cropInterval;

	private final Map< Integer, Integer > timepointIdToTimepointIndex;

	private final Map< Integer, Integer > setupIdToSourceIndex;

	public CropImgLoader(
			final ArrayList< Source< UnsignedShortType > > sources,
			final AffineTransform3D globalToCropTransform,
			final RealInterval cropInterval,
			final Map< Integer, Integer > timepointIdToTimepointIndex,
			final Map< Integer, Integer > setupIdToSourceIndex )
	{
		this.sources = sources;
		this.globalToCropTransform = globalToCropTransform;
		this.cropInterval = cropInterval;
		this.timepointIdToTimepointIndex = timepointIdToTimepointIndex;
		this.setupIdToSourceIndex = setupIdToSourceIndex;
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
	{
		final Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > pair = cropView( view );
		return pair.getA();
	}

	@Override
	public UnsignedShortType getImageType()
	{
		return new UnsignedShortType();
	}

	public AffineTransform3D getCroppedTransform( final ViewId view )
	{
		final Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > pair = cropView( view );
		return pair.getB();
	}

	private Pair< RandomAccessibleInterval< UnsignedShortType >, AffineTransform3D > cropView( final ViewId view )
	{
		final int setupId = view.getViewSetupId();
		final int timepointId = view.getTimePointId();

		final int sourceIndex = setupIdToSourceIndex.get( setupId );
		final int timepointIndex = timepointIdToTimepointIndex.get( timepointId );

		return crop( globalToCropTransform, cropInterval, sources.get( sourceIndex ), timepointIndex );
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
	public static < T extends NumericType< T > > Pair< RandomAccessibleInterval< T >, AffineTransform3D > crop(
			final AffineTransform3D globalToCropTransform,
			final RealInterval interval,
			final Source< T > source,
			final int timepoint )
	{
		final int n = interval.numDimensions();
		final AffineTransform3D cropToGlobal = globalToCropTransform.inverse();

		final AffineTransform3D sourceToGlobal = new AffineTransform3D();
		source.getSourceTransform( timepoint, 0, sourceToGlobal );
		final AffineTransform3D globalToSource = sourceToGlobal.inverse();
		final AffineTransform3D cropToSource = globalToSource.copy();
		cropToSource.concatenate( cropToGlobal );

		final ArrayList< RealPoint > sourceCorners = new ArrayList< RealPoint >();
		for ( final RealLocalizable corner : IntervalBoundingBox.getCorners( interval ) )
		{
			final RealPoint sourceCorner = new RealPoint( n );
			cropToSource.apply( corner, sourceCorner );
			sourceCorners.add( sourceCorner );
		}
		final RandomAccessibleInterval< T > sourceImg = source.getSource( timepoint, 0 );
		final Interval cropBoundingBox = Intervals.smallestContainingInterval( IntervalBoundingBox.getBoundingBox( sourceCorners ) );
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
}
