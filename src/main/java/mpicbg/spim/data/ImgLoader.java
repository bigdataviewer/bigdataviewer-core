package mpicbg.spim.data;

import java.io.File;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;

public interface ImgLoader
{
	/**
	 * initialize the loader from a "ImageLoader" DOM element.
	 */
	public void init( final Element elem, final File basePath );

	/**
	 * create a "ImageLoader" DOM element for this loader.
	 */
	public Element toXml( final File basePath );

	/**
	 * Get {@link FloatType} image normalized to range [0,1].
	 *
	 * @param view
	 *            timepoint and setup for which to retrieve the image.
	 * @return {@link FloatType} image normalized to range [0,1]
	 */
	public RandomAccessibleInterval< FloatType > getImage( View view );

	/**
	 * Get {@link UnsignedShortType} un-normalized image.
	 *
	 * @param view
	 *            timepoint and setup for which to retrieve the image.
	 * @return {@link UnsignedShortType} image.
	 */
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( View view );
}
