package creator.spim.imgloader;

import ij.ImagePlus;

import java.io.File;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.display.RealFloatConverter;
import net.imglib2.display.RealUnsignedShortConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.io.ImgIOException;
import net.imglib2.io.ImgOpener;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FileSequenceImageLoader implements ImgLoader
{
	String path;

	String pattern;

	int numSlices;

	double min;

	double max;

	public FileSequenceImageLoader()
	{
		path = null;
		pattern = null;
		numSlices = 0;
		min = 0;
		max = 0;
	}

	public FileSequenceImageLoader( final String path, final String pattern, final int numSlices, final double min, final double max )
	{
		this.path = path;
		this.pattern = pattern;
		this.numSlices = numSlices;
		this.min = min;
		this.max = max;
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Element toXml( final Document doc, final File basePath )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		// TODO Auto-generated method stub
		return null;
	}

	static interface SliceLoader
	{
		public RandomAccessibleInterval< FloatType > load( String fn );
	}

	static class FloatArrayImgLoader implements SliceLoader
	{
		final ImgOpener o = new ImgOpener();
		final ArrayImgFactory< FloatType > fFactory = new ArrayImgFactory< FloatType >();
		final FloatType fType = new FloatType();

		@Override
		public Img< FloatType > load( final String fn )
		{
			Img< FloatType > slice = null;
			try
			{
				slice = o.openImg( fn, fFactory, fType );
			}
			catch ( final ImgIOException e )
			{
				e.printStackTrace();
			}
			return slice;
		}
	}

	static class FloatImagePlusLoader implements SliceLoader
	{
		@Override
		public Img< FloatType > load( final String fn )
		{
			return ImageJFunctions.wrapFloat( new ImagePlus( fn ) );
		}
	}

	static class ByteImagePlusLoader implements SliceLoader
	{
		@Override
		public RandomAccessibleInterval< FloatType > load( final String fn )
		{
			final RandomAccessibleInterval< UnsignedByteType > img = ImageJFunctions.wrapByte( new ImagePlus( fn ) );
			return Converters.convert( img, new RealFloatConverter< UnsignedByteType >(), new FloatType() );
		}
	}

	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		final SliceLoader slices = new ByteImagePlusLoader();

		int z = 0;
		String fn = path + "/" + String.format( pattern, z + 41 );
		RandomAccessibleInterval< FloatType > slice = slices.load( fn );

		final long[] dimensions = new long[ 3 ];
		dimensions[ 0 ] = slice.dimension( 0 );
		dimensions[ 1 ] = slice.dimension( 1 );
		dimensions[ 2 ] = numSlices;

		final ArrayImgFactory< UnsignedShortType > sFactory = new ArrayImgFactory< UnsignedShortType >();
		final UnsignedShortType sType = new UnsignedShortType();
		final RealUnsignedShortConverter< FloatType > converter = new RealUnsignedShortConverter< FloatType >( min, max );
		final Img< UnsignedShortType > img = sFactory.create( dimensions, sType );

		for ( z = 0; z < numSlices; ++z )
		{
			System.out.print( z + " " );
			if ( ( z + 1 ) % 20 == 0 )
				System.out.println();
			fn = path + "/" + String.format( pattern, z + 41 );
			slice = slices.load( fn );

			final Cursor< UnsignedShortType > d = Views.flatIterable( Views.hyperSlice( img, 2, z ) ).cursor();
			for ( final UnsignedShortType t : Converters.convert( Views.flatIterable( slice ), converter, new UnsignedShortType() ) )
				d.next().set( t );
		}

		return new ImgPlus< UnsignedShortType >( img );
	}

}
