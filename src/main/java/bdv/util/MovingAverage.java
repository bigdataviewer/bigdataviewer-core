package bdv.util;

import java.util.Arrays;

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
