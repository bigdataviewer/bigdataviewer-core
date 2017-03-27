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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jdom2.Element;

import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import net.imglib2.realtransform.AffineTransform3D;

public class Bookmarks
{
	private final LinkedHashMap< String, Bookmark > bookmarks;

	private final List< BookmarksCollectionChangedListener > listeners;

	public Bookmarks()
	{
		bookmarks = new LinkedHashMap<>();
		listeners = new ArrayList<>();
	}

	public void addListener( BookmarksCollectionChangedListener listener )
	{
		listeners.add( listener );
	}

	public void removeListener( BookmarksCollectionChangedListener listener )
	{
		listeners.remove( listener );
	}

	private void fireBookmarksCollectionChangedListener()
	{
		for ( BookmarksCollectionChangedListener l : listeners )
		{
			l.bookmarksCollectionChanged();
		}
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
		// TODO clear active bookmark

		final Element elemBookmarks = parent.getChild( "Bookmarks" );
		if ( elemBookmarks == null )
			return;

		// TODO abstract
		for ( final Element elem : elemBookmarks.getChildren( SimpleBookmark.XML_ELEM_BOOKMARK_NAME ) )
		{
			SimpleBookmark bookmark = new SimpleBookmark( elem );
			bookmarks.put( bookmark.getKey(), bookmark );
		}

		for ( final Element elem : elemBookmarks.getChildren( DynamicBookmark.XML_ELEM_BOOKMARK_NAME ) )
		{
			DynamicBookmark bookmark = new DynamicBookmark( elem );
			bookmarks.put( bookmark.getKey(), bookmark );
		}

		fireBookmarksCollectionChangedListener();
	}

	public void put( final Bookmark bookmark )
	{
		bookmarks.put( bookmark.getKey(), bookmark );

		fireBookmarksCollectionChangedListener();
	}

	public Bookmark remove( final String key )
	{
		Bookmark bookmark = bookmarks.remove( key );
		if ( bookmark != null )
		{
			fireBookmarksCollectionChangedListener();
		}

		return bookmark;
	}

	public Bookmark rename( final String oldKey, final String newKey )
	{
		final Bookmark oldBookmark = get( oldKey );
		if ( oldBookmark != null )
		{
			final Bookmark newBookmark = oldBookmark.copy( newKey );
			final Bookmark replacedBookmark = bookmarks.replace( oldKey, newBookmark );
			if ( replacedBookmark != null ){
				fireBookmarksCollectionChangedListener();
				return newBookmark;
			}
		}
		return null;
	}

	public boolean containsKey( final String key )
	{
		return bookmarks.containsKey( key );
	}

	public Collection< Bookmark > getAll()
	{
		return bookmarks.values();
	}

	public Bookmark get( final String key )
	{
		return bookmarks.get( key );
	}

	public < T extends Bookmark > T get( final String key, Class< T > clazz )
	{
		Bookmark bookmark = bookmarks.get( key );

		if ( clazz.isInstance( bookmark ) ) { return clazz.cast( bookmark ); }

		return null;
	}

	// TODO replace with generic method
	public SimpleBookmark getSimpleBookmark( final String key )
	{
		Bookmark bookmark = get( key );
		if ( bookmark instanceof SimpleBookmark ) { return ( SimpleBookmark ) bookmark; }

		return null;
	}

	// TODO replace with generic method
	public DynamicBookmark getDynamicBookmark( final String key )
	{
		Bookmark bookmark = get( key );
		if ( bookmark instanceof DynamicBookmark ) { return ( DynamicBookmark ) bookmark; }

		return null;
	}

	public AffineTransform3D getTransform( final String key, final int currentTimepoint, final double cX, final double cY )
	{

		final SimpleBookmark simpleBookmark = getSimpleBookmark( key );
		if ( simpleBookmark != null ) { return simpleBookmark.getTransform(); }

		final DynamicBookmark dynamicBookmark = getDynamicBookmark( key );
		if ( dynamicBookmark != null ) { return dynamicBookmark.getInterpolatedTransform( currentTimepoint, cX, cY ); }

		return null;
	}
}
