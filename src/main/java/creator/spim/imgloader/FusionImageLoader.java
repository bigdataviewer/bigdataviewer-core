package creator.spim.imgloader;

import ij.ImagePlus;

import java.io.File;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.display.RealUnsignedShortConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.jdom2.Element;

/**
 * This {@link ImgLoader} loads images that represent a 3D stack as a sequence
 * of slice with one image file per slice, such as created by Stephan
 * Preibisch's Multi-view fusion plugin. It is constructed with the pattern of
 * the image filenames. Then, to laod the image for a given {@link View}, its
 * TODO timepoint? index?, channel, and slice indices are filled into the
 * template to get the slice filenames.
 *
 * This {@link ImgLoader} is used for exporting spim sequences to hdf5. Only the
 * {@link #getUnsignedShortImage(View)} method is implemented because this is
 * the only method required for exporting to hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class FusionImageLoader< T extends RealType< T > > implements ImgLoader
{
	private final String pattern;

	private final int numSlices;

	private final SliceLoader< T > sliceLoader;

	private final RealUnsignedShortConverter< T > converter;

	private final ImgFactory< UnsignedShortType > factory;

	private final UnsignedShortType type;

	public FusionImageLoader( final String pattern, final int numSlices, final SliceLoader< T > sliceLoader, final double sliceValueMin, final double sliceValueMax )
	{
		this( pattern, numSlices, sliceLoader, sliceValueMin, sliceValueMax, new PlanarImgFactory< UnsignedShortType >() );
	}

	public FusionImageLoader( final String pattern, final int numSlices, final SliceLoader< T > sliceLoader, final double sliceValueMin, final double sliceValueMax, final ImgFactory< UnsignedShortType > factory )
	{
		this.pattern = pattern;
		this.numSlices = numSlices;
		this.sliceLoader = sliceLoader;
		converter = new RealUnsignedShortConverter< T >( sliceValueMin, sliceValueMax );
		this.factory = factory;
		type = new UnsignedShortType();
	}

	public static interface SliceLoader< T >
	{
		public RandomAccessibleInterval< T > load( String fn );
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
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final int tp = view.getTimepoint();
		final int c = view.getSetup().getChannel();

		RandomAccessibleInterval< T > slice = sliceLoader.load( String.format( pattern, tp, c, 0 ) );
		final long[] dimensions = new long[ 3 ];
		dimensions[ 0 ] = slice.dimension( 0 );
		dimensions[ 1 ] = slice.dimension( 1 );
		dimensions[ 2 ] = numSlices;
		final Img< UnsignedShortType > img = factory.create( dimensions, type );

		for ( int z = 0; z < numSlices; ++z )
		{
			slice = sliceLoader.load( String.format( pattern, tp, c, z ) );

			final Cursor< UnsignedShortType > d = Views.flatIterable( Views.hyperSlice( img, 2, z ) ).cursor();
			for ( final UnsignedShortType t : Converters.convert( Views.flatIterable( slice ), converter, type ) )
				d.next().set( t );
		}
		return img;
	}

	public static class ArrayImgLoader< T extends RealType< T > & NativeType< T > > implements SliceLoader< T >
	{
		final ImgOpener opener;

		final ArrayImgFactory< T > factory;

		final T type;

		public ArrayImgLoader( final T type )
		{
			opener = new ImgOpener();
			factory = new ArrayImgFactory< T >();
			this.type = type;
		}

		@Override
		public RandomAccessibleInterval< T > load( final String fn )
		{
			try
			{
				System.out.println( fn );
				return opener.openImg( fn, factory, type );
			}
			catch ( final ImgIOException e )
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public static class Gray32ImagePlusLoader implements SliceLoader< FloatType >
	{
		@Override
		public RandomAccessibleInterval< FloatType > load( final String fn )
		{
			return ImageJFunctions.wrapFloat( new ImagePlus( fn ) );
		}
	}

	public static class Gray16ImagePlusLoader implements SliceLoader< UnsignedShortType >
	{
		@Override
		public RandomAccessibleInterval< UnsignedShortType > load( final String fn )
		{
			return ImageJFunctions.wrapShort( new ImagePlus( fn ) );
		}
	}

	public static class Gray8ImagePlusLoader implements SliceLoader< UnsignedByteType >
	{
		@Override
		public RandomAccessibleInterval< UnsignedByteType > load( final String fn )
		{
			return ImageJFunctions.wrapByte( new ImagePlus( fn ) );
		}
	}
}
