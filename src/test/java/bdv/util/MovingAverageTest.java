package bdv.util;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovingAverageTest
{
	@Test
	public void testAverage()
	{
		final double[] values = new double[ 1000 ];
		Arrays.setAll( values, i -> Math.random() );

		final int width = 3;
		final MovingAverage avg = new MovingAverage( width );
		avg.init( 0 );

		for ( int i = 0; i < values.length; ++i )
		{
			avg.add( values[ i ] );
			double expected = average( values, i - width + 1, i + 1 );
			assertEquals( expected, avg.getAverage(), 1e-6 );
		}
	}

	private static double average( final double[] values, final int fromIndex, final int toIndex )
	{
		final int numValues = toIndex - fromIndex;
		double sum = 0;
		for ( int i = fromIndex; i < toIndex; i++ )
			sum += i < 0 ? 0 : values[ i ];
		return sum / numValues;
	}
}
