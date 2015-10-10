/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

public class Bookmarks
{
	private final HashMap< String, AffineTransform3D > bookmarks;

	public Bookmarks()
	{
		bookmarks = new HashMap< String, AffineTransform3D >();
	}

	public Element toXml()
	{
		final Element elem = new Element( "Bookmarks" );
		for ( final Entry< String, AffineTransform3D > entry : bookmarks.entrySet() )
		{
			final String key = entry.getKey();
			final AffineTransform3D transform = entry.getValue();

			final Element elemBookmark = new Element( "Bookmark" );
			elemBookmark.addContent( XmlHelpers.textElement( "key", key ) );
			elemBookmark.addContent( XmlHelpers.affineTransform3DElement( "transform", transform ) );
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

		for ( final Element elem : elemBookmarks.getChildren( "Bookmark" ) )
		{
			final String key = XmlHelpers.getText( elem, "key" );
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D( elem, "transform" );
			bookmarks.put( key, transform );
		}
	}

	public void put( final String key, final AffineTransform3D transform )
	{
		bookmarks.put( key, transform );
	}

	public AffineTransform3D get( final String key )
	{
		return bookmarks.get( key );
	}
}

