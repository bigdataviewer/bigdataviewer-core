package bdv.img.remote;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.io.IOException;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;

import org.jdom2.Element;

@ImgLoaderIo( format = "bdv.remote", type = RemoteImageLoader.class )
public class XmlIoRemoteImageLoader implements XmlIoBasicImgLoader< RemoteImageLoader >
{

	@Override
	public Element toXml( final RemoteImageLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.remote" );
		elem.addContent( XmlHelpers.textElement( "baseUrl", imgLoader.baseUrl ) );
		return elem;
	}

	@Override
	public RemoteImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final String baseUrl = elem.getChildText( "baseUrl" );
		try
		{
			return new RemoteImageLoader( baseUrl );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

}
