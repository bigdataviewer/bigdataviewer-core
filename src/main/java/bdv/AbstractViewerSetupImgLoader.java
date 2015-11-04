package bdv;

import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

public abstract class AbstractViewerSetupImgLoader< T, V extends Volatile< T > > implements ViewerSetupImgLoader< T, V >
{
	protected final T type;

	protected final V volatileType;

	public AbstractViewerSetupImgLoader( final T type, final V volatileType )
	{
		this.type = type;
		this.volatileType = volatileType;
	}

	@Override
	public T getImageType()
	{
		return type;
	}

	@Override
	public V getVolatileImageType()
	{
		return volatileType;
	}

	@Override
	public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
	{
		return getImage( timepointId, 0, hints );
	}
}
