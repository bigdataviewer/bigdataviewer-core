package bdv.export.n5.meta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.GsonUtils;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.LockedChannel;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.cache.N5JsonCache;

import static bdv.export.n5.meta.AttributesCache.CHILDREN;
import static bdv.export.n5.meta.AttributesCache.IS_DATASET;
import static bdv.export.n5.meta.AttributesCache.IS_GROUP;

public class AttributesCaching {

	public static final String ATTRIBUTES_CACHE = "attributes_cache.json";

	public static void storeCachedAttributes(final CachedGsonKeyValueN5Reader n5) throws IOException {
		final URI uri = n5.getURI();
		final Gson gson = new GsonBuilder().create();
		final KeyValueAccess kva = n5.getKeyValueAccess();
		final JsonObject root = AttributesCache.collect(n5, "");
		try (final LockedChannel channel = kva.lockForWriting(kva.compose(uri, ATTRIBUTES_CACHE))) {
			GsonUtils.writeAttributes(channel.newWriter(), root, gson);
		}
	}

	public static void injectCachedAttributes(final CachedGsonKeyValueN5Reader n5) {
		try {
			final URI uri = n5.getURI();
			final Gson gson = new GsonBuilder().create();
			final KeyValueAccess kva = n5.getKeyValueAccess();
			final JsonObject root;
			try (final LockedChannel channel = kva.lockForReading(kva.compose(uri, ATTRIBUTES_CACHE))) {
				root = gson.fromJson(channel.newReader(), JsonObject.class);
			}
			populate(n5.getCache(), root, "");
		} catch (N5Exception | IOException ignored) {
		}
	}

	private static final Collection<String> reservedNames = Arrays.asList(CHILDREN, IS_DATASET, IS_GROUP);

	private static void populate(final N5JsonCache cache, final JsonObject group, final String prefix) {
		final JsonElement childrenElement = group.get(CHILDREN);
		final JsonElement isDatasetElement = group.get(IS_DATASET);
		final JsonElement isGroupElement = group.get(IS_GROUP);
		group.asMap().forEach((name, child) -> {
			if (!reservedNames.contains(name)) {
				final String normalPathKey = prefix;
				final String normalCacheKey = name;
				cache.initializeNonemptyCache(normalPathKey, normalCacheKey);
				cache.setAttributes(normalPathKey, normalCacheKey, child);
				final String[] children = childrenElement.getAsJsonObject().keySet().toArray(new String[0]);
				try {
					final Object info = N5JsonCacheReflect.getCacheInfo(cache, normalPathKey);
					N5JsonCacheReflect.setChildren(info, children);
					N5JsonCacheReflect.setIsDataset(info, isDatasetElement.getAsBoolean());
					N5JsonCacheReflect.setIsGroup(info, isGroupElement.getAsBoolean());
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		});
		if (childrenElement != null) {
			childrenElement.getAsJsonObject().asMap().forEach((name, child) -> {
				populate(cache,
						child.getAsJsonObject(),
						prefix.isEmpty() ? name : (prefix + "/" + name));
			});
		}
	}







}
