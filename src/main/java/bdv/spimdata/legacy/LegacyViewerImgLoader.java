package bdv.spimdata.legacy;

import mpicbg.spim.data.legacy.LegacyBasicImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.cache.Cache;

//@Deprecated
public interface LegacyViewerImgLoader< T, V extends Volatile< T > > extends LegacyBasicImgLoader< T >
{
	public RandomAccessibleInterval< T > getImage( final ViewId view, final int level );

	public RandomAccessibleInterval< V > getVolatileImage( final ViewId view, final int level );

	public V getVolatileImageType();

	public double[][] getMipmapResolutions( final int setupId );

	public AffineTransform3D[] getMipmapTransforms( final int setupId );

	public int numMipmapLevels( final int setupId );

	public Cache getCache();
}
