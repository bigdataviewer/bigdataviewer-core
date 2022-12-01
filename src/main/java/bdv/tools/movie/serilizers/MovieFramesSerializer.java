package bdv.tools.movie.serilizers;

import bdv.tools.movie.preview.MovieFrameInst;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class MovieFramesSerializer {

    private static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls().serializeSpecialFloatingPointValues();
        gsonBuilder.registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DJsonSerializer());
        return gsonBuilder.create();
    }

    public static boolean save(List<MovieFrameInst> list, File file){
        try (Writer writer = new FileWriter(file)) {
            getGson().toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static List<MovieFrameInst> getFrom(File file) throws FileNotFoundException {
        return getGson().fromJson(new FileReader(file.getAbsolutePath()),new TypeToken<List<MovieFrameInst>>(){}.getType());
    }
}
