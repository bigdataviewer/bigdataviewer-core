package bdv.tools.boundingbox;

import bdv.util.ModifiableInterval;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class TransformedBoxModel extends AbstractTransformedBoxModel
{
	private final ModifiableInterval interval;

	public TransformedBoxModel(
			final Interval interval,
			final AffineTransform3D transform )
	{
		this( new ModifiableInterval( interval ), transform );
	}

	public TransformedBoxModel(
			final ModifiableInterval interval,
			final AffineTransform3D transform )
	{
		super( transform );
		this.interval = interval;
	}

	@Override
	public RealInterval getInterval()
	{
		return enlarge( interval );
	}

	private void setDiscreteInterval( final Interval i )
	{
		if ( ! Intervals.equals( interval, i ) )
		{
			interval.set( i );
			notifyIntervalChanged();
		}
	}

	@Override
	public void setInterval( final RealInterval i )
	{
		setDiscreteInterval( round( shrink( i ) ) );
	}

	private final BoxSelectionPanel.Box box = new BoxSelectionPanel.Box()
	{
		@Override
		public Interval getInterval()
		{
			return interval;
		}

		@Override
		public void setInterval( final Interval i )
		{
			setDiscreteInterval( i );
		}
	};

	public BoxSelectionPanel.Box box()
	{
		return box;
	}

	private static RealInterval enlarge( final RealInterval interval )
	{
		return expand( interval, 0.5 );
	}

	private static RealInterval shrink( final RealInterval interval )
	{
		return expand( interval, -0.5 );
	}

	/**
	 * Grow/shrink an interval in all dimensions.
	 *
	 * Create a {@link FinalRealInterval} , which is the input interval plus border
	 * on every side, in every dimension.
	 *
	 * @param interval
	 *            the input interval
	 * @param border
	 *            how much to add on every side
	 * @return expanded interval
	 */
	// TODO: move to imglib2 Intervals
	private static FinalRealInterval expand( final RealInterval interval, final double border )
	{
		final int n = interval.numDimensions();
		final double[] min = new double[ n ];
		final double[] max = new double[ n ];
		interval.realMin( min );
		interval.realMax( max );
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] -= border;
			max[ d ] += border;
		}
		return new FinalRealInterval( min, max );
	}

	/**
	 * Compute {@link Interval} by rounding {@link RealInterval} min/max to nearest integer.
	 *
	 * @param ri
	 *            input interval.
	 * @return the integer interval obtained by rounding input interval min/max.
	 */
	// TODO: move to imglib2 Intervals
	private static Interval round( final RealInterval ri )
	{
		final int n = ri.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = Math.round( ri.realMin( d ) );
			max[ d ] = Math.round( ri.realMax( d ) );
		}
		return new FinalInterval( min, max );
	}
}
