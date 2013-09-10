package creator.spim.imgloader;

import ij.ImagePlus;

import java.io.File;
import java.util.List;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;


/**
 * This {@link ImgLoader} loads images that represent a 3D stack in a single
 * file, for example in tif, lsm, or czi format. It is constructed with a list
 * of image filenames and the number of setups (e.g. angles). Then, to laod the
 * image for a given {@link View}, its index in the filename list is computed as
 * <code>view.getSetupIndex() + numViewSetups * view.getTimepointIndex()</code>.
 *
 * This {@link ImgLoader} is used for exporting spim sequences to hdf5. Only the
 * {@link #getUnsignedShortImage(View)} method is implemented because this is
 * the only method required for exporting to hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class StackImageLoader implements ImgLoader
{
	private final ImgOpener opener;

	private final ArrayImgFactory< UnsignedShortType > factory;

	private final UnsignedShortType type;

	private final List< String > filenames;

	private final int numViewSetups;

	private final boolean useImageJOpener;

	public StackImageLoader( final List< String > filenames, final int numViewSetups, final boolean useImageJOpener )
	{
		this.filenames = filenames;
		this.numViewSetups = numViewSetups;
		this.useImageJOpener = useImageJOpener;
		opener = new ImgOpener();
		factory = new ArrayImgFactory< UnsignedShortType >();
		type = new UnsignedShortType();
	}

	/**
	 * not implemented.
	 */
	@Override
	public void init( final Element elem, final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	/**
	 * not implemented.
	 */
	@Override
	public Element toXml( final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	/**
	 * not implemented.
	 */
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
		if ( useImageJOpener )
		{
			return new ImgPlus< UnsignedShortType >( ImageJFunctions.wrapShort( new ImagePlus( fn ) ) );
		}
		else
		{
			try
			{
				return opener.openImg( fn, factory, type );
			}
			catch ( final ImgIOException e )
			{
				throw new RuntimeException( e );
			}
		}
	}
}
