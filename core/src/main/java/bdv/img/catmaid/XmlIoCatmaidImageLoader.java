package bdv.img.catmaid;

import java.io.File;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;

import org.jdom2.Element;

@ImgLoaderIo( format = "catmaid", type = CatmaidImageLoader.class )
public class XmlIoCatmaidImageLoader implements XmlIoBasicImgLoader< CatmaidImageLoader >
{
	@Override
	public Element toXml( final CatmaidImageLoader imgLoader, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public CatmaidImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final long width = Long.parseLong( elem.getChildText( "width" ) );
		final long height = Long.parseLong( elem.getChildText( "height" ) );
		final long depth = Long.parseLong( elem.getChildText( "depth" ) );

//		double resXY = Double.parseDouble( elem.getChildText( "resXY" ) );
//		double resZ = Double.parseDouble( elem.getChildText( "resZ" ) );

		final String urlFormat = elem.getChildText( "urlFormat" );

		final int tileWidth = Integer.parseInt( elem.getChildText( "tileWidth" ) );
		final int tileHeight = Integer.parseInt( elem.getChildText( "tileHeight" ) );

		final String numScalesString = elem.getChildText( "numScales" );
		int numScales;
		if ( numScalesString == null )
			numScales = CatmaidImageLoader.getNumScales( width, height, tileWidth, tileHeight );
		else
			numScales = Integer.parseInt( numScalesString );

		return new CatmaidImageLoader( width, height, depth, numScales, urlFormat, tileWidth, tileHeight );
	}
}
