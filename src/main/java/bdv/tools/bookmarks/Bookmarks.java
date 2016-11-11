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

import java.util.HashMap;
import java.util.Map.Entry;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;

public class Bookmarks
{
	private final HashMap< String, Bookmark > bookmarks;

	public Bookmarks()
	{
		bookmarks = new HashMap<>();
	}

	public Element toXml()
	{
		final Element elem = new Element( "Bookmarks" );
		for ( final Entry< String, Bookmark > entry : bookmarks.entrySet() )
		{
			final Bookmark bookmark = entry.getValue();
			Element elemBookmark = bookmark.toXmlNode();
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

		for ( final Element elem : elemBookmarks.getChildren( Bookmark.XmlElementBookmarkName ) )
		{
			Bookmark bookmark = new Bookmark(elem);	
			bookmarks.put( bookmark.getKey(), bookmark );
		}
		
		for ( final Element elem : elemBookmarks.getChildren( DynamicBookmark.XmlElementBookmarkName ) )
		{
			DynamicBookmark bookmark = new DynamicBookmark(elem);
			bookmarks.put( bookmark.getKey(), bookmark );
		}
	}

	public void put( final Bookmark bookmark )
	{
		bookmarks.put( bookmark.getKey(), bookmark );
	}
	
	public Bookmark getBookmark( final String key )
	{
		return bookmarks.get( key );
	}

	public AffineTransform3D get( final String key )
	{
		Bookmark bookmark = getBookmark( key );
		if(bookmark != null){
			return bookmark.getAffineTransform3D();
		}
		
		return null;
	}
}

