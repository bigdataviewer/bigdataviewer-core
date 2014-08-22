package bdv.ij.export.imgloader;

import ij.ImagePlus;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;


/**
 * This {@link ImgLoader} loads images that represent a 3D stack in a single
 * file, for example in tif, lsm, or czi format. It is constructed with a list
 * of image filenames and the number of setups (e.g. angles). Then, to laod the
 * image for a given {@link ViewDescription}, its index in the filename list is computed as
 * <code>view.getSetupIndex() + numViewSetups * view.getTimepointIndex()</code>.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class StackImageLoader implements BasicImgLoader< UnsignedShortType >
{
	private final ImgOpener opener;

	private final ArrayImgFactory< UnsignedShortType > factory;

	private final UnsignedShortType type;

	final HashMap< ViewId, String > filenames;

	private boolean useImageJOpener;

	public StackImageLoader( final HashMap< ViewId, String > filenames, final boolean useImageJOpener )
	{
		this.filenames = filenames;
		this.useImageJOpener = useImageJOpener;
		opener = useImageJOpener ? null : new ImgOpener( new Context( SCIFIOService.class, AppService.class, StatusService.class ) );
		factory = new ArrayImgFactory< UnsignedShortType >();
		type = new UnsignedShortType();
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
	{
		final String fn = filenames.get( view );
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

	@Override
	public UnsignedShortType getImageType()
	{
		return type;
	}
}
