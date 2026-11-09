/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2026 BigDataViewer developers.
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
package bdv.tools.links;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import bdv.tools.JsonUtils;

/**
 * Malformed link JSON must be rejected with a {@code JsonParseException}, never
 * with another {@code RuntimeException}, because pasted clipboard text can be
 * anything.
 */
public class MalformedLinkJsonTest
{
	private final Gson gson = JsonUtils.gson();

	@Test
	public void validPropertiesRoundTrip()
	{
		final JsonElement json = gson.toJsonTree( new BdvPropertiesV0() );
		gson.fromJson( json, BdvPropertiesV0.class );

		final Links.VersionAndProperties vp = gson.fromJson(
				gson.toJsonTree( new Links.VersionAndProperties( 0, json ) ),
				Links.VersionAndProperties.class );
		assertEquals( 0, vp.version() );
	}

	private static final String UNKNOWN_SPEC = "{\"type\":\"UnknownResource.Spec\",\"data\":{}}";

	private static final String UNKNOWN_CONFIG = "{\"type\":\"UnknownResource.Config\",\"data\":{}}";

	private static final String CONVERTER_CONFIG = "{\"min\":0,\"max\":1,\"minBound\":0,\"maxBound\":1}";

	@Test
	public void validPropertiesWithSources()
	{
		final JsonObject json = gson.toJsonTree( new BdvPropertiesV0() ).getAsJsonObject();
		json.add( "sourceSpecs", JsonParser.parseString( "[" + UNKNOWN_SPEC + "," + UNKNOWN_SPEC + "]" ) );
		json.add( "sourceConfigs", JsonParser.parseString( "[" + UNKNOWN_CONFIG + "," + UNKNOWN_CONFIG + "]" ) );
		json.add( "converterConfigs", JsonParser.parseString( "[" + CONVERTER_CONFIG + ",null]" ) );
		json.add( "groupConfigs", JsonParser.parseString( "[{\"name\":\"g\",\"sources\":[0,1]},{}]" ) );
		json.add( "panelsize", JsonParser.parseString( "[0,0]" ) );
		final BdvPropertiesV0 properties = gson.fromJson( json, BdvPropertiesV0.class );
		properties.validateDeserialized();
		assertEquals( 2, properties.sourceSpecs().size() );
	}

	@Test
	public void malformedVersionAndProperties()
	{
		final List< String > inputs = Arrays.asList(
				"\"hello\"",
				"[]",
				"{}",
				"{\"bdv\":3}",
				"{\"bdv\":{}}",
				"{\"bdv\":{\"version\":\"x\",\"properties\":{}}}",
				"{\"bdv\":{\"version\":1.5,\"properties\":{}}}",
				"{\"bdv\":{\"version\":0}}",
				"{\"bdv\":{\"version\":0,\"properties\":[]}}" );
		for ( final String input : inputs )
			assertRejected( input, Links.VersionAndProperties.class );
	}

	@Test
	public void malformedProperties()
	{
		final List< String[] > cases = Arrays.asList(
				// attribute, malformed value
				new String[] { "displaymode", "[]" },
				new String[] { "displaymode", "{}" },
				new String[] { "displaymode", "\"no-such-mode\"" },
				new String[] { "interpolation", "{}" },
				new String[] { "transform", "[1,2,3]" },
				new String[] { "transform", "{}" },
				new String[] { "converterConfigs", "[\"x\"]" },
				new String[] { "converterConfigs", "[{}]" },
				new String[] { "converterConfigs", "[{\"min\":\"a\",\"max\":1,\"minBound\":0,\"maxBound\":1}]" },
				new String[] { "converterConfigs", "[{\"color\":\"zz\",\"min\":0,\"max\":1,\"minBound\":0,\"maxBound\":1}]" },
				new String[] { "converterConfigs", "[{\"color\":[],\"min\":0,\"max\":1,\"minBound\":0,\"maxBound\":1}]" },
				new String[] { "groupConfigs", "[5]" },
				new String[] { "groupConfigs", "[{\"name\":3}]" },
				new String[] { "groupConfigs", "[{\"sources\":{}}]" },
				new String[] { "sourceSpecs", "{}" },
				new String[] { "sourceSpecs", "[1]" },
				new String[] { "sourceSpecs", "[null]" },
				new String[] { "sourceSpecs", "[{\"type\":\"no-such-type\",\"data\":{}}]" },
				new String[] { "sourceSpecs", "[{\"type\":\"SpimDataMinimalFileResource.Spec\"}]" },
				new String[] { "sourceSpecs", "[{\"type\":\"SpimDataMinimalFileResource.Spec\",\"data\":{}}]" },
				new String[] { "sourceSpecs", "[{\"type\":\"SpimDataMinimalFileResource.Spec\",\"data\":{\"uri\":\"::not a uri\"}}]" },
				new String[] { "sourceSpecs", "[{\"type\":\"SpimDataSetupSourceResource.Spec\",\"data\":{\"setupId\":0,\"name\":\"a\"}}]" },
				new String[] { "sourceSpecs", "[{\"type\":\"TransformedSourceResource.Spec\",\"data\":{\"delegate\":7}}]" },
				new String[] { "sourceConfigs", "[{\"type\":\"TransformedSourceResource.Config\",\"data\":{\"delegate\":{\"type\":\"UnknownResource.Config\",\"data\":{}}}}]" },
				new String[] { "timepoint", "\"x\"" },
				new String[] { "activeSourceIndices", "{}" },
				// wrong element type in TypedList
				new String[] { "sourceSpecs", "[{\"type\":\"DisplayMode\",\"data\":\"fused-source\"}]" },
				new String[] { "sourceConfigs", "[{\"type\":\"UnknownResource.Spec\",\"data\":{}}]" },
				// incomplete or inconsistent properties (rejected by validateDeserialized())
				new String[] { "transform", "null" },
				new String[] { "displaymode", "null" },
				new String[] { "sourceSpecs", "null" },
				new String[] { "activeGroupIndices", "null" },
				new String[] { "panelsize", "[100]" },
				new String[] { "panelsize", "[-1,100]" },
				new String[] { "mousepos", "[1,2,3]" },
				new String[] { "groupConfigs", "[null]" },
				new String[] { "sourceSpecs", "[" + UNKNOWN_SPEC + "]" } );
		for ( final String[] c : cases )
		{
			final JsonObject json = gson.toJsonTree( new BdvPropertiesV0() ).getAsJsonObject();
			json.add( c[ 0 ], JsonParser.parseString( c[ 1 ] ) );
			assertRejected( json.toString(), BdvPropertiesV0.class );
		}
	}

	private void assertRejected( final String input, final Class< ? > type )
	{
		try
		{
			final Object result = gson.fromJson( JsonParser.parseString( input ), type );
			if ( result instanceof BdvPropertiesV0 )
				( ( BdvPropertiesV0 ) result ).validateDeserialized();
			fail( "expected JsonParseException for " + input );
		}
		catch ( final JsonParseException e )
		{
			assertTrue( e.getMessage() != null && !e.getMessage().isEmpty() );
		}
		catch ( final RuntimeException e )
		{
			throw new AssertionError( "expected JsonParseException for " + input + ", got " + e, e );
		}
	}
}
