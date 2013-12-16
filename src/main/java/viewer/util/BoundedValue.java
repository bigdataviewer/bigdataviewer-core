package viewer.util;

/**
 * A <code>int</code> variable that can take any value in a given range. A
 * {@link #setUpdateListener(UpdateListener) listener} is notified when the
 * value or its allowed range is changed.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class BoundedValue
{
	private int rangeMin;

	private int rangeMax;

	private int currentValue;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	public BoundedValue( final int rangeMin, final int rangeMax, final int currentValue )
	{
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.currentValue = currentValue;
		updateListener = null;
	}

	public int getRangeMin()
	{
		return rangeMin;
	}

	public int getRangeMax()
	{
		return rangeMax;
	}

	public int getCurrentValue()
	{
		return currentValue;
	}

	public void setRange( final int min, final int max )
	{
		assert min <= max;
		rangeMin = min;
		rangeMax = max;
		currentValue = Math.min( Math.max( currentValue, min ), max );

		if ( updateListener != null )
			updateListener.update();
	}

	public void setCurrentValue( final int value )
	{
		currentValue = value;

		if ( currentValue < rangeMin )
			currentValue = rangeMin;
		else if ( currentValue > rangeMax )
			currentValue = rangeMax;

		if ( updateListener != null )
			updateListener.update();
	}

	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}
}