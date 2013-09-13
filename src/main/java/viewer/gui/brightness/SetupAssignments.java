package viewer.gui.brightness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.type.numeric.ARGBType;

import org.jdom2.Element;

/**
 * Manage a (fixed) set of {@link ConverterSetup}s and (changing) set of
 * {@link MinMaxGroup}s, such that the following is always true:
 * <ol>
 * <li>Every setup is assigned to exactly one group.</li>
 * <li>No group is empty.</li>
 * </ol>
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SetupAssignments
{
	/**
	 * All {@link ConverterSetup}s managed by this SetupAssignments.
	 */
	private final ArrayList< ConverterSetup > setups;

	/**
	 * A list of {@link MinMaxGroup}s, such that every {@link #setups setup} is
	 * contained in exactly one group.
	 */
	private final ArrayList< MinMaxGroup > minMaxGroups;

	/**
	 * Maps every {@link #setups setup} to the {@link #minMaxGroups group}
	 * containing it.
	 */
	private final Map< ConverterSetup, MinMaxGroup > setupToGroup;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	/**
	 *
	 * @param converterSetups
	 * @param fullRangeMin
	 * @param fullRangeMax
	 */
	public SetupAssignments( final ArrayList< ConverterSetup > converterSetups, final int fullRangeMin, final int fullRangeMax )
	{
		setups = new ArrayList< ConverterSetup >( converterSetups );
		minMaxGroups = new ArrayList< MinMaxGroup >();
		setupToGroup = new HashMap< ConverterSetup, MinMaxGroup >();
		for ( final ConverterSetup setup : setups )
		{
			final MinMaxGroup group = new MinMaxGroup( fullRangeMin, fullRangeMax, fullRangeMin, fullRangeMax, setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
			minMaxGroups.add( group );
			setupToGroup.put( setup, group );
			group.addSetup( setup );
		}
		updateListener = null;
	}

	/**
	 * Add the specified setup to the specified group. The setup is removed from
	 * its previous group. If this previous group is made empty by this, it is
	 * removed from the list of groups.
	 */
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

	/**
	 * Remove the specified setup from the specified group. If this group is
	 * made empty by this, it is removed from the list of groups. A new group is
	 * created containing only the specified setup, and this new group is added
	 * to the list of group. The settings of the new group are initialized with
	 * the settings of the old group.
	 */
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

	/**
	 * @return the list of {@link MinMaxGroup}s, such that every {@link #setups
	 *         setup} is contained in exactly one group.
	 */
	public List< MinMaxGroup > getMinMaxGroups()
	{
		return Collections.unmodifiableList( minMaxGroups );
	}

	/**
	 * @return a list of all {@link ConverterSetup}s.
	 */
	public List< ConverterSetup > getConverterSetups()
	{
		return Collections.unmodifiableList( setups );
	}

	/**
	 * Serialize the state of this {@link SetupAssignments} to XML.
	 * @param doc
	 * @return
	 */
	public Element toXml()
	{
		final Element elem = new Element( "SetupAssignments" );

		final Element elemConverterSetups = new Element( "ConverterSetups" );
		for ( final ConverterSetup setup : setups )
		{
			final Element elemConverterSetup = new Element( "ConverterSetup" );
			elemConverterSetup.addContent( XmlHelpers.intElement( "id", setup.getSetupId() ) );
			elemConverterSetup.addContent( XmlHelpers.intElement( "min", setup.getDisplayRangeMin() ) );
			elemConverterSetup.addContent( XmlHelpers.intElement( "max", setup.getDisplayRangeMax() ) );
			elemConverterSetup.addContent( XmlHelpers.intElement( "color", setup.getColor().get() ) );
			elemConverterSetup.addContent( XmlHelpers.intElement( "groupId", minMaxGroups.indexOf( setupToGroup.get( setup ) ) ) );
			elemConverterSetups.addContent( elemConverterSetup );
		}
		elem.addContent( elemConverterSetups );

		final Element elemMinMaxGroups = new Element( "MinMaxGroups" );
		for ( int i = 0; i < minMaxGroups.size(); ++i )
		{
			final MinMaxGroup group = minMaxGroups.get( i );
			final Element elemMinMaxGroup = new Element( "MinMaxGroup" );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "id", i ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "fullRangeMin", group.getFullRangeMin() ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "fullRangeMax", group.getFullRangeMax() ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "rangeMin", group.getRangeMin() ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "rangeMax", group.getRangeMax() ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "currentMin", group.getMinBoundedValue().getCurrentValue() ) );
			elemMinMaxGroup.addContent( XmlHelpers.intElement( "currentMax", group.getMaxBoundedValue().getCurrentValue() ) );
			elemMinMaxGroups.addContent( elemMinMaxGroup );
		}
		elem.addContent( elemMinMaxGroups );

		return elem;
	}

	/**
	 * Restore the state of this {@link SetupAssignments} from XML. Note, that
	 * this only restores the assignments of setups to groups and group
	 * settings. The list of {@link ConverterSetup}s is not restored.
	 */
	public void restoreFromXml( final Element parent )
	{
		final Element elemSetupAssignments = parent.getChild( "SetupAssignments" );
		if ( elemSetupAssignments == null )
			return;
		final Element elemConverterSetups = elemSetupAssignments.getChild( "ConverterSetups" );
		final List< Element > converterSetupNodes = elemConverterSetups.getChildren( "ConverterSetup" );
		if ( converterSetupNodes.size() != setups.size() )
			throw new IllegalArgumentException();

		final Element elemMinMaxGroups = elemSetupAssignments.getChild( "MinMaxGroups" );
		final List< Element > minMaxGroupNodes = elemMinMaxGroups.getChildren( "MinMaxGroup" );
		minMaxGroups.clear();
		for ( int i = 0; i < minMaxGroupNodes.size(); ++i )
			minMaxGroups.add( null );
		for ( final Element elem : minMaxGroupNodes  )
		{
			final int id = Integer.parseInt( elem.getChildText( "id" ) );
			final int fullRangeMin = Integer.parseInt( elem.getChildText( "fullRangeMin" ) );
			final int fullRangeMax = Integer.parseInt( elem.getChildText( "fullRangeMax" ) );
			final int rangeMin = Integer.parseInt( elem.getChildText( "rangeMin" ) );
			final int rangeMax = Integer.parseInt( elem.getChildText( "rangeMax" ) );
			final int currentMin = Integer.parseInt( elem.getChildText( "currentMin" ) );
			final int currentMax = Integer.parseInt( elem.getChildText( "currentMax" ) );
			minMaxGroups.set( id, new MinMaxGroup( fullRangeMin, fullRangeMax, rangeMin, rangeMax, currentMin, currentMax ) );
		}

		for ( final Element elem : converterSetupNodes )
		{
			final int id = Integer.parseInt( elem.getChildText( "id" ) );
			final int min = Integer.parseInt( elem.getChildText( "min" ) );
			final int max = Integer.parseInt( elem.getChildText( "max" ) );
			final int color = Integer.parseInt( elem.getChildText( "color" ) );
			final int groupId = Integer.parseInt( elem.getChildText( "groupId" ) );
			final ConverterSetup setup = setups.get( id );
			setup.setDisplayRange( min, max );
			setup.setColor( new ARGBType( color ) );
			final MinMaxGroup group = minMaxGroups.get( groupId );
			setupToGroup.put( setup, group );
			group.addSetup( setup );
		}

		if ( updateListener != null )
			updateListener.update();
	}
}
