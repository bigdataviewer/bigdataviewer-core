package bdv.cache;

import bdv.img.cache.VolatileGlobalCellCache;

/**
 * Allows to override the {@link bdv.img.cache.VolatileGlobalCellCache} in
 * ImageLoader(s) which implement this interface.
 */

public interface CacheOverrider {

    void setCache(VolatileGlobalCellCache cache);

}
