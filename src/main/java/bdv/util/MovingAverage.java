package bdv.util;

import java.util.Arrays;

/**
 * Maintains a moving average over the last {@code width} values {@link #add
 * added}. The average can be {@link #init initialized} to some value (or starts
 * as 0, i.e., as if {@code width} 0 values had been added)
 */
public class MovingAverage
{
	private final double[] values;

	private final int width;

	private int index = 0;

	private double average;

	public MovingAverage( final int width )
	{
		values = new double[ width ];
		this.width = width;
	}

	public void init( final double initialValue )
	{
		Arrays.fill( values, initialValue );
		average = initialValue;
	}

	public void add( final double value )
	{
		average = average + ( value - values[ index ] ) / width;
		values[ index ] = value;
		index = ( index + 1 ) % width;
	}

	public double getAverage()
	{
		return average;
	}
}
