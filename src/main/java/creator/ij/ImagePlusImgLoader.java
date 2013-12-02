package creator.ij;

import ij.ImagePlus;

import java.io.File;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedShortConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.jdom2.Element;

/**
 * This {@link ImgLoader} implementation returns a wrapped, converted
 * {@link ImagePlus}. It is used for exporting {@link ImagePlus} to hdf5. Only
 * the {@link #getUnsignedShortImage(View)} method is implemented because this
 * is the only method required for exporting to hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ImagePlusImgLoader< T extends RealType< T > > implements ImgLoader
{
	public static enum MinMaxOption
	{
		SET,
		COMPUTE,
		TAKE_FROM_IMAGEPROCESSOR
	}

	public static ImagePlusImgLoader< UnsignedByteType > createGray8( final ImagePlus imp, final MinMaxOption minMaxOption, final double min, final double max )
	{
		if( imp.getType() != ImagePlus.GRAY8 )
			throw new RuntimeException( "expected ImagePlus type GRAY8" );
		return new ImagePlusImgLoader< UnsignedByteType >( imp, ImageJFunctions.wrapByte( imp ), new UnsignedByteType(), minMaxOption, min, max );
	}

	public static ImagePlusImgLoader< UnsignedShortType > createGray16( final ImagePlus imp, final MinMaxOption minMaxOption, final double min, final double max )
	{
		if( imp.getType() != ImagePlus.GRAY16 )
			throw new RuntimeException( "expected ImagePlus type GRAY16" );
		return new ImagePlusImgLoader< UnsignedShortType >( imp, ImageJFunctions.wrapShort( imp ), new UnsignedShortType(), minMaxOption, min, max );
	}

	public static ImagePlusImgLoader< FloatType > createGray32( final ImagePlus imp, final MinMaxOption minMaxOption, final double min, final double max )
	{
		if( imp.getType() != ImagePlus.GRAY32 )
			throw new RuntimeException( "expected ImagePlus type GRAY32" );
		return new ImagePlusImgLoader< FloatType >( imp, ImageJFunctions.wrapFloat( imp ), new FloatType(), minMaxOption, min, max );
	}

	protected final ImagePlus imp;

	protected final RandomAccessibleInterval< T > wrappedImp;

	protected final double impMin;

	protected final double impMax;

	protected final boolean isMultiChannel;

	protected final int channelDim;

	protected final boolean isMultiFrame;

	protected final int frameDim;

	protected ImagePlusImgLoader( final ImagePlus imp, final RandomAccessibleInterval< T > wrappedImp, final T type, final MinMaxOption minMaxOption, final double min, final double max )
	{
		this.imp = imp;
		this.wrappedImp = wrappedImp;

		if ( minMaxOption == MinMaxOption.COMPUTE )
		{
			final T minT = type.createVariable();
			final T maxT = type.createVariable();
			ComputeMinMax.computeMinMax( wrappedImp, minT, maxT );
			impMin = minT.getRealDouble();
			impMax = maxT.getRealDouble();
			System.out.println( "COMPUTE" );
			System.out.println( impMin + "  " + impMax );
		}
		else if ( minMaxOption == MinMaxOption.TAKE_FROM_IMAGEPROCESSOR )
		{
			impMin = imp.getDisplayRangeMin();
			impMax = imp.getDisplayRangeMax();
			System.out.println( "TAKE_FROM_IMAGEPROCESSOR" );
			System.out.println( impMin + "  " + impMax );
		}
		else
		{
			impMin = min;
			impMax = max;
			System.out.println( "SET" );
			System.out.println( impMin + "  " + impMax );
		}

		if ( imp.getNDimensions() <= 3 )
		{
			isMultiChannel = false;
			channelDim = 0;
			isMultiFrame = false;
			frameDim = 0;
		}
		else
		{
			isMultiChannel = imp.getNChannels() > 1;
			channelDim = isMultiChannel ? 2 : 0;
			isMultiFrame = imp.getNFrames() > 1;
			frameDim = isMultiFrame ? ( isMultiChannel ? 4 : 3 ) : 0;
		}
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
		RandomAccessibleInterval< T > img = wrappedImp;
		if ( isMultiFrame )
		{
			final int frame = view.getTimepointIndex();
			img = Views.hyperSlice( img, frameDim, frame );
		}
		if ( isMultiChannel )
		{
			final int channel = view.getSetupIndex();
			img = Views.hyperSlice( img, channelDim, channel );
		}
		return Converters.convert( img, new RealUnsignedShortConverter< T >( impMin, impMax ), new UnsignedShortType() );
	}
}
