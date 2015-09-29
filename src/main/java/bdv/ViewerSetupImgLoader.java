package bdv;

import mpicbg.spim.data.generic.sequence.BasicMultiResolutionSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

public interface ViewerSetupImgLoader< T, V extends Volatile< T > > extends BasicMultiResolutionSetupImgLoader< T >
{
	public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, ImgLoaderHint... hints );

	public V getVolatileImageType();
}