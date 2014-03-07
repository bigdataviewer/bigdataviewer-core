package bdv.viewer.render;

import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;

public interface SetLoadingStrategy
{
	public void setLoadingStrategy( int level, LoadingStrategy strategy );

	public static SetLoadingStrategy empty = new SetLoadingStrategy()
	{
		@Override
		public void setLoadingStrategy( final int level, final LoadingStrategy strategy )
		{}
	};
}
