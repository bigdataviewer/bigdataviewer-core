package bdv.tools.links;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import bdv.img.remote.AffineTransform3DJsonSerializer;
import net.imglib2.realtransform.AffineTransform3D;

class JsonUtils
{
	static String prettyPrint( final JsonElement jsonElement )
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson( jsonElement );
	}

	static String toJson( final BdvPropertiesV0 properties )
	{
		final JsonObject bdv = new JsonObject();
		bdv.addProperty("version", 0);
		bdv.add( "properties", gson().toJsonTree( properties ) );
		final JsonObject root = new JsonObject();
		root.add( "bdv", bdv );
		return root.toString();
	}

	static BdvPropertiesV0 fromJson( final String json )
	{
		final JsonElement root = JsonParser.parseString( json );
		final JsonObject bdv = memberAsObject( root, "bdv", "No \"bdv\" element found" );
		final int version = memberAsInt( bdv, "version", "No \"bdv\" version found" );
		if ( version == 0 )
		{
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

		final String json = "{\"bdv\":{\"version\":0,\"properties\":{\"transform\":[-0.3690909599114589,-0.5202502260220657,-0.5391939229411717,755.2832391159902,0.7197828327445233,-0.07926133818124305,-0.41623170813508614,95.27157099521344,0.2080938679994459,-0.6485953002410446,0.4833628031896136,50.42108888325292]}}}";
		System.out.println( "fromJson( json ).transform() = " + fromJson( json ).transform() );
	}
}
