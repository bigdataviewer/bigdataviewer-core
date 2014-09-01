package bdv.tools.bookmarks;

import java.util.HashMap;
import java.util.Map.Entry;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

public class Bookmarks
{
	private final HashMap< String, AffineTransform3D > bookmarks;

	public Bookmarks()
	{
		bookmarks = new HashMap< String, AffineTransform3D >();
	}

	public Element toXml()
	{
		final Element elem = new Element( "Bookmarks" );
		for ( final Entry< String, AffineTransform3D > entry : bookmarks.entrySet() )
		{
			final String key = entry.getKey();
			final AffineTransform3D transform = entry.getValue();

			final Element elemBookmark = new Element( "Bookmark" );
			elemBookmark.addContent( XmlHelpers.textElement( "key", key ) );
			elemBookmark.addContent( XmlHelpers.affineTransform3DElement( "transform", transform ) );
			elem.addContent( elemBookmark );
		}
		return elem;
	}

	public void restoreFromXml( final Element parent )
	{
		bookmarks.clear();

		final Element elemBookmarks = parent.getChild( "Bookmarks" );
		if ( elemBookmarks == null )
			return;

		for ( final Element elem : elemBookmarks.getChildren( "Bookmark" ) )
		{
			final String key = XmlHelpers.getText( elem, "key" );
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D( elem, "transform" );
			bookmarks.put( key, transform );
		}
	}

	public void put( final String key, final AffineTransform3D transform )
	{
		bookmarks.put( key, transform );
	}

	public AffineTransform3D get( final String key )
	{
		return bookmarks.get( key );
	}
}

