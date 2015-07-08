package bdv.img.hdf5;

import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;

public interface MultiResolutionSetupImgLoader< T > extends BasicSetupImgLoader< T >
{
	public RandomAccessibleInterval< T > getImage( final int timepointId, final int level );

	public double[][] getMipmapResolutions();

	public AffineTransform3D[] getMipmapTransforms();

	public int numMipmapLevels();
}
