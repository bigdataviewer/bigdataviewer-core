package bdv.tools.links.resource;

import static bdv.tools.JsonUtils.asObject;
import static bdv.tools.JsonUtils.getInt;
import static bdv.tools.JsonUtils.getString;
import static bdv.tools.JsonUtils.getTyped;
import static bdv.tools.JsonUtils.typed;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.BigDataViewer;
import bdv.tools.JsonUtils;
import bdv.tools.links.ResourceConfig;
import bdv.tools.links.ResourceCreationException;
import bdv.tools.links.ResourceSpec;
import bdv.tools.links.ResourceManager;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

public interface SpimDataSetupSourceResource
{
	class Spec implements ResourceSpec< SourceAndConverter< ? > >
	{
		private final ResourceSpec< ? extends AbstractSpimData< ? > > spimDataSpec;

		private final int setupId;

		private final String name;

		public Spec(
				final ResourceSpec< ? extends AbstractSpimData< ? > > spimDataSpec,
				final int setupId,
				final String name )
		{
			this.spimDataSpec = spimDataSpec;
			this.setupId = setupId;
			this.name = name;
		}

		@Override
		public SourceAndConverter< ? > create( final ResourceManager resources ) throws ResourceCreationException
		{
			final AbstractSpimData< ? > spimData = resources.getOrCreateResource( spimDataSpec );
			try
			{
				return BigDataViewer.createSetupSourceNumericType( spimData, setupId, name, resources );
			}
			catch ( final Exception e )
			{
				throw new ResourceCreationException( e );
			}
		}

		@Override
		public ResourceConfig getConfig( final ResourceManager resources )
		{
			final ResourceConfig config = spimDataSpec.getConfig( resources );
			return new Config( config );
		}

		@Override
		public String toString()
		{
			return "SpimDataSetupSourceResource.Spec{" +
					"spimDataSpec=" + spimDataSpec +
					", setupId=" + setupId +
					", name='" + name + '\'' +
					'}';
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( !( o instanceof Spec ) )
				return false;
			final Spec that = ( Spec ) o;
			return setupId == that.setupId && Objects.equals( spimDataSpec, that.spimDataSpec );
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( spimDataSpec, setupId );
		}
	}

	class Config implements ResourceConfig
	{
		private final ResourceConfig spimDataConfig;

		private Config(
				final ResourceConfig spimDataConfig )
		{
			this.spimDataConfig = spimDataConfig;
		}

		@Override
		public void apply( final ResourceSpec< ? > spec, final ResourceManager resources )
		{
			if ( spec instanceof UnknownResource.Spec )
				return;

			if ( spec instanceof Spec )
				spimDataConfig.apply( ( ( Spec ) spec ).spimDataSpec, resources );
		}
	}

	@JsonUtils.JsonIo( jsonType = "SpimDataSetupSourceResource.Spec", type = SpimDataSetupSourceResource.Spec.class )
	class JsonAdapter implements JsonDeserializer< Spec >, JsonSerializer< Spec >
	{
		@Override
		public Spec deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = asObject( json );
			final ResourceSpec< ? extends AbstractSpimData< ? > > spimDataSpec = getTyped( context, obj, "spimData", ResourceSpec.class );
			final int setupId = getInt( obj, "setupId" );
			final String name = getString( obj, "name" );
			return new Spec( spimDataSpec, setupId, name );
		}

		@Override
		public JsonElement serialize(
				final Spec src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			obj.add( "spimData", context.serialize( typed( src.spimDataSpec ) ) );
			obj.addProperty( "setupId", src.setupId );
			obj.addProperty( "name", src.name );
			return obj;
		}
	}

	@JsonUtils.JsonIo( jsonType = "SpimDataSetupSourceResource.Config", type = SpimDataSetupSourceResource.Config.class )
	class ConfigAdapter implements JsonDeserializer< Config >, JsonSerializer< Config >
	{
		@Override
		public Config deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = asObject( json );
			final ResourceConfig spimDataConfig = getTyped( context, obj, "spimData", ResourceConfig.class );
			return new Config( spimDataConfig );
		}

		@Override
		public JsonElement serialize(
				final Config src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			obj.add( "spimData", context.serialize( typed( src.spimDataConfig ) ) );
			return obj;
		}
	}
}
