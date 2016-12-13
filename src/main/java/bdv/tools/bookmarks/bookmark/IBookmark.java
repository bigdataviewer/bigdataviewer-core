package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

import net.imglib2.realtransform.AffineTransform3D;

public interface IBookmark {
	
	String getKey();
	
	Element toXmlNode();
	
	IBookmark copy();
}
