package bdv;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import bdv.img.cache.Cache;

public interface ViewerImgLoader extends BasicImgLoader
{
	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId );

	public Cache getCache();
}
