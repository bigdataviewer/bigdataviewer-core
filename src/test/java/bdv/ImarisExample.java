package bdv;

import bdv.export.ProgressWriterConsole;
import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import java.io.IOException;
import mpicbg.spim.data.SpimDataException;

public class ImarisExample
{
	static void createXmlForIms( String imsFilename, String xmlFilename ) throws IOException, SpimDataException
	{
		final SpimDataMinimal spimData = Imaris.openIms( imsFilename );
		new XmlIoSpimDataMinimal().save( spimData, xmlFilename );
	}

	public static void main( String[] args ) throws IOException, SpimDataException
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final String imsFilename = "/Users/pietzsch/Desktop/OvernightClaritySmall.ims";
		final String xmlFilename = "/Users/pietzsch/Desktop/OvernightClaritySmall.xml";

//		final String imsFilename = "/Users/pietzsch/Imaris Demo Images/DrosophilaEggChamber_with_objects.ims";
//		final String xmlFilename = "/Users/pietzsch/Imaris Demo Images/DrosophilaEggChamber_with_objects.xml";

		// write xml file for ims
		createXmlForIms( imsFilename, xmlFilename );

		// open xml file in BigDataViewer
		BigDataViewer.open( xmlFilename, "BigDataViewer", new ProgressWriterConsole(), ViewerOptions.options() );
	}
}

