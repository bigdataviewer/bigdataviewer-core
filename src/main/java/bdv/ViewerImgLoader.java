package bdv;

import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;
import bdv.img.cache.Cache;

public interface ViewerImgLoader extends BasicMultiResolutionImgLoader
{
	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId );

	public Cache getCache();
}
