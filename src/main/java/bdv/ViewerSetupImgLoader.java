package bdv;

import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;

public interface ViewerSetupImgLoader< T, V extends Volatile< T > > extends BasicSetupImgLoader< T >
{
	public RandomAccessibleInterval< T > getImage( final int timepointId, final int level );

	public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level );

	public V getVolatileImageType();

	public double[][] getMipmapResolutions();

	public AffineTransform3D[] getMipmapTransforms();

	public int numMipmapLevels();
}
