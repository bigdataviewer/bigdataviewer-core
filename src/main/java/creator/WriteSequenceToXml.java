package creator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import mpicbg.spim.data.XmlHelpers;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class WriteSequenceToXml
{
	public static void writeSequenceToXml( final SequenceDescription sequence, final ViewRegistrations registrations, final String xmlFilename ) throws IOException
	{
		System.out.println( "writing sequence description to " + xmlFilename );
		final Element root = sequenceDescriptionToXml( sequence, new File( xmlFilename ).getParentFile() );
		root.addContent( viewRegistrationsToXml( registrations ) );
		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileWriter( xmlFilename ) );
	}

	public static Element sequenceDescriptionToXml( final SequenceDescription sequence, final File xmlFileDirectory )
	{
		final Element elem = new Element( "SequenceDescription" );

		// add BasePath
		elem.addContent( XmlHelpers.pathElement( "BasePath", sequence.getBasePath(), xmlFileDirectory ) );

		// add ImageLoader
		elem.addContent( sequence.imgLoader.toXml( sequence.getBasePath() ) );

		// add ViewSetups
		for ( final ViewSetup setup : sequence.setups )
			elem.addContent( setup.toXml() );

		elem.addContent( timepointsToXml( sequence ) );

		return elem;
	}

	public static Element viewRegistrationsToXml(  final ViewRegistrations viewRegistrations )
	{
		final Element elem = new Element( "ViewRegistrations" );

		// add reference timepoint
		elem.addContent( XmlHelpers.intElement( "ReferenceTimepoint", viewRegistrations.referenceTimePoint ) );

		// add ViewSetups
		for ( final ViewRegistration reg : viewRegistrations.registrations )
			elem.addContent( reg.toXml() );

		return elem;
	}

	/**
	 * TODO: Add support for non-contiguous range of time-points. (Timepoints
	 * type="list")
	 *
	 * @param doc
	 * @param sequence
	 */
	protected static Element timepointsToXml( final SequenceDescription sequence )
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

		final Element tp = new Element( "Timepoints" );
		tp.setAttribute( "type", "range" );
		tp.addContent( XmlHelpers.intElement( "first", first ) );
		tp.addContent( XmlHelpers.intElement( "last", last ) );
		return tp;
	}
}
