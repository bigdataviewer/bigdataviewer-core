package mpicbg.tracking.data.io;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlHelpers
{
	public static Document newXmlDocument() throws ParserConfigurationException
	{
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}

	public static void writeXmlDocument( final Document doc, final String xmlFilename ) throws TransformerFactoryConfigurationError, TransformerException
	{
		writeXmlDocument( doc, new File( xmlFilename ) );
	}

	public static void writeXmlDocument( final Document doc, final File xmlFile ) throws TransformerFactoryConfigurationError, TransformerException
	{
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
		transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );
		transformer.transform( new DOMSource( doc ), new StreamResult( xmlFile ) );
	}

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

	public static File loadPath( final Element parent, final String name, final String defaultRelativePath, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final NodeList nd = parent.getElementsByTagName( name );
		final String path = nd.getLength() > 0 ? nd.item( 0 ).getTextContent() : defaultRelativePath;
		final boolean isRelative = nd.getLength() > 0 ? ( ( Element ) nd.item( 0 ) ).getAttribute( "type" ).equals( "relative" ) : true;
		if ( isRelative )
		{
			if ( basePath == null )
				return null;
			else
				return new File( basePath + "/" + path );
		}
		else
			return new File( path );
	}

	public static File loadPath( final Element parent, final String name, final File basePath ) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final NodeList nd = parent.getElementsByTagName( name );
		if ( nd.getLength() == 0 )
			return null;
		final String path =  nd.item( 0 ).getTextContent();
		final boolean isRelative = ( ( Element ) nd.item( 0 ) ).getAttribute( "type" ).equals( "relative" );
		if ( isRelative )
		{
			if ( basePath == null )
				return null;
			else
				return new File( basePath + "/" + path );
		}
		else
			return new File( path );
	}

}
