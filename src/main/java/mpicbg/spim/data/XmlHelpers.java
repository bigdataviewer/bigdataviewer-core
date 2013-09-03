package mpicbg.spim.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

import net.imglib2.realtransform.AffineTransform3D;

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

	public static void writeXmlDocument( final Document doc, final String xmlFilename ) throws TransformerFactoryConfigurationError, TransformerException, FileNotFoundException
	{
		writeXmlDocument( doc, new File( xmlFilename ) );
	}

	public static void writeXmlDocument( final Document doc, final File xmlFile ) throws TransformerFactoryConfigurationError, TransformerException, FileNotFoundException
	{
		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
		transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );
		transformer.transform( new DOMSource( doc ), new StreamResult( new FileOutputStream( xmlFile ) ) );
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

	/**
	 * @param basePath if null put the absolute path, otherwise relative to this
	 */
	public static Element pathElement( final Document doc, final String name, final File path, final File basePath )
	{
		final Element e = doc.createElement( name );

		if ( basePath == null )
			e.appendChild( doc.createTextNode( path.getAbsolutePath() ) );
		else
		{
			e.setAttribute( "type", "relative" );
			e.appendChild( doc.createTextNode( getRelativePath( path, basePath ).getPath() ) );
		}

		return e;
	}

	public static File getRelativePath( final File file, final File relativeToThis )
	{
		try
		{
			return getRelativePath( file.getCanonicalFile(), relativeToThis.getCanonicalFile(), "" );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private static File getRelativePath( final File file, final File relativeToThis, final String relativeInitial )
	{
		File parent = file;
		String relative = null;
		while( parent != null )
		{
			if ( parent.equals( relativeToThis ) )
			{
				return new File( relativeInitial + ( relative == null ? "." : relative ) );
			}
			relative = parent.getName() + ( relative == null ? "" : "/" + relative );
			parent = parent.getParentFile();
		}
		final File toParent = relativeToThis.getParentFile();
		if ( toParent == null )
			return null;
		else
			return getRelativePath( file, toParent, "../" + relativeInitial );
	}

	public static Element affineTransform3DElement( final Document doc, final String name, final AffineTransform3D value )
	{
		final Element e = doc.createElement( name );
		final double[] v = value.getRowPackedCopy();
		final String text =
				v[0] + " " + v[1] + " " + v[2] + " " + v[3] + " " +
				v[4] + " " + v[5] + " " + v[6] + " " + v[7] + " " +
				v[8] + " " + v[9] + " " + v[10] + " " + v[11];
		e.appendChild( doc.createTextNode( text ) );
		return e;
	}

	public static AffineTransform3D loadAffineTransform3D( final Element elem )
	{
		final String data = elem.getTextContent();
		final String[] fields = data.split( "\\s+" );
		if ( fields.length == 12 )
		{
			final double[] values = new double[ 12 ];
			for ( int i = 0; i < 12; ++i )
				values[ i ] = Double.parseDouble( fields[ i ] );
			final AffineTransform3D a = new AffineTransform3D();
			a.set( values );
			return a;
		}
		else
			throw new NumberFormatException( "Inappropriate parameters for " + AffineTransform3D.class.getCanonicalName() );
	}
}
