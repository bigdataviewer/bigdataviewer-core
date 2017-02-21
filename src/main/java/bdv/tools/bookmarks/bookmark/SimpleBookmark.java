package bdv.tools.bookmarks.bookmark;

import java.awt.datatransfer.StringSelection;

import org.jdom2.Element;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class SimpleBookmark implements Bookmark {

	public static final String XML_ELEM_BOOKMARK_NAME = "Bookmark" ;
	public static final String XML_ELEM_KEY_NAME = "key" ;
	public static final String XML_ELEM_TRANSFORM_NAME = "transform" ;
	
	protected final String key;
	protected AffineTransform3D transform;
	
	public SimpleBookmark(Element element){
		this.key = XmlHelpers.getText( element, XML_ELEM_KEY_NAME );
		this.transform = XmlHelpers.getAffineTransform3D( element, XML_ELEM_TRANSFORM_NAME );
	}
	
	public SimpleBookmark(String key, AffineTransform3D transform) {
		this.key = key;
		this.transform = transform;
	}
	
	protected SimpleBookmark(SimpleBookmark s) {
		this.key = s.key;
		this.transform = s.transform.copy();
	}
	
	protected SimpleBookmark(SimpleBookmark s, String newKey) {
		this.key = newKey;
		this.transform = s.transform.copy();
	}

	public String getKey() {
		return this.key;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof SimpleBookmark){
			if (getClass() == other.getClass()) {
				SimpleBookmark b = (SimpleBookmark) other;
				return this.key == b.key;
			}
		}
		return false;
	}

	public AffineTransform3D getTransform() {
		return this.transform;
	}

	public void setTransform(AffineTransform3D transform) {
		this.transform = transform;
	}
	
	public Element toXmlNode() {
		final Element elemBookmark = new Element( XML_ELEM_BOOKMARK_NAME );
		elemBookmark.addContent( XmlHelpers.textElement( XML_ELEM_KEY_NAME, this.key ) );
		elemBookmark.addContent( XmlHelpers.affineTransform3DElement( XML_ELEM_TRANSFORM_NAME, this.transform ) );
		return elemBookmark;
	}

	@Override
	public SimpleBookmark copy() {
		return new SimpleBookmark(this);
	}
	
	@Override
	public SimpleBookmark copy(String newKey) {
		return new SimpleBookmark(this, newKey);
	}

	@Override
	public int compareTo(Bookmark o) {
		return key.compareTo(o.getKey());
	}
}
