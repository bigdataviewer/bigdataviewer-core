package viewer.gui.brightness;

import java.util.LinkedHashSet;
import java.util.Set;

import viewer.util.BoundedInterval;
import viewer.util.BoundedValue;

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
public class MinMaxGroup extends BoundedInterval
{
	private final int fullRangeMin;

	private final int fullRangeMax;

	final Set< ConverterSetup > setups;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	public MinMaxGroup( final int fullRangeMin, final int fullRangeMax, final int rangeMin, final int rangeMax, final int currentMin, final int currentMax )
	{
		super( rangeMin, rangeMax, currentMin, currentMax, 2 );
		this.fullRangeMin = fullRangeMin;
		this.fullRangeMax = fullRangeMax;
		setups = new LinkedHashSet< ConverterSetup >();
		updateListener = null;
	}

	@Override
	protected void updateInterval( final int min, final int max )
	{
		for ( final ConverterSetup setup : setups )
			setup.setDisplayRange( min, max );
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
		setup.setDisplayRange( getMinBoundedValue().getCurrentValue(), getMaxBoundedValue().getCurrentValue() );

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
