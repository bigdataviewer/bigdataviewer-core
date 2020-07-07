package bdv.viewer.render;

import net.imglib2.RandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;

public class ProjectorUtils
{
	/**
	 * Extracts the underlying {@code int[]} array in case {@code img} is a
	 * standard {@code ArrayImg<ARGBType>}. This supports certain (optional)
	 * optimizations in projector implementations.
	 *
	 * @return the underlying {@code int[]} array of {@code img}, if it is a
	 * standard {@code ArrayImg<ARGBType>}. Otherwise {@code null}.
	 */
	public static int[] getARGBArrayImgData( final RandomAccessible< ? > img )
	{
		if ( ! ( img instanceof ArrayImg ) )
			return null;
		final ArrayImg< ?, ? > aimg = ( ArrayImg< ?, ? > ) img;
		if( ! ( aimg.firstElement() instanceof ARGBType ) )
			return null;
		final Object access = aimg.update( null );
		if ( ! ( access instanceof IntArray ) )
			return null;
		return ( ( IntArray ) access ).getCurrentStorageArray();
	}
}
