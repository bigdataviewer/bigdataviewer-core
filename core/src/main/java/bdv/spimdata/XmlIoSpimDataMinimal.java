package bdv.spimdata;

import static mpicbg.spim.data.XmlKeys.SPIMDATA_TAG;

import java.io.File;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.XmlIoAbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.XmlIoBasicViewSetups;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoTimePoints;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import bdv.spimdata.legacy.XmlIoSpimDataMinimalLegacy;

public class XmlIoSpimDataMinimal extends XmlIoAbstractSpimData< SequenceDescriptionMinimal, SpimDataMinimal >
{
	public XmlIoSpimDataMinimal()
	{
		super( SpimDataMinimal.class,
				new XmlIoAbstractSequenceDescription< BasicViewSetup, SequenceDescriptionMinimal >(
						SequenceDescriptionMinimal.class,
						new XmlIoTimePoints(),
						new XmlIoBasicViewSetups< BasicViewSetup >( BasicViewSetup.class ),
						new XmlIoMissingViews() ),
				new XmlIoViewRegistrations() );
	}

	@Override
	public SpimDataMinimal load( final String xmlFilename ) throws SpimDataException
		{
			final SAXBuilder sax = new SAXBuilder();
			Document doc;
			try
			{
				doc = sax.build( xmlFilename );
			}
			catch ( final Exception e )
			{
				throw new SpimDataIOException( e );
			}
			final Element root = doc.getRootElement();

			if ( root.getName().equals( "SequenceDescription" ) )
				return XmlIoSpimDataMinimalLegacy.fromXml( root, new File( xmlFilename ) );

			if ( root.getName() != SPIMDATA_TAG )
				throw new RuntimeException( "expected <" + SPIMDATA_TAG + "> root element. wrong file?" );

			return fromXml( root, new File( xmlFilename ) );
		}
}
