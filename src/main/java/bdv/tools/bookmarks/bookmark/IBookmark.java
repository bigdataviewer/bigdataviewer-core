package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

public interface IBookmark {
	
	String getKey();
	
	String getTypeName();
	
	Element toXmlNode();
	
	IBookmark copy();
	
	IBookmark copy(String newKey);
}
