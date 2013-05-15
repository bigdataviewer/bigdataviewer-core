package viewer;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public interface ViewerImgLoader extends ImgLoader
{
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view, final int level );

	public double[][] getMipmapResolutions( final int setup );

	public int numMipmapLevels( final int setup );
}
