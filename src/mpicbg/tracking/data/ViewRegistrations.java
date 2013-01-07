package mpicbg.tracking.data;

import static mpicbg.tracking.data.io.XmlHelpers.loadPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ViewRegistrations
{
	final public ArrayList< ViewRegistration > registrations;

	final public String sequenceDescriptionName;

	final public int referenceTimePoint;

	public ViewRegistrations( final ArrayList< ViewRegistration > registrations, final String sequenceDescriptionName, final int referenceTimePoint )
	{
		this.registrations = registrations;
		this.sequenceDescriptionName = sequenceDescriptionName;
		this.referenceTimePoint = referenceTimePoint;
	}

	/**
	 * Load ViewRegistrations from an XML file.
	 */
	public static ViewRegistrations load( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse( xmlFilename );
		final Element root = dom.getDocumentElement();

		final String seqname = loadPath( root, "SequenceDescriptionName", new File( xmlFilename ).getParentFile() ).toString();
		final int reftp = Integer.parseInt( root.getElementsByTagName( "ReferenceTimepoint" ).item( 0 ).getTextContent() );
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		final NodeList nodes = root.getElementsByTagName( "ViewRegistration" );
		for ( int i = 0; i < nodes.getLength(); ++i )
			regs.add( ViewRegistration.fromXml( ( Element ) nodes.item( i ) ) );

		return new ViewRegistrations( regs, seqname, reftp );
	}
}
