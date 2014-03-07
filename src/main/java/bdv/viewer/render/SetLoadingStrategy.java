package bdv.viewer.render;

import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;

public interface SetLoadingStrategy
{
	public void setLoadingStrategy( int level, LoadingStrategy strategy );
}
