package bdv.tools.movie.serilizers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;

public class AffineTransform3DJsonSerializer implements JsonSerializerDeserializer<AffineTransform3D> {

    public AffineTransform3D deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        double[] data = context.deserialize(json, double[].class);
        AffineTransform3D t = new AffineTransform3D();
        t.set(data);
        return t;
    }

    public JsonElement serialize(AffineTransform3D src, Type typeOfSrc, JsonSerializationContext context) {
        double[] data = new double[12];
        src.toArray(data);
        return context.serialize(data);
    }
}