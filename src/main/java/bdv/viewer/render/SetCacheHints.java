package bdv.viewer.render;

import bdv.img.cache.CacheHints;

public interface SetCacheHints
{
	void setCacheHints( int level, CacheHints cacheHints );

	public static SetCacheHints empty = new SetCacheHints()
	{
		@Override
		public void setCacheHints( final int level, final CacheHints cacheHints )
		{}
	};
}
