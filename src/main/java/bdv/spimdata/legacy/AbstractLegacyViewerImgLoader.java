package bdv.spimdata.legacy;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

//@Deprecated
public abstract class AbstractLegacyViewerImgLoader< T, V extends Volatile< T > > implements LegacyViewerImgLoader< T, V >
{
	protected final T type;

	protected final V volatileType;

	public AbstractLegacyViewerImgLoader( final T type, final V volatileType )
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
