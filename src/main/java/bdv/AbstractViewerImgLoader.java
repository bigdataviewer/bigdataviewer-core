package bdv;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

public abstract class AbstractViewerImgLoader< T, V extends Volatile< T > > implements ViewerImgLoader< T, V >
{
	protected final T type;

	protected final V volatileType;

	public AbstractViewerImgLoader( final T type, final V volatileType )
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
	public RandomAccessibleInterval< T > getImage( final ViewId view )
	{
		return getImage( view, 0 );
	}
}
