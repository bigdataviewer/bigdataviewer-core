package bdv.tools.links.resource;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.JsonUtils;
import bdv.tools.links.ResourceConfig;
import bdv.tools.links.ResourceCreationException;
import bdv.tools.links.ResourceSpec;
import bdv.tools.links.ResourceManager;
import mpicbg.spim.data.SpimDataException;

public interface SpimDataMinimalFileResource
{
	class Spec implements ResourceSpec< SpimDataMinimal >
	{
		// NB: URI to a local file!
		private final URI xmlURI;

		public Spec( final URI xmlURI )
		{
			this.xmlURI = xmlURI;
		}

		public Spec( final String xmlFilename )
		{
			this( new File( xmlFilename ).toURI() );
		}

		@Override
		public SpimDataMinimal create( ResourceManager resources ) throws ResourceCreationException
		{
			try
			{
				final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( toFile( xmlURI ).toString() );
				resources.put( spimData, this );
				return spimData;
			}
			catch ( SpimDataException e )
			{
				throw new ResourceCreationException( e );
			}
		}

		@Override
		public ResourceConfig getConfig( final ResourceManager resources )
		{
			return new Config();
		}

		private static File toFile( final URI uri )
		{
			if ( "file".equalsIgnoreCase( uri.getScheme() ) )
				return new File( uri );
			throw new IllegalArgumentException( uri + " is not a file" );
		}

		@Override
		public String toString()
		{
			return "SpimDataMinimalFileResource.Spec{" +
					"xmlURI=" + xmlURI +
					'}';
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( !( o instanceof Spec ) )
				return false;
			final Spec that = ( Spec ) o;
			return Objects.equals( xmlURI.normalize(), that.xmlURI.normalize() );
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode( xmlURI.normalize() );
		}
	}

	class Config implements ResourceConfig
	{
		@Override
		public void apply( final ResourceSpec< ? > spec, final ResourceManager resources )
		{
			// nothing to configure
		}
	}

	@JsonUtils.JsonIo( jsonType = "SpimDataMinimalFileResource.Spec", type = SpimDataMinimalFileResource.Spec.class )
	class SpecAdapter implements JsonDeserializer< Spec >, JsonSerializer< Spec >
	{
		@Override
		public Spec deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = json.getAsJsonObject();
			final String uri = obj.get( "uri" ).getAsString();
			return new Spec( URI.create( uri ) );
		}

		@Override
		public JsonElement serialize(
				final Spec src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			obj.addProperty( "uri", src.xmlURI.toString() );
			return obj;
		}
	}

	@JsonUtils.JsonIo( jsonType = "SpimDataMinimalFileResource.Config", type = SpimDataMinimalFileResource.Config.class )
	class ConfigAdapter implements JsonDeserializer< Config >, JsonSerializer< Config >
	{
		@Override
		public Config deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			return new Config();
		}

		@Override
		public JsonElement serialize(
				final Config src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			return new JsonObject();
		}
	}
}
