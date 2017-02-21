package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

public interface IBookmark extends Comparable<IBookmark> {
	
	String getKey();
	
	Element toXmlNode();
	
	IBookmark copy();
	
	IBookmark copy(String newKey);
}
