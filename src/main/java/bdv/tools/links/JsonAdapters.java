package bdv.tools.links;

import static bdv.tools.JsonUtils.asObject;
import static bdv.tools.JsonUtils.asString;
import static bdv.tools.JsonUtils.getDouble;
import static bdv.tools.JsonUtils.getMemberOptional;
import static bdv.tools.JsonUtils.getString;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.tools.JsonUtils;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;

class JsonAdapters
{
	@JsonUtils.JsonIo( jsonType = "BdvPropertiesV0.SourceConverterConfig", type = BdvPropertiesV0.SourceConverterConfig.class )
	public static class SourceConverterConfigAdapter implements JsonDeserializer< BdvPropertiesV0.SourceConverterConfig >, JsonSerializer< BdvPropertiesV0.SourceConverterConfig >
	{
		@Override
		public BdvPropertiesV0.SourceConverterConfig deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = asObject( json );
			final boolean hasColor = getMemberOptional( obj, "color" ) != null;
			final int color = hasColor ? ( int ) Long.parseLong( getString( obj, "color" ), 16 ) : -1;
			final double min = getDouble( obj, "min" );
			final double max = getDouble( obj, "max" );
			final double minBound = getDouble( obj, "minBound" );
			final double maxBound = getDouble( obj, "maxBound" );
			return new BdvPropertiesV0.SourceConverterConfig( hasColor, color, min, max, minBound, maxBound );
		}

		@Override
		public JsonElement serialize(
				final BdvPropertiesV0.SourceConverterConfig src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			if ( src.hasColor )
				obj.addProperty( "color", String.format( "%08x", src.color ) );
			obj.addProperty( "min", src.min );
			obj.addProperty( "max", src.max );
			obj.addProperty( "minBound", src.minBound );
			obj.addProperty( "maxBound", src.maxBound );
			return obj;
		}
	}

	@JsonUtils.JsonIo( jsonType = "BdvPropertiesV0.SourceGroupConfig", type = BdvPropertiesV0.SourceGroupConfig.class )
	public static class SourceGroupConfigAdapter implements JsonDeserializer< BdvPropertiesV0.SourceGroupConfig >, JsonSerializer< BdvPropertiesV0.SourceGroupConfig >
	{
		@Override
		public BdvPropertiesV0.SourceGroupConfig deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context )
		{
			final JsonObject obj = asObject( json );
			final String name = getMemberOptional( obj, "name" ) == null ? null : getString( obj, "name" );
			final int[] sources = context.deserialize( getMemberOptional( obj, "sources" ), int[].class );
			return new BdvPropertiesV0.SourceGroupConfig( name, sources == null ? new int[ 0 ] : sources );
		}

		@Override
		public JsonElement serialize(
				final BdvPropertiesV0.SourceGroupConfig src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final JsonObject obj = new JsonObject();
			if ( src.name != null )
				obj.addProperty( "name", src.name );
			obj.add( "sources", context.serialize( src.sources ) );
			return obj;
		}
	}

	@JsonUtils.JsonIo( jsonType = "DisplayMode", type = DisplayMode.class )
	public static class DisplayModeAdapter implements JsonDeserializer< DisplayMode >, JsonSerializer< DisplayMode >
	{
		@Override
		public DisplayMode deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
		{
			final String mode = asString( json );
			switch ( mode )
			{
			case "single-source":
				return DisplayMode.SINGLE;
			case "single-group":
				return DisplayMode.GROUP;
			case "fused-source":
				return DisplayMode.FUSED;
			case "fused-group":
				return DisplayMode.FUSEDGROUP;
			default:
				throw new JsonParseException( "Unsupported display mode: " + mode );
			}
		}

		@Override
		public JsonElement serialize(
				final DisplayMode src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final String mode;
			switch ( src )
			{
			case SINGLE:
				mode = "single-source";
				break;
			case GROUP:
				mode = "single-group";
				break;
			case FUSED:
				mode = "fused-source";
				break;
			case FUSEDGROUP:
				mode = "fused-group";
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + src);
			}
			return new JsonPrimitive( mode );
		}
	}

	@JsonUtils.JsonIo( jsonType = "Interpolation", type = Interpolation.class )
	public static class InterpolationAdapter implements JsonDeserializer< Interpolation >, JsonSerializer< Interpolation >
	{
		@Override
		public Interpolation deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
		{
			final String mode = asString( json );
			switch ( mode )
			{
			case "nearest":
				return Interpolation.NEARESTNEIGHBOR;
			case "linear":
				return Interpolation.NLINEAR;
			default:
				throw new JsonParseException( "Unsupported interpolation mode: " + mode );
			}
		}

		@Override
		public JsonElement serialize(
				final Interpolation src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			final String mode;
			switch ( src )
			{
			case NEARESTNEIGHBOR:
				mode = "nearest";
				break;
			case NLINEAR:
				mode = "linear";
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + src);
			}
			return new JsonPrimitive( mode );
		}
	}
}
