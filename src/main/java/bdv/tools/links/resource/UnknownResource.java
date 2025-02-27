package bdv.tools.links.resource;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.tools.JsonUtils;
import bdv.tools.links.ResourceConfig;
import bdv.tools.links.ResourceCreationException;
import bdv.tools.links.ResourceManager;
import bdv.tools.links.ResourceSpec;

/**
 * Used for resources that do not have associated specs.
 * <p>
 * Equality is Object identity. This should make sure that ResourceSpecs
 * wrapping {@link UnknownResource} are never equal unless they are the same
 * instance.
 */
public interface UnknownResource
{
	class Spec< T > implements ResourceSpec< T >
	{
		@Override
		public T create( final ResourceManager resources ) throws ResourceCreationException
		{
			throw new ResourceCreationException( "UnknownResource cannot be created" );
		}

		@Override
		public ResourceConfig getConfig( final ResourceManager resources )
		{
			return new Config();
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

	@JsonUtils.JsonIo( jsonType = "UnknownResource.Spec", type = UnknownResource.Spec.class )
	class SpecAdapter implements JsonDeserializer< Spec >, JsonSerializer< Spec >
	{
		@Override
		public Spec deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
		{
			return new Spec<>();
		}

		@Override
		public JsonElement serialize(
				final Spec src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			return new JsonObject();
		}
	}

	@JsonUtils.JsonIo( jsonType = "UnknownResource.Config", type = UnknownResource.Config.class )
	class ConfigAdapter implements JsonDeserializer< Config >, JsonSerializer< Config >
	{
		@Override
		public Config deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
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
