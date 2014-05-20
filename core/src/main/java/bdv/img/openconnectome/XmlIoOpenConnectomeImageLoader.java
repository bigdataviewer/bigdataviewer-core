package bdv.img.openconnectome;

import java.io.File;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;

import org.jdom2.Element;

@ImgLoaderIo( format = "openconnectome", type = OpenConnectomeImageLoader.class )
public class XmlIoOpenConnectomeImageLoader implements XmlIoBasicImgLoader< OpenConnectomeImageLoader >
{
	@Override
	public Element toXml( final OpenConnectomeImageLoader imgLoader, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public OpenConnectomeImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final String baseUrl = elem.getChildText( "baseUrl" );
		final String token = elem.getChildText( "token" );
		final String mode = elem.getChildText( "mode" );
		return new OpenConnectomeImageLoader( baseUrl, token, mode );
	}
}
