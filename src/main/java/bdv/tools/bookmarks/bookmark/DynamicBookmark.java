package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class DynamicBookmark extends Bookmark{

	public static final String XmlElementBookmarkName = "DynamicBookmark" ;
	public static final String XmlElementTimepointName = "timepoint" ;
	
	private int timepoint;

	public DynamicBookmark(Element element){
		super(element);
	}
	
	public DynamicBookmark(String key, AffineTransform3D transform, int timepoint) {
		super(key, transform);
		this.timepoint = timepoint;
	}
	
	public int getTimepoint() {
		return timepoint;
	}

	public void setTimepoint(int timepoint) {
		this.timepoint = timepoint;
	}

	@Override
	public Element toXmlNode() {
		final Element elemBookmark = new Element( XmlElementBookmarkName );
		elemBookmark.addContent( XmlHelpers.textElement( XmlElementKeyName, this.key ) );
		elemBookmark.addContent( XmlHelpers.affineTransform3DElement( XmlElementTransformName, this.transform ) );
		elemBookmark.addContent( XmlHelpers.intElement( XmlElementTimepointName, this.timepoint ) );
		return elemBookmark;
	}
	
	@Override
	public void restoreFromXml(Element element) {
		this.key = XmlHelpers.getText( element, XmlElementKeyName );
		this.transform = XmlHelpers.getAffineTransform3D( element, XmlElementTransformName );
		this.timepoint = XmlHelpers.getInt( element, XmlElementTimepointName );
	}
}
