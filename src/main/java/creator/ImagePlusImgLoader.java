package creator;

import ij.ImagePlus;

import java.io.File;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.converter.Converters;
import net.imglib2.display.RealUnsignedShortConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This {@link ImgLoader} implementations returns a wrapped, converted
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
		}
		else if ( minMaxOption == MinMaxOption.TAKE_FROM_IMAGEPROCESSOR )
		{
			this.impMin = imp.getDisplayRangeMin();
			this.impMax = imp.getDisplayRangeMax();
		}
		else
		{
			this.impMin = min;
			this.impMax = max;
		}
	}

	/**
	 * not implemented.
	 */
	@Override
	public void init( final Element elem, final File basePath )
	{
	}

	/**
	 * not implemented.
	 */
	@Override
	public Element toXml( final Document doc, final File basePath )
	{
		return null;
	}

	/**
	 * not implemented.
	 */
	@Override
	public RandomAccessibleInterval< FloatType > getImage( final View view )
	{
		return null;
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		return Converters.convert( wrappedImp, new RealUnsignedShortConverter< T >( impMin, impMax ), new UnsignedShortType() );
	}
}
