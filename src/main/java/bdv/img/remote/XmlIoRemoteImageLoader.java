package bdv.img.remote;

import java.io.File;
import java.io.IOException;

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
		throw new UnsupportedOperationException( "not implemented" );
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
