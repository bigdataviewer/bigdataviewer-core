package bdv.ij.export.imgloader;

import ij.ImagePlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.List;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.ViewDescription;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;


/**
 * This {@link ImgLoader} loads images that represent a 3D stack in a single
 * file, for example in tif, lsm, or czi format. It is constructed with a list
 * of image filenames and the number of setups (e.g. angles). Then, to laod the
 * image for a given {@link ViewDescription}, its index in the filename list is computed as
 * <code>view.getSetupIndex() + numViewSetups * view.getTimepointIndex()</code>.
 *
 * This {@link ImgLoader} is used for exporting spim sequences to hdf5. Only the
 * {@link #getImage(ViewDescription)} method is implemented because this is
 * the only method required for exporting to hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class StackImageLoader implements ImgLoader< UnsignedShortType >
{
	private final ImgOpener opener;

	private final ArrayImgFactory< UnsignedShortType > factory;

	private final UnsignedShortType type;

	private final List< String > filenames;

	private final int numViewSetups;

	private boolean useImageJOpener;

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
	public ImgPlus< FloatType > getFloatImage( final ViewDescription view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public ImgPlus< UnsignedShortType > getImage( final ViewDescription view )
	{
		final int setup = view.getSetupIndex();
		final int timepoint = view.getTimepointIndex();
		final int index = timepoint * numViewSetups + setup;
		final String fn = filenames.get( index );
		if ( useImageJOpener )
		{
			final ImagePlus imp = new ImagePlus( fn );
			if ( imp.getType() == ImagePlus.GRAY16 )
				return new ImgPlus< UnsignedShortType >( ImageJFunctions.wrapShort( imp ) );
			else if ( imp.getType() == ImagePlus.GRAY8 )
			{
				System.out.println( "wrapping" );
				return new ImgPlus< UnsignedShortType >(
					new ImgView< UnsignedShortType >(
							Converters.convert(
									( RandomAccessibleInterval<UnsignedByteType> ) ImageJFunctions.wrapByte( imp ),
									new Converter< UnsignedByteType, UnsignedShortType >() {
										@Override
										public void convert( final UnsignedByteType input, final UnsignedShortType output )
										{
											output.set( input.get() );
										}
									},
									new UnsignedShortType()
							), null ) );
			}
			else
				useImageJOpener = false;
		}

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
