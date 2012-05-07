package mpicbg.tracking.data;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Element;

public interface ImgLoader
{
	/**
	 * initialize the loader from a "ImageLoader" DOM element.
	 */
	public void init( final Element elem );

	public ImgPlus< FloatType > getImage( View view );
}
