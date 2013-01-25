package mpicbg.spim.data;

import java.io.File;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Element;

public interface ImgLoader
{
	/**
	 * initialize the loader from a "ImageLoader" DOM element.
	 */
	public void init( final Element elem, final File basePath );

	/**
	 * Get {@link FloatType} image normalized to range [0,1].
	 *
	 * @param view
	 *            timepoint and setup for which to retrieve the image.
	 * @return {@link FloatType} image normalized to range [0,1]
	 */
	public ImgPlus< FloatType > getImage( View view );

	/**
	 * Get {@link UnsignedShortType} un-normalized image.
	 *
	 * @param view
	 *            timepoint and setup for which to retrieve the image.
	 * @return {@link UnsignedShortType} image.
	 */
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( View view );
}
