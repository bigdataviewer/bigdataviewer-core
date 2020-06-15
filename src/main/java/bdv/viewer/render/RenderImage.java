package bdv.viewer.render;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Fraction;

/**
 * An ARGBType ArrayImg that knows it's {@code int[]} data array.
 */
class RenderImage extends ArrayImg< ARGBType, IntArray >
{
	final private int[] data;

	/**
	 * Create an image with {@code data}. Writing to the {@code data}
	 * array will update the image.
	 */
	public RenderImage( final int width, final int height, final int[] data )
	{
		super( new IntArray( data ), new long[] { width, height }, new Fraction() );
		setLinkedType( new ARGBType( this ) );
		this.data = data;
	}

	/**
	 * The underlying array holding the data.
	 */
	public int[] getData()
	{
		return data;
	}
}
