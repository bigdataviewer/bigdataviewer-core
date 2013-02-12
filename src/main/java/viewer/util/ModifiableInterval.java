package viewer.util;

import net.imglib2.AbstractInterval;
import net.imglib2.Interval;

public class ModifiableInterval extends AbstractInterval
{
	public ModifiableInterval( final int numDimensions )
	{
		super( new long[ numDimensions ], new long[ numDimensions ] );
	}

	public ModifiableInterval( final Interval interval )
	{
		super( interval );
	}

	public void set( final Interval interval )
	{
		assert interval.numDimensions() == n;
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = interval.min( d );
			max[ d ] = interval.max( d );
		}
	}
}
