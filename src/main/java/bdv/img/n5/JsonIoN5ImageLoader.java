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

import static bdv.zarr.DebugUtils.uri;

import java.net.URI;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import mpicbg.spim.data.zarr.JsonImgLoaderIo;
import mpicbg.spim.data.zarr.JsonIoBasicImgLoader;

@JsonImgLoaderIo( format = "bdv.n5", type = N5ImageLoader.class )
public class JsonIoN5ImageLoader implements JsonIoBasicImgLoader< N5ImageLoader >
{
	@Override
	public N5ImageLoader deserialize( final JsonElement json, final URI basePathURI, final JsonDeserializationContext context ) throws JsonParseException
	{
		System.out.println( "JsonIoN5ImageLoader.deserialize" );
		System.out.println( "json = " + json + ", basePathURI = " + basePathURI + ", context = " + context );
		System.out.println();

		return null;
	}

	@Override
	public JsonElement serialize( final N5ImageLoader imgLoader, final URI basePathURI, final JsonSerializationContext context )
	{
		System.out.println( "JsonIoN5ImageLoader.serialize" );
		System.out.println( "imgLoader = " + imgLoader + ", basePathURI = " + basePathURI + ", context = " + context );
		System.out.println();

		// basePathURI must an absolute URI.
		// basePathURI is the "SpimData/BasePath" URI resolved against the
		// containerURI if the "SpimData/BasePath" attribute is present, and
		// containerURI otherwise.
		if ( !basePathURI.isAbsolute() )
		{
			throw new IllegalArgumentException( "basePathURI must be absolute. basePathURI='" + basePathURI + "'" );
		}

		final URI uri = basePathURI.relativize( imgLoader.getN5URI() );
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("format", "bdv.n5");
		jsonObject.addProperty("version", "0.0.1");
		jsonObject.addProperty( "uri", uri.toString() );
		return jsonObject;
	}
}
