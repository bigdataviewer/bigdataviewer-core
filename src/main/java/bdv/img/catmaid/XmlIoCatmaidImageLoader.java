/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.img.catmaid;

import java.io.File;

import org.jdom2.Element;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;

@ImgLoaderIo( format = "catmaid", type = CatmaidImageLoader.class )
public class XmlIoCatmaidImageLoader
		implements XmlIoBasicImgLoader< CatmaidImageLoader >
{
	@Override
	public Element toXml( final CatmaidImageLoader imgLoader, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public CatmaidImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final long width = Long.parseLong( elem.getChildText( "width" ) );
		final long height = Long.parseLong( elem.getChildText( "height" ) );
		final long depth = Long.parseLong( elem.getChildText( "depth" ) );

		final double resXY = Double.parseDouble( elem.getChildText( "resXY" ) );
		final double resZ = Double.parseDouble( elem.getChildText( "resZ" ) );

		final String urlFormat = elem.getChildText( "urlFormat" );

		final int tileWidth = Integer.parseInt( elem.getChildText( "tileWidth" ) );
		final int tileHeight = Integer.parseInt( elem.getChildText( "tileHeight" ) );
		
		final String blockWidthString = elem.getChildText( "blockWidth" );
		final String blockHeightString = elem.getChildText( "blockHeight" );
		final String blockDepthString = elem.getChildText( "blockDepth" );
		
		final int blockWidth = blockWidthString == null ? tileWidth : Integer.parseInt( blockWidthString );
		final int blockHeight = blockHeightString == null ? tileHeight : Integer.parseInt( blockHeightString );
		final int blockDepth = blockDepthString == null ? 1 : Integer.parseInt( blockDepthString );
		
		System.out.println( String.format( "Block size = (%d, %d, %d)", blockWidth, blockHeight, blockDepth ) );
		
		final String numScalesString = elem.getChildText( "numScales" );
		int numScales;
		if ( numScalesString == null )
			numScales = CatmaidImageLoader.getNumScales( width, height, tileWidth, tileHeight );
		else
			numScales = Integer.parseInt( numScalesString );

		final int[][] blockSize = new int[ numScales ][];
		for ( int i = 0; i < numScales; ++i )
			blockSize[ i ] = new int[]{ blockWidth, blockHeight, blockDepth };
		
		return new CatmaidImageLoader(
				width,
				height,
				depth,
				resZ / resXY,
				urlFormat,
				tileWidth,
				tileHeight,
				blockSize );
	}
}
