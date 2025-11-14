package bdv.export.n5.meta;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.janelia.saalfeldlab.n5.CachedGsonKeyValueN5Reader;
import org.janelia.saalfeldlab.n5.KeyValueAccess;
import org.janelia.saalfeldlab.n5.N5KeyValueReader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.cache.N5JsonCache;

class AttributesCache {

	public static JsonObject collect(
			final CachedGsonKeyValueN5Reader n5,
			final String path) {

		final AttributesCache cache = new AttributesCache(n5.getKeyValueAccess(), path);
		collectN5(n5, path, cache);
		return cache.root;
	}

	private static void collectN5(
			final CachedGsonKeyValueN5Reader n5,
			final String path,
			final AttributesCache result) {

		final N5JsonCache n5JsonCache = n5.getCache();

		final String normalPath = N5URI.normalizeGroupPath(path);
		final String[] children = n5JsonCache.list(normalPath);
		Arrays.sort(children);
		final JsonElement attr = n5JsonCache.getAttributes(normalPath,
				N5KeyValueReader.ATTRIBUTES_JSON); // TODO: for zarr, we have to harvest .zarray, .zattr, .zgroup instead
		if (attr != null) {
			final boolean isDataset = n5JsonCache.isDataset(normalPath, N5KeyValueReader.ATTRIBUTES_JSON);
			final boolean isGroup = n5JsonCache.isGroup(normalPath, N5KeyValueReader.ATTRIBUTES_JSON);
			result.put(path, children, isDataset, isGroup, Collections.singletonMap(N5KeyValueReader.ATTRIBUTES_JSON, attr));
		}
		for (final String child : children) {
			collectN5(n5, path.isEmpty() ? child : (path + "/" + child), result);
		}
	}

	private final KeyValueAccess kva;
	private final String basePath;

	public static final String CHILDREN = "children";
	public static final String IS_DATASET = "is_dataset";
	public static final String IS_GROUP = "is_group";

	final JsonObject root = new JsonObject();

	AttributesCache(final KeyValueAccess kva, final String basePath) {
		this.kva = kva;
		this.basePath = basePath;
	}

	void put(
			final String path,
			final String[] children,
			final boolean isDataset,
			final boolean isGroup,
			final Map<String, JsonElement> attrs) {
		final JsonObject group = getGroup(path);
		addChildren(group, children);
		attrs.forEach(group::add);
		group.addProperty(IS_DATASET, isDataset);
		group.addProperty(IS_GROUP, isGroup);
	}

	private void addChildren(final JsonObject group, final String[] children) {
		JsonObject childrenObject = group.getAsJsonObject(CHILDREN);
		if (childrenObject == null) {
			childrenObject = new JsonObject();
			group.add(CHILDREN, childrenObject);
		}
		for (String child : children) {
			childrenObject.add(child, new JsonObject());
		}
	}

	private JsonObject getGroup(final String path) {
		final String relativePath = kva.relativize(kva.normalize(path), kva.normalize(basePath));
		if (relativePath.isEmpty()) {
			return root;
		} else {
			final String[] components = kva.components(relativePath);
			JsonObject current = root;
			for (final String component : components) {
				JsonObject children = current.getAsJsonObject(CHILDREN);
				if (children == null) {
					children = new JsonObject();
					current.add(CHILDREN, children);
				}
				current = children.getAsJsonObject(component);
				if (current == null) {
					current = new JsonObject();
					children.add(component, current);
				}
			}
			return current;
		}
	}

	public void put(final String[] components, final JsonElement jsonElement) {
		JsonObject current = root;
		for (int i = 0; i < components.length - 1; ++i) {
			JsonObject children = current.getAsJsonObject(CHILDREN);
			if (children == null) {
				children = new JsonObject();
				current.add(CHILDREN, children);
			}
			current = children.getAsJsonObject(components[i]);
			if (current == null) {
				current = new JsonObject();
				children.add(components[i], current);
			}
		}
		current.add(components[components.length - 1], jsonElement);
	}
}
