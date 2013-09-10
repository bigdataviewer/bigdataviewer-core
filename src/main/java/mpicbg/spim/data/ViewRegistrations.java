package mpicbg.spim.data;

import java.util.ArrayList;

import org.jdom2.Element;

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
				Integer.parseInt( elem.getChildText( "ReferenceTimepoint" ) )
		);
	}

	protected static ArrayList< ViewRegistration > createRegistrationsFromXml( final Element viewRegistrations )
	{
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		for ( final Element elem : viewRegistrations.getChildren( "ViewRegistration" ) )
			regs.add( new ViewRegistration( elem ) );
		return regs;
	}
}
