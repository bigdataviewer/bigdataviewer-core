package bdv.tools.movie.serilizers;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public interface JsonSerializerDeserializer<T> extends JsonSerializer<T>, JsonDeserializer<T> {
}