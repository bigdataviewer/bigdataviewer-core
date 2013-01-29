package creator.spim.imgloader;

import java.io.File;
import java.util.List;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StackImageLoader implements ImgLoader
{
	private final ImgOpener opener;

	private final ArrayImgFactory< UnsignedShortType > factory;

	private final UnsignedShortType type;

	private final List< String > filenames;

	private final int numViewSetups;

	public StackImageLoader( final List< String > filenames, final int numViewSetups )
	{
		this.filenames = filenames;
		this.numViewSetups = numViewSetups;
		opener = new ImgOpener();
		factory = new ArrayImgFactory< UnsignedShortType >();
		type = new UnsignedShortType();
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public Element toXml( final Document doc, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final int setup = view.getSetupIndex();
		final int timepoint = view.getTimepointIndex();
		final int index = timepoint * numViewSetups + setup;
		final String fn = filenames.get( index );
		ImgPlus< UnsignedShortType > img;
		try
		{
			img = opener.openImg( fn, factory, type );
		}
		catch ( final ImgIOException e )
		{
			throw new RuntimeException( e );
		}
		return img;
	}
}
