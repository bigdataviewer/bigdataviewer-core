package bdv.img.hdf5;

import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

public interface MultiResolutionImgLoader extends ImgLoader
{
	@Override
	public MultiResolutionSetupImgLoader< ? > getSetupImgLoader( final int setupId );

	public RandomAccessibleInterval< FloatType > getFloatImage( ViewId view, final int level, boolean normalize );

	public Dimensions getImageSize( ViewId view, final int level );
}
