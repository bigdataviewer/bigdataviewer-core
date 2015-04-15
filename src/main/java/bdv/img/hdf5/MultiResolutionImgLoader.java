package bdv.img.hdf5;

import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;

public interface MultiResolutionImgLoader< T > extends ImgLoader< T >
{
	public RandomAccessibleInterval< T > getImage( final ViewId view, final int level );

	public RandomAccessibleInterval< FloatType > getFloatImage( ViewId view, final int level, boolean normalize );

	public Dimensions getImageSize( ViewId view, final int level );

	public double[][] getMipmapResolutions( final int setupId );

	public AffineTransform3D[] getMipmapTransforms( final int setupId );

	public int numMipmapLevels( final int setupId );
}
