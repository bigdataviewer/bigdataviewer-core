package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

public interface Bookmark extends Comparable<Bookmark> {
	
	String getKey();
	
	Element toXmlNode();
	
	Bookmark copy();
	
	Bookmark copy(String newKey);
}
