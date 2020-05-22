package bdv.viewer.render;

import net.imglib2.RandomAccessible;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;

public class ProjectorUtils
{
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
