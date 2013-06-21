package viewer.gui.brightness;

import java.util.LinkedHashSet;
import java.util.Set;

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

	public BoundedValue getMinBoundedValue()
	{
		return minValue;
	}

	public BoundedValue getMaxBoundedValue()
	{
		return maxValue;
	}

	public int getRangeMin()
	{
		return minValue.getRangeMin();
	}

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

	public void addSetup( final ConverterSetup setup )
	{
		setups.add( setup );
		setup.setDisplayRange( minValue.getCurrentValue(), maxValue.getCurrentValue() );

		if ( updateListener != null )
			updateListener.update();
	}

	public boolean removeSetup( final ConverterSetup setup )
	{
		setups.remove( setup );

		if ( updateListener != null )
			updateListener.update();

		return setups.isEmpty();
	}

	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}
}
