/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.tools.bookmarks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.IBookmark;

public class Bookmarks {
	private final HashMap<String, IBookmark> bookmarks;

	public Bookmarks() {
		bookmarks = new HashMap<>();
	}

	public Element toXml() {
		final Element elem = new Element("Bookmarks");
		for (final Entry<String, IBookmark> entry : bookmarks.entrySet()) {
			final IBookmark bookmark = entry.getValue();
			Element elemBookmark = bookmark.toXmlNode();
			elem.addContent(elemBookmark);
		}
		return elem;
	}

	public void restoreFromXml(final Element parent) {
		bookmarks.clear();
		// TODO clear active bookmark

		final Element elemBookmarks = parent.getChild("Bookmarks");
		if (elemBookmarks == null)
			return;

		// TODO abstract
		for (final Element elem : elemBookmarks.getChildren(SimpleBookmark.XML_ELEM_BOOKMARK_NAME)) {
			SimpleBookmark bookmark = new SimpleBookmark(elem);
			bookmarks.put(bookmark.getKey(), bookmark);
		}

		for (final Element elem : elemBookmarks.getChildren(DynamicBookmark.XML_ELEM_BOOKMARK_NAME)) {
			DynamicBookmark bookmark = new DynamicBookmark(elem);
			bookmarks.put(bookmark.getKey(), bookmark);
		}
	}

	public void put(final IBookmark bookmark) {
		bookmarks.put(bookmark.getKey(), bookmark);
	}

	public Collection<IBookmark> getAll(){
		return bookmarks.values();
	}
	
	public IBookmark get(final String key) {
		return bookmarks.get(key);
	}
	
	public <T extends IBookmark> T get(final String key, Class<T> clazz) {
		IBookmark bookmark = bookmarks.get(key);
		
		if(clazz.isInstance(bookmark)){
			return clazz.cast(bookmark);
		}

		return null;
	}
	
	public IBookmark remove(final String key){
		return bookmarks.remove(key);
	}
	
	public void rename(String oldKey, String newKey){
		IBookmark bookmark = remove(oldKey);
		if(bookmark != null){
			IBookmark newBookmark = bookmark.copy(newKey);
			put(newBookmark);
		}
	}

	// TODO replace with generic method
	public SimpleBookmark getSimpleBookmark(final String key) {
		IBookmark bookmark = get(key);
		if (bookmark instanceof SimpleBookmark) {
			return (SimpleBookmark) bookmark;
		}

		return null;
	}

	// TODO replace with generic method
	public DynamicBookmark getDynamicBookmark(final String key) {
		IBookmark bookmark = get(key);
		if (bookmark instanceof DynamicBookmark) {
			return (DynamicBookmark) bookmark;
		}

		return null;
	}

	public AffineTransform3D getTransform(final String key, final int currentTimepoint, final double cX, final double cY) {
		
		final SimpleBookmark simpleBookmark = getSimpleBookmark(key);
		if (simpleBookmark != null) {
			return simpleBookmark.getTransform();
		}

		final DynamicBookmark dynamicBookmark = getDynamicBookmark(key);
		if (dynamicBookmark != null) {
			return dynamicBookmark.getInterpolatedTransform(currentTimepoint, cX, cY);
		}

		return null;
	}
}
