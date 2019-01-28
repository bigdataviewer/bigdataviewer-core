package bdv.tools.boundingbox;

import bdv.util.ModifiableRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class TransformedRealBoxModel extends AbstractTransformedBoxModel implements RealBoxSelectionPanel.RealBox
{
	private final ModifiableRealInterval interval;

	public TransformedRealBoxModel(
			final ModifiableRealInterval interval,
			final AffineTransform3D transform )
	{
		super( transform );
		this.interval = interval;
	}

	@Override
	public RealInterval getInterval()
	{
		return interval;
	}

	@Override
	public void setInterval( final RealInterval i )
	{
		if ( ! equals( interval, i ) )
		{
			interval.set( i );
			notifyIntervalChanged();
		}
	}

	private static boolean equals( final RealInterval a, final RealInterval b )
	{
		if ( a.numDimensions() != b.numDimensions() )
			return false;

		for ( int d = 0; d < a.numDimensions(); ++d )
			if ( a.realMin( d ) != b.realMin( d ) || a.realMax( d ) != b.realMax( d ) )
				return false;

		return true;
	}
}
