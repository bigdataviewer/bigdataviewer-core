package bdv.tools.bookmarks.bookmark;

import org.jdom2.Element;

import mpicbg.spim.data.XmlHelpers;

public abstract class Bookmark implements Comparable<Bookmark> {

	public static final String XML_ELEM_KEY_NAME = "key";
	public static final String XML_ELEM_KEY_TITLE = "title";
	public static final String XML_ELEM_KEY_DESCRIPTION = "description";

	private final String key;
	private String title;
	private String description;

	public Bookmark(String key) {
		this.key = key;
	}

	public Bookmark(Element element) {
		this.key = XmlHelpers.getText(element, XML_ELEM_KEY_NAME);
		this.title = XmlHelpers.getText(element, XML_ELEM_KEY_TITLE, "");
		this.description = XmlHelpers.getText(element, XML_ELEM_KEY_DESCRIPTION, "");
	}

	public String getKey() {
		return this.key;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int compareTo(Bookmark o) {
		return key.compareTo(o.key);
	}

	public abstract Element toXmlNode();

	public abstract Bookmark copy();

	public abstract Bookmark copy(String newKey);
}
