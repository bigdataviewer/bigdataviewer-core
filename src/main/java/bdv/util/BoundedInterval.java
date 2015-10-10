/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util;

/**
 * A interval consisting of {@link BoundedValue} minimum and maximum that can
 * span any interval in a given range. The UI can listen to changes by adding
 * listeners can attach to the
 * {@link BoundedValue#setUpdateListener(UpdateListener) minimum and maximum}
 * values and/or overriding the {@link #updateInterval(int, int)} method.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BoundedInterval
{
	private final BoundedValue minValue;

	private final BoundedValue maxValue;

	/**
	 * minimum interval size - 1
	 */
	protected final int minSizeMinusOne;

	/**
	 * @param rangeMin
	 *            lower bound of the allowed range. the interval minimum cannot
	 *            be lower than this.
	 * @param rangeMax
	 *            upper bound of the allowed range. the interval maximum cannot
	 *            be lower than this.
	 * @param initialMin
	 *            initial interval minimum.
	 * @param initialMax
	 *            initial interval maximum.
	 * @param minIntervalSize
	 *            interval is forced to have this size at least.
	 */
	public BoundedInterval( final int rangeMin, final int rangeMax, final int initialMin, final int initialMax, final int minIntervalSize )
	{
		minSizeMinusOne = minIntervalSize - 1;

		minValue = new BoundedValue( rangeMin, rangeMax - minSizeMinusOne, initialMin )
		{
			@Override
			public void setCurrentValue( final int value )
			{
				super.setCurrentValue( value );
				final int min = minValue.getCurrentValue();
				int max = maxValue.getCurrentValue();
				if ( min > max - minSizeMinusOne )
				{
					max = min + minSizeMinusOne;
					maxValue.setCurrentValue( max );
				}
				updateInterval( min, max );
			}
		};

		maxValue = new BoundedValue( rangeMin + minSizeMinusOne, rangeMax, initialMax )
		{
			@Override
			public void setCurrentValue( final int value )
			{
				super.setCurrentValue( value );
				int min = minValue.getCurrentValue();
				final int max = maxValue.getCurrentValue();
				if ( min > max - minSizeMinusOne )
				{
					min = max - minSizeMinusOne;
					minValue.setCurrentValue( min );
				}
				updateInterval( min, max );
			}
		};
	}

	/**
	 * This is called when the {@link #getMinBoundedValue() min} or
	 * {@link #getMaxBoundedValue() max} values change. The default
	 * implementation does nothing but derived classes can use it to trigger UI
	 * updates, etc.
	 *
	 * @param min
	 *            the new minimum of the interval
	 * @param max
	 *            the new maximum of the interval
	 */
	protected void updateInterval( final int min, final int max )
	{}

	/**
	 * Get the current minimum of the interval.
	 *
	 * @return the current minimum of the interval.
	 */
	public BoundedValue getMinBoundedValue()
	{
		return minValue;
	}

	/**
	 * Get the current maximum of the interval.
	 *
	 * @return the current maximum of the interval.
	 */
	public BoundedValue getMaxBoundedValue()
	{
		return maxValue;
	}

	/**
	 * Get the current minimum of the allowed range. (The interval (
	 * {@link #getMinBoundedValue()}, {@link #getMaxBoundedValue()}) is be a
	 * non-empty interval within the allowed range.)
	 *
	 * @return the current minimum of the allowed range.
	 */
	public int getRangeMin()
	{
		return minValue.getRangeMin();
	}

	/**
	 * Get the current maximum of the allowed range. (The interval (
	 * {@link #getMinBoundedValue()}, {@link #getMaxBoundedValue()}) is be a
	 * non-empty interval within the allowed range.)
	 *
	 * @return the current maximum of the allowed range.
	 */
	public int getRangeMax()
	{
		return maxValue.getRangeMax();
	}

	/**
	 * Set the allowed range. The interval ({@link #getMinBoundedValue()},
	 * {@link #getMaxBoundedValue()}) is enforced to be a non-empty interval
	 * within the allowed range.
	 *
	 * @param min
	 *            minimum of the allowed range.
	 * @param max
	 *            maximum of the allowed range.
	 */
	public void setRange( final int min, final int max )
	{
		assert min < max - minSizeMinusOne;
		minValue.setRange( min, max - minSizeMinusOne );
		maxValue.setRange( min + minSizeMinusOne, max );
		final int currentMin = minValue.getCurrentValue();
		final int currentMax = maxValue.getCurrentValue();
		if ( currentMin > currentMax - minSizeMinusOne )
		{
			if ( currentMax == max )
				minValue.setCurrentValue( currentMax - minSizeMinusOne );
			else
				maxValue.setCurrentValue( currentMin + minSizeMinusOne );
		}
	}
}
