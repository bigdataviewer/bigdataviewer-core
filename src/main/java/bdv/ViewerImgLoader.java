package bdv;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;

public interface ViewerImgLoader< T, V extends Volatile< T > > extends ImgLoader< T >
{
	public RandomAccessibleInterval< T > getImage( final View view, final int level );

	public T getImageType();

	public RandomAccessibleInterval< V > getVolatileImage( final View view, final int level );

	public V getVolatileImageType();

	public double[][] getMipmapResolutions( final int setup );

	public int numMipmapLevels( final int setup );
}
