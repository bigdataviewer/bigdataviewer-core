package mpicbg.tracking.data.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlHelpers
{
	public static Element intElement( final Document doc, final String name, final int value )
	{
		final Element e = doc.createElement( name );
		e.appendChild( doc.createTextNode( Integer.toString( value ) ) );
		return e;
	}

	public static Element doubleElement( final Document doc, final String name, final double value )
	{
		final Element e = doc.createElement( name );
		e.appendChild( doc.createTextNode( Double.toString( value ) ) );
		return e;
	}

	public static Element textElement( final Document doc, final String name, final String value )
	{
		final Element e = doc.createElement( name );
		e.appendChild( doc.createTextNode( value ) );
		return e;
	}
}
