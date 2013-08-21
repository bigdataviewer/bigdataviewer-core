package viewer.gui.brightness;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An <code>int</code> interval. The {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval are stored as
 * {@link BoundedValue}. The min and max can be changed to span any non-empty
 * interval within the range ({@link #getRangeMin()}, {@link #getRangeMax()}).
 * This range can be {@link #setRange(int, int) modified} as well.
 * <p>
 * Some {@link ConverterSetup ConverterSetups} can be
 * {@link #addSetup(ConverterSetup) linked}. They will have their display range
 * set according to {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval.
 * <p>
 * An {@link UpdateListener} (usually a GUI component) can be notified about
 * changes.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MinMaxGroup
{
	private final int fullRangeMin;

	private final int fullRangeMax;

	private final BoundedValue minValue;

	private final BoundedValue maxValue;

	final Set< ConverterSetup > setups;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	public MinMaxGroup( final int fullRangeMin, final int fullRangeMax, final int rangeMin, final int rangeMax, final int currentMin, final int currentMax )
	{
		this.fullRangeMin = fullRangeMin;
		this.fullRangeMax = fullRangeMax;
		minValue = new BoundedValue( rangeMin, rangeMax - 1, currentMin )
		{
			@Override
			public void setCurrentValue( final int value )
			{
				super.setCurrentValue( value );
				final int min = minValue.getCurrentValue();
				int max = maxValue.getCurrentValue();
				if (min >= max)
				{
					max = min + 1;
					maxValue.setCurrentValue( max );
				}
				for ( final ConverterSetup setup : setups )
					setup.setDisplayRange( min, max );
			}
		};

		maxValue = new BoundedValue( rangeMin + 1, rangeMax, currentMax )
		{
			@Override
			public void setCurrentValue( final int value )
			{
				super.setCurrentValue( value );
				int min = minValue.getCurrentValue();
				final int max = maxValue.getCurrentValue();
				if ( min >= max )
				{
					min = max - 1;
					minValue.setCurrentValue( min );
				}
				for ( final ConverterSetup setup : setups )
					setup.setDisplayRange( min, max );
			}
		};

		setups = new LinkedHashSet< ConverterSetup >();

		updateListener = null;
	}

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

	public int getFullRangeMin()
	{
		return fullRangeMin;
	}

	public int getFullRangeMax()
	{
		return fullRangeMax;
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
		assert min < max;
		minValue.setRange( min, max - 1 );
		maxValue.setRange( min + 1, max );
		final int currentMin = minValue.getCurrentValue();
		final int currentMax = maxValue.getCurrentValue();
		if ( currentMin >= currentMax )
		{
			if ( currentMax == max )
				minValue.setCurrentValue( currentMax - 1 );
			else
				maxValue.setCurrentValue( currentMin + 1 );
		}
	}

	/**
	 * Add a {@link ConverterSetup} which will have its
	 * {@link ConverterSetup#setDisplayRange(int, int) display range} updated to
	 * the interval ({@link #getMinBoundedValue()},
	 * {@link #getMaxBoundedValue()}).
	 *
	 * @param setup
	 */
	public void addSetup( final ConverterSetup setup )
	{
		setups.add( setup );
		setup.setDisplayRange( minValue.getCurrentValue(), maxValue.getCurrentValue() );

		if ( updateListener != null )
			updateListener.update();
	}

	/**
	 * Remove a {@link ConverterSetup} from this group.
	 *
	 * @param setup
	 * @return true, if this group is now empty. false otherwise.
	 */
	public boolean removeSetup( final ConverterSetup setup )
	{
		setups.remove( setup );

		if ( updateListener != null )
			updateListener.update();

		return setups.isEmpty();
	}

	/**
	 * Set an {@link UpdateListener} (usually a GUI component).
	 */
	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}
}
