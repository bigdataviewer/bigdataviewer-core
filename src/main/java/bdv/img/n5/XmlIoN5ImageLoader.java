/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.img.n5;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.net.URI;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.jdom2.Element;

import mpicbg.spim.data.SpimDataInstantiationException;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.ImgLoaders;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import net.imglib2.util.Cast;

@ImgLoaderIo( format = "bdv.n5", type = N5ImageLoader.class )
public class XmlIoN5ImageLoader implements XmlIoBasicImgLoader< N5ImageLoader >
{
	public static boolean PREFER_URI_FOR_LOCAL_FILES = true;

	@Override
	public Element toXml( final N5ImageLoader imgLoader, final File basePath )
	{
		return toXml( imgLoader, basePath == null ? null : basePath.toURI() );
	}

	@Override
	public Element toXml( final N5ImageLoader imgLoader, final URI basePathURI )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.n5" );
		elem.setAttribute( "version", "1.1" );
		final File n5File = getLocalFile( imgLoader.getN5URI() );
		if ( !PREFER_URI_FOR_LOCAL_FILES && n5File != null )
		{
			final File basePath = basePathURI == null ? null : new File( basePathURI );
			elem.addContent( XmlHelpers.pathElement( "n5", n5File, basePath ) );
		}
		else
		{
			elem.addContent( XmlHelpers.pathElementURI( "n5", imgLoader.getN5URI(), basePathURI ) );
		}
		return elem;
	}

	private static File getLocalFile( final URI uri )
	{
		if ( "file".equalsIgnoreCase( uri.getScheme() ) )
			return new File( uri );
		else if ( uri.getScheme() == null )
			return new File( uri.getPath() );
		else
			return null;
	}

	@Override
	public N5ImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		return fromXml( elem, basePath.toURI(), sequenceDescription );
	}

	@Override
	public N5ImageLoader fromXml( final Element elem, final URI basePathURI, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
//		final String version = elem.getAttributeValue( "version" );
		final URI uri = XmlHelpers.loadPathURI( elem, "n5", basePathURI );

		// try to open with N5FSReader if URI is a file
		try
		{
			final String scheme = uri.getScheme();
			final boolean hasScheme = scheme != null;
			if ( !hasScheme || "file".equalsIgnoreCase( uri.getScheme() ) )
			{
				final String path = hasScheme
						? new File( uri ).getAbsolutePath()
						: uri.getPath();
				final N5FSReader n5 = new N5FSReader( path );
				return new N5ImageLoader( n5, uri, sequenceDescription );
			}
		}
		catch ( Exception e ) {}

		// try to open with "bdv.n5.cloud" format, if XmlIo for that can be discovered
		try
		{
			final XmlIoBasicImgLoader< ? > io = ImgLoaders.createXmlIoForFormat( "bdv.n5.cloud" );
			return Cast.unchecked( io.fromXml( elem, basePathURI, sequenceDescription ) );
		}
		catch ( SpimDataInstantiationException e )
		{
			return null;
		}
	}
}
