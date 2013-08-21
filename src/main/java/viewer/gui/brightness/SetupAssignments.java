package viewer.gui.brightness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.type.numeric.ARGBType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

	// private final Element firstSavedState = null;
	//
	// public void testXml()
	// {
	// try
	// {
	// final Document doc = XmlHelpers.newXmlDocument();
	// final Element elem = toXml( doc );
	// doc.appendChild( elem );
	// final Transformer transformer =
	// TransformerFactory.newInstance().newTransformer();
	// transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
	// transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
	// transformer.setOutputProperty(
	// "{http://xml.apache.org/xslt}indent-amount", "4" );
	//
	// final StringWriter w = new StringWriter();
	// transformer.transform( new DOMSource( doc ), new StreamResult( w ) );
	// System.out.println( w );
	//
	// if ( firstSavedState != null )
	// restoreFromXml( firstSavedState );
	// else
	// firstSavedState = elem;
	// }
	// catch ( final Exception e )
	// {}
	// }

	/**
	 * Serialize the state of this {@link SetupAssignments} to XML.
	 * @param doc
	 * @return
	 */
	public Element toXml( final Document doc )
	{
		final Element elem = doc.createElement( "SetupAssignments" );

		final Element elemConverterSetups = doc.createElement( "ConverterSetups" );
		for ( final ConverterSetup setup : setups )
		{
			final Element elemConverterSetup = doc.createElement( "ConverterSetup" );
			elemConverterSetup.appendChild( XmlHelpers.intElement( doc, "id", setup.getSetupId() ) );
			elemConverterSetup.appendChild( XmlHelpers.intElement( doc, "min", setup.getDisplayRangeMin() ) );
			elemConverterSetup.appendChild( XmlHelpers.intElement( doc, "max", setup.getDisplayRangeMax() ) );
			elemConverterSetup.appendChild( XmlHelpers.intElement( doc, "color", setup.getColor().get() ) );
			elemConverterSetup.appendChild( XmlHelpers.intElement( doc, "groupId", minMaxGroups.indexOf( setupToGroup.get( setup ) ) ) );
			elemConverterSetups.appendChild( elemConverterSetup );
		}
		elem.appendChild( elemConverterSetups );

		final Element elemMinMaxGroups = doc.createElement( "MinMaxGroups" );
		for ( int i = 0; i < minMaxGroups.size(); ++i )
		{
			final MinMaxGroup group = minMaxGroups.get( i );
			final Element elemMinMaxGroup = doc.createElement( "MinMaxGroup" );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "id", i ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "fullRangeMin", group.getFullRangeMin() ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "fullRangeMax", group.getFullRangeMax() ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "rangeMin", group.getRangeMin() ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "rangeMax", group.getRangeMax() ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "currentMin", group.getMinBoundedValue().getCurrentValue() ) );
			elemMinMaxGroup.appendChild( XmlHelpers.intElement( doc, "currentMax", group.getMaxBoundedValue().getCurrentValue() ) );
			elemMinMaxGroups.appendChild( elemMinMaxGroup );
		}
		elem.appendChild( elemMinMaxGroups );

		return elem;
	}

	/**
	 * Restore the state of this {@link SetupAssignments} from XML. Note, that
	 * this only restores the assignments of setups to groups and group
	 * settings. The list of {@link ConverterSetup}s is not restored.
	 */
	public void restoreFromXml( final Element elemSetupAssignments )
	{
		final Element elemConverterSetups = ( Element ) elemSetupAssignments.getElementsByTagName( "ConverterSetups" ).item( 0 );
		final NodeList converterSetupNodes = elemConverterSetups.getElementsByTagName( "ConverterSetup" );
		if ( converterSetupNodes.getLength() != setups.size() )
			throw new IllegalArgumentException();

		final Element elemMinMaxGroups = ( Element ) elemSetupAssignments.getElementsByTagName( "MinMaxGroups" ).item( 0 );
		final NodeList minMaxGroupNodes = elemMinMaxGroups.getElementsByTagName( "MinMaxGroup" );
		minMaxGroups.clear();
		for ( int i = 0; i < minMaxGroupNodes.getLength(); ++i )
			minMaxGroups.add( null );
		for ( int i = 0; i < minMaxGroupNodes.getLength(); ++i )
		{
			final Element elem = ( Element ) minMaxGroupNodes.item( i );
			final int id = Integer.parseInt( elem.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
			final int fullRangeMin = Integer.parseInt( elem.getElementsByTagName( "fullRangeMin" ).item( 0 ).getTextContent() );
			final int fullRangeMax = Integer.parseInt( elem.getElementsByTagName( "fullRangeMax" ).item( 0 ).getTextContent() );
			final int rangeMin = Integer.parseInt( elem.getElementsByTagName( "rangeMin" ).item( 0 ).getTextContent() );
			final int rangeMax = Integer.parseInt( elem.getElementsByTagName( "rangeMax" ).item( 0 ).getTextContent() );
			final int currentMin = Integer.parseInt( elem.getElementsByTagName( "currentMin" ).item( 0 ).getTextContent() );
			final int currentMax = Integer.parseInt( elem.getElementsByTagName( "currentMax" ).item( 0 ).getTextContent() );
			minMaxGroups.set( id, new MinMaxGroup( fullRangeMin, fullRangeMax, rangeMin, rangeMax, currentMin, currentMax ) );
		}

		for ( int i = 0; i < converterSetupNodes.getLength(); ++i )
		{
			final Element elem = ( Element ) converterSetupNodes.item( i );
			final int id = Integer.parseInt( elem.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
			final int min = Integer.parseInt( elem.getElementsByTagName( "min" ).item( 0 ).getTextContent() );
			final int max = Integer.parseInt( elem.getElementsByTagName( "max" ).item( 0 ).getTextContent() );
			final int color = Integer.parseInt( elem.getElementsByTagName( "color" ).item( 0 ).getTextContent() );
			final int groupId = Integer.parseInt( elem.getElementsByTagName( "groupId" ).item( 0 ).getTextContent() );
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
