package bdv;

import java.io.File;

import mpicbg.spim.data.ViewDescription;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;

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
	public Element toXml( final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< FloatType > getFloatImage( final ViewDescription view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< T > getImage( final ViewDescription view )
	{
		return getImage( view, 0 );
	}
}
