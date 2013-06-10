package creator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.data.XmlHelpers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WriteSequenceToXml
{
	public static void writeSequenceToXml( final SequenceDescription sequence, final ViewRegistrations registrations, final String xmlFilename ) throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException, FileNotFoundException
	{
		System.out.println( "writing sequence description to " + xmlFilename );
		final Document doc = XmlHelpers.newXmlDocument();
		final Element root = sequenceDescriptionToXml( doc, sequence, new File( xmlFilename ).getParentFile() );
		root.appendChild( viewRegistrationsToXml( doc, registrations ) );
		doc.appendChild( root );
		XmlHelpers.writeXmlDocument( doc, xmlFilename );
	}

	public static Element sequenceDescriptionToXml( final Document doc, final SequenceDescription sequence, final File xmlFileDirectory )
	{
		final Element elem = doc.createElement( "SequenceDescription" );

		// add BasePath
		elem.appendChild( XmlHelpers.pathElement( doc, "BasePath", sequence.getBasePath(), xmlFileDirectory ) );

		// add ImageLoader
		elem.appendChild( sequence.imgLoader.toXml( doc, sequence.getBasePath() ) );

		// add ViewSetups
		for ( final ViewSetup setup : sequence.setups )
			elem.appendChild( setup.toXml( doc ) );

		elem.appendChild( timepointsToXml( doc, sequence ) );

		return elem;
	}

	public static Element viewRegistrationsToXml( final Document doc, final ViewRegistrations viewRegistrations ) throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException
	{
		final Element elem = doc.createElement( "ViewRegistrations" );

		// add reference timepoint
		elem.appendChild( XmlHelpers.intElement( doc, "ReferenceTimepoint", viewRegistrations.referenceTimePoint ) );

		// add ViewSetups
		for ( final ViewRegistration reg : viewRegistrations.registrations )
			elem.appendChild( reg.toXml( doc ) );

		return elem;
	}

	/**
	 * TODO: Add support for non-contiguous range of time-points. (Timepoints
	 * type="list")
	 *
	 * @param doc
	 * @param sequence
	 */
	protected static Element timepointsToXml( final Document doc, final SequenceDescription sequence )
	{
		final ArrayList< Integer > timepoints = sequence.timepoints;
		if ( timepoints.size() == 0 )
			throw new IllegalArgumentException( "sequence must have at least one timepoint" );

		// find first and last timepoint
		int first = timepoints.get( 0 );
		int last = first;
		for ( final int t : timepoints )
			if ( t < first )
				first = t;
			else if ( t > last )
				last = t;

		// check whether all intermediate timepoints are present
		boolean nonContiguousTimepoints = false;
A:		for ( int i = first + 1; i < last; ++i )
		{
			for ( final int t : timepoints )
				if ( t == i )
					continue A;
			nonContiguousTimepoints = true;
		}
		if ( nonContiguousTimepoints )
			throw new RuntimeException( "non-contiguous time-point range not implemented." );

		final Element tp = doc.createElement( "Timepoints" );
		tp.setAttribute( "type", "range" );
		tp.appendChild( XmlHelpers.intElement( doc, "first", first ) );
		tp.appendChild( XmlHelpers.intElement( doc, "last", last ) );
		return tp;
	}
}
