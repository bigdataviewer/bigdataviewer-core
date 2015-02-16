package bdv.img.remote;

import java.lang.reflect.Type;

import net.imglib2.realtransform.AffineTransform3D;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AffineTransform3DJsonSerializer implements JsonDeserializer< AffineTransform3D >, JsonSerializer< AffineTransform3D >
{
	@Override
	public AffineTransform3D deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		final double[] data = context.deserialize( json, double[].class );
		final AffineTransform3D t = new AffineTransform3D();
		t.set( data );
		return t;
	}

	@Override
	public JsonElement serialize( final AffineTransform3D src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final double[] data = new double[ 12 ];
		src.toArray( data );
		return context.serialize( data );
	}
}
