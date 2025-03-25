package bdv.tools.links.resource;

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
import bdv.tools.JsonUtils.Typed;
import bdv.tools.JsonUtils;
import bdv.tools.links.ResourceConfig;
import bdv.tools.links.ResourceCreationException;
import bdv.tools.links.ResourceSpec;
import bdv.tools.links.ResourceManager;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

public interface TransformedSourceResource
{
	class Spec implements ResourceSpec< SourceAndConverter< ? > >
	{
		private final ResourceSpec< SourceAndConverter< ? > > delegateSpec;

		public Spec( final ResourceSpec< SourceAndConverter< ? > > delegateSpec )
		{
			this.delegateSpec = delegateSpec;
		}

		@Override
		public SourceAndConverter< ? > create( final ResourceManager resources ) throws ResourceCreationException
		{
			final SourceAndConverter< ? > delegate = resources.getOrCreateResource( delegateSpec );
			return BigDataViewer.wrapWithTransformedSource( delegate, resources );
		}

		@Override
		public ResourceConfig getConfig( final ResourceManager resources )
		{
			final ResourceConfig delegateConfig = delegateSpec.getConfig( resources );
			final SourceAndConverter< ? > soc = resources.getResource( this );
			final TransformedSource< ? > ts = ( TransformedSource< ? > ) soc.getSpimSource();
			final AffineTransform3D transform = new AffineTransform3D();
			ts.getFixedTransform( transform );
			return new Config( delegateConfig, transform );
		}

		@Override
		public String toString()
		{
			return "TransformedSourceResource.Spec{" +
					"delegateSpec=" + delegateSpec +
					'}';
		}

		@Override
		public boolean equals( final Object o )
		{
			if ( !( o instanceof Spec ) )
				return false;
			final Spec that = ( Spec ) o;
			return Objects.equals( delegateSpec, that.delegateSpec );
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode( delegateSpec );
		}
	}

	class Config implements ResourceConfig
	{
		private final ResourceConfig delegateConfig;

		private final AffineTransform3D transform;

		private Config(
				final ResourceConfig delegateConfig,
				final AffineTransform3D transform )
		{
			this.delegateConfig = delegateConfig;
			this.transform = transform;
		}

		@Override
		public void apply( final ResourceSpec< ? > spec, final ResourceManager resources )
		{
			if ( spec instanceof UnknownResource.Spec )
				return;

			if ( spec instanceof Spec )
				delegateConfig.apply( ( ( Spec ) spec ).delegateSpec, resources );

			final Object resource = resources.getResource( spec );
			if ( resource instanceof SourceAndConverter )
			{
				SourceAndConverter< ? > soc = ( SourceAndConverter< ? > ) resource;
				final Source< ? > source = soc.getSpimSource();
				if ( source instanceof TransformedSource )
				{
					final TransformedSource< ? > ts = ( TransformedSource< ? > ) source;
					ts.setFixedTransform( transform );
				}
			}
		}
	}

	@JsonUtils.JsonIo( jsonType = "TransformedSourceResource.Spec", type = TransformedSourceResource.Spec.class )
	class SpecAdapter implements JsonDeserializer< Spec >, JsonSerializer< Spec >
	{
		@Override
		public Spec deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = json.getAsJsonObject();
			final Typed< ResourceSpec< SourceAndConverter< ? > > > delegateSpec = context.deserialize( obj.get( "delegate" ), Typed.class );
			return new Spec( delegateSpec.get() );
		}

		@Override
		public JsonElement serialize(
				final Spec src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			obj.add( "delegate", context.serialize( typed( src.delegateSpec ) ) );
			return obj;
		}
	}

	@JsonUtils.JsonIo( jsonType = "TransformedSourceResource.Config", type = TransformedSourceResource.Config.class )
	class ConfigAdapter implements JsonDeserializer< Config >, JsonSerializer< Config >
	{
		@Override
		public Config deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = json.getAsJsonObject();
			final Typed< ResourceConfig > delegateConfig = context.deserialize( obj.get( "delegate" ), Typed.class );
			final AffineTransform3D transform = context.deserialize( obj.get( "transform" ), AffineTransform3D.class );
			return new Config( delegateConfig.get(), transform );
		}

		@Override
		public JsonElement serialize(
				final Config src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			obj.add( "delegate", context.serialize( typed( src.delegateConfig ) ) );
			obj.add( "transform", context.serialize(  src.transform ) );
			return obj;
		}
	}
}
