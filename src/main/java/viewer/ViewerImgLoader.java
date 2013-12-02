package viewer;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.natives.VolatileUnsignedShortType;

public interface ViewerImgLoader extends ImgLoader
{
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view, final int level );

	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileUnsignedShortImage( final View view, final int level );

	public double[][] getMipmapResolutions( final int setup );

	public int numMipmapLevels( final int setup );
}
