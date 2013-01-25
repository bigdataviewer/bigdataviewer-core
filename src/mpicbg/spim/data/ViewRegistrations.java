package mpicbg.spim.data;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ViewRegistrations
{
	final public ArrayList< ViewRegistration > registrations;

	/**
	 * the id (not index!) of the reference timepoint.
	 */
	final public int referenceTimePoint;

	public ViewRegistrations( final ArrayList< ViewRegistration > registrations, final int referenceTimePoint )
	{
		this.registrations = registrations;
		this.referenceTimePoint = referenceTimePoint;
	}

	/**
	 * Load a ViewRegistrations from an XML file.
	 *
	 * @param elem
	 *            The "ViewRegistrations" DOM element.
	 */
	public ViewRegistrations( final Element elem )
	{
		this(
				createRegistrationsFromXml( elem ),
				Integer.parseInt( elem.getElementsByTagName( "ReferenceTimepoint" ).item( 0 ).getTextContent() )
		);
	}

	protected static ArrayList< ViewRegistration > createRegistrationsFromXml( final Element viewRegistrations )
	{
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		final NodeList nodes = viewRegistrations.getElementsByTagName( "ViewRegistration" );
		for ( int i = 0; i < nodes.getLength(); ++i )
			regs.add( new ViewRegistration( ( Element ) nodes.item( i ) ) );
		return regs;
	}
}
