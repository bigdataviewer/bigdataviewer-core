package mpicbg.tracking.steffi;

import static mpicbg.tracking.data.io.XmlHelpers.loadPath;

import java.io.File;

import mpicbg.tracking.data.View;
import mpicbg.tracking.data.ViewSetup;
import mpicbg.tracking.data.io.ImgLoader;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Element;

public class TheImgLoader implements ImgLoader
{
	private String path;

	@Override
	public void init( final Element elem, final File basePath )
	{
		try
		{
			path = loadPath( elem, "path", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final ViewSetup setup = view.getSetup();
		final int channel = setup.getChannel();
		final int illumination = setup.getIllumination();
		final int angle = setup.getAngle();
		final int timepoint = view.getTimepoint();

		final String name = path + "/" + getBasename( timepoint, angle, channel, illumination ) + ".tif";
		final ImgPlus< UnsignedShortType > img;
		try
		{
			img = new ImgOpener().openImg( name, new ArrayImgFactory< UnsignedShortType >(), new UnsignedShortType() );
		}
		catch ( final ImgIOException e )
		{
			throw new RuntimeException( e );
		}

		return img;
	}

	final static private String basenameFormatString = "t%05d-a%03d-c%03d-i%01d";

	private static String getBasename( final int timepoint, final int angle, final int channel, final int illumination )
	{
		return String.format( basenameFormatString, timepoint, angle, channel, illumination );
	}
}
