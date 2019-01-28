package bdv.tools.boundingbox;

import bdv.tools.boundingbox.BoxSelectionPanel.Box;
import bdv.util.ModifiableInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class TransformedBoxModel extends AbstractTransformedBoxModel implements Box
{
	private final ModifiableInterval interval;

	public TransformedBoxModel(
			final ModifiableInterval interval,
			final AffineTransform3D transform )
	{
		super( transform );
		this.interval = interval;
	}

	@Override
	public Interval getInterval()
	{
		return interval;
	}

	@Override
	public void setInterval( final Interval i )
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
		setInterval( Intervals.smallestContainingInterval( i ) );
	}
}
