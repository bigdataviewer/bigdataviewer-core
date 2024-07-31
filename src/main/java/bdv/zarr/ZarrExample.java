package bdv.zarr;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.zarr.JsonImgLoaderIos;
import mpicbg.spim.data.zarr.JsonIoBasicImgLoader;

public class ZarrExample
{
	public static void main( final String[] args ) throws SpimDataException
	{
		// load SpimData from xml file
		final String xmlFilename = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final XmlIoSpimData io = new XmlIoSpimData();
		final SpimData spimData = io.load( xmlFilename );

		final Class< ? extends ImgLoader > aClass = spimData.getSequenceDescription().getImgLoader().getClass();

		System.out.println( "imgloader class: " + aClass );
		final JsonIoBasicImgLoader< ? > jsonIo = JsonImgLoaderIos.createJsonIoForImgLoaderClass( aClass );
		System.out.println( "jsonIo = " + jsonIo );

//		final String basePath = "/Users/pietzsch/tmp/spimdata.zarr";
//		final N5ZarrWriter writer = new N5ZarrWriter(basePath, gsonBuilder());
//		writer.setAttribute(".", "spimdata", spimData );
//		writer.close();
	}
}
