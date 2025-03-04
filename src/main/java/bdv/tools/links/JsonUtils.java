package bdv.tools.links;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bdv.img.remote.AffineTransform3DJsonSerializer;
import bdv.tools.links.BdvPropertiesV0.Anchor;
import net.imglib2.realtransform.AffineTransform3D;

class JsonUtils
{
	static String prettyPrint( final JsonElement jsonElement )
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson( jsonElement );
	}

	static JsonElement toJson( final BdvPropertiesV0 properties )
	{
		final JsonObject bdv = new JsonObject();
		bdv.addProperty("version", 0);
		bdv.add( "properties", gson().toJsonTree( properties ) );
		final JsonObject root = new JsonObject();
		root.add( "bdv", bdv );
		return root;
	}

	static BdvPropertiesV0 fromJson( final String json )
	{
		final JsonElement root = JsonParser.parseString( json );
		final JsonObject bdv = memberAsObject( root, "bdv", "No \"bdv\" element found" );
		final int version = memberAsInt( bdv, "version", "No \"bdv\" version found" );
		if ( version == 0 )
		{
			// TODO move to new class BdvPropertiesSerializerV0 ?
			return gson().fromJson( bdv.getAsJsonObject( "properties" ), BdvPropertiesV0.class );
		}
		throw new IllegalArgumentException( "Unsupported version: " + version );
	}

	static Gson gson()
	{
		return gson( new GsonBuilder() );
	}

	static Gson gson( final GsonBuilder gsonBuilder )
	{
//		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter());
//		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter());
//		gsonBuilder.disableHtmlEscaping();
		gsonBuilder.registerTypeAdapter( Anchor.class, new Anchor.JsonAdapter());
		gsonBuilder.registerTypeAdapter( AffineTransform3D.class, new AffineTransform3DJsonSerializer() );
		return gsonBuilder.create();
	}

	private static int memberAsInt( final JsonElement element, final String member, final String error )
	{
		if ( element.isJsonObject() ) {
			final JsonElement obj = element.getAsJsonObject().get( member );
			if ( obj != null && obj.isJsonPrimitive() ) {
				return obj.getAsInt();
			}
		}
		throw new IllegalArgumentException( error );
	}

	private static JsonObject memberAsObject( final JsonElement element, final String member, final String error ) {
		if ( element.isJsonObject() ) {
			final JsonElement obj = element.getAsJsonObject().get( member );
			if ( obj != null && obj.isJsonObject() ) {
				return obj.getAsJsonObject();
			}
		}
		throw new IllegalArgumentException( error );
	}





	public static void main( String[] args )
	{
		final String json = "{\"bdv\":{\"version\":0,\"properties\":{\"transform\":[0.8352346636377921,-8.903934919667648E-4,-8.858658796824059E-4,2.866999656214432,8.898857008893401E-4,0.8352349965516729,-4.791031507569769E-4,143.0947248487617,8.863759736561795E-4,4.781587802977226E-4,0.8352350008248438,-96.58699637731013],\"timepoint\":0,\"panelsize\":[800,609],\"mousepos\":[460,360]}}}";
		System.out.println( "fromJson( json ) = " + fromJson( json ) );
		System.out.println( prettyPrint( toJson( fromJson( json ) ) ) );
	}
}
