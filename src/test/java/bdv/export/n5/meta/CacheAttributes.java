package bdv.export.n5.meta;

import java.io.IOException;
import mpicbg.spim.data.SpimDataException;
import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.N5FSReader;

public class CacheAttributes {
	public static void main(String[] args) throws IOException, SpimDataException {
		final String basePath = args[0];
		final CachedGsonKeyValueN5Reader n5 = new N5FSReader(basePath, true);
		AttributesCaching.storeCachedAttributes(n5);
	}
}
