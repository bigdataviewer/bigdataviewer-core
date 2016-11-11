package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class Bookmark {

	public static final String XmlElementBookmarkName = "Bookmark" ;
	public static final String XmlElementKeyName = "key" ;
	public static final String XmlElementTransformName = "transform" ;
	
	protected String key;
	protected AffineTransform3D transform;
	
	public Bookmark(Element element){
		restoreFromXml(element);
	}
	
	public Bookmark(String key, AffineTransform3D transform) {
		this.key = key;
		this.transform = transform;
	}

	public String getKey() {
		return this.key;
	}
	
	public AffineTransform3D getAffineTransform3D() {
		return this.transform;
	}

	public void setAffineTransform3D(AffineTransform3D transform) {
		this.transform = transform;
	}
	
	public Element toXmlNode() {
		final Element elemBookmark = new Element( XmlElementBookmarkName );
		elemBookmark.addContent( XmlHelpers.textElement( XmlElementKeyName, this.key ) );
		elemBookmark.addContent( XmlHelpers.affineTransform3DElement( XmlElementTransformName, this.transform ) );
		return elemBookmark;
	}
	
	public void restoreFromXml(Element element) {
		this.key = XmlHelpers.getText( element, XmlElementKeyName );
		this.transform = XmlHelpers.getAffineTransform3D( element, XmlElementTransformName );
	}	
}
