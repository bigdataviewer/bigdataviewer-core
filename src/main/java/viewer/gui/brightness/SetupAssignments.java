package viewer.gui.brightness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupAssignments
{
	private final int typeMin;

	private final int typeMax;

	private final ArrayList< ConverterSetup > setups;

	private final ArrayList< MinMaxGroup > minMaxGroups;

	private final Map< ConverterSetup, MinMaxGroup > setupToGroup;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	public SetupAssignments( final ArrayList< ConverterSetup > converterSetups, final int fullRangeMin, final int fullRangeMax )
	{
		typeMin = fullRangeMin;
		typeMax = fullRangeMax;
		setups = new ArrayList< ConverterSetup >( converterSetups );
		minMaxGroups = new ArrayList< MinMaxGroup >();
		setupToGroup = new HashMap< ConverterSetup, MinMaxGroup >();
		for ( final ConverterSetup setup : setups )
		{
			final MinMaxGroup group = new MinMaxGroup( typeMin, typeMax, typeMin, typeMax, setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
			minMaxGroups.add( group );
			setupToGroup.put( setup, group );
			group.addSetup( setup );
		}
		updateListener = null;
	}

	public void moveSetupToGroup( final ConverterSetup setup, final MinMaxGroup group )
	{
		final MinMaxGroup oldGroup = setupToGroup.get( setup );
		if ( oldGroup == group )
			return;

		setupToGroup.put( setup, group );
		group.addSetup( setup );

		final boolean oldGroupIsEmpty = oldGroup.removeSetup( setup );
		if ( oldGroupIsEmpty )
			minMaxGroups.remove( oldGroup );

		if ( updateListener != null )
			updateListener.update();
	}

	public void removeSetupFromGroup( final ConverterSetup setup, final MinMaxGroup group )
	{
		if ( setupToGroup.get( setup ) != group )
			return;

		final MinMaxGroup newGroup = new MinMaxGroup( group.getFullRangeMin(), group.getFullRangeMax(), group.getRangeMin(), group.getRangeMax(), setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
		minMaxGroups.add( newGroup );
		setupToGroup.put( setup, newGroup );
		newGroup.addSetup( setup );

		final boolean groupIsEmpty = group.removeSetup( setup );
		if ( groupIsEmpty )
			minMaxGroups.remove( group );

		if ( updateListener != null )
			updateListener.update();
	}

	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}

	public List< MinMaxGroup > getMinMaxGroups()
	{
		return Collections.unmodifiableList( minMaxGroups );
	}

	public List< ConverterSetup > getConverterSetups()
	{
		return Collections.unmodifiableList( setups );
	}
}
