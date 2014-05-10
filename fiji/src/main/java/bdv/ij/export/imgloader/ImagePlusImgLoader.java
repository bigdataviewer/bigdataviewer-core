package bdv.ij.export.imgloader;

import ij.ImagePlus;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedShortConverter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import bdv.img.virtualstack.VirtualStackImageLoader;

/**
 * This {@link BasicImgLoader} implementation returns a wrapped, converted
 * {@link ImagePlus}. It is used for exporting {@link ImagePlus} to hdf5.
 *
 * Internally it relies on {@link VirtualStackImageLoader} to be able to handle
 * large virtual stacks.
 *
 * When {@link #getImage(ViewId) loading images}, the provided setup id is used
 * as the channel index of the {@link ImagePlus}, the provided timepoint id is
 * used as the frame index of the {@link ImagePlus}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ImagePlusImgLoader< T extends RealType< T > & NativeType< T > > implements BasicImgLoader< UnsignedShortType >
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
		return new ImagePlusImgLoader< UnsignedByteType >( imp, VirtualStackImageLoader.createUnsignedByteInstance( imp ), minMaxOption, min, max );
	}

	public static ImagePlusImgLoader< UnsignedShortType > createGray16( final ImagePlus imp, final MinMaxOption minMaxOption, final double min, final double max )
	{
		if( imp.getType() != ImagePlus.GRAY16 )
			throw new RuntimeException( "expected ImagePlus type GRAY16" );
		return new ImagePlusImgLoader< UnsignedShortType >( imp, VirtualStackImageLoader.createUnsignedShortInstance( imp ), minMaxOption, min, max );
	}

	public static ImagePlusImgLoader< FloatType > createGray32( final ImagePlus imp, final MinMaxOption minMaxOption, final double min, final double max )
	{
		if( imp.getType() != ImagePlus.GRAY32 )
			throw new RuntimeException( "expected ImagePlus type GRAY32" );
		return new ImagePlusImgLoader< FloatType >( imp, VirtualStackImageLoader.createFloatInstance( imp ), minMaxOption, min, max );
	}

	protected final ImagePlus imp;

	protected final VirtualStackImageLoader< T, ?, ? > loader;

	protected double impMin;

	protected double impMax;

	protected ImagePlusImgLoader( final ImagePlus imp,
			final VirtualStackImageLoader< T, ?, ? > loader,
			final MinMaxOption minMaxOption,
			final double min,
			final double max )
	{
		this.imp = imp;
		this.loader = loader;

		if ( minMaxOption == MinMaxOption.COMPUTE )
		{
			impMin = Double.POSITIVE_INFINITY;
			impMax = Double.NEGATIVE_INFINITY;
			final T minT = loader.getImageType().createVariable();
			final T maxT = loader.getImageType().createVariable();
			final int numSetups = imp.getNChannels();
			final int numTimepoints = imp.getNFrames();
			for ( int t = 0; t < numTimepoints; t++ )
				for ( int s = 0; s < numSetups; ++s )
				{
					ComputeMinMax.computeMinMax( loader.getImage( new ViewId( t, s ) ), minT, maxT );
					impMin = Math.min( minT.getRealDouble(), impMin );
					impMax = Math.max( maxT.getRealDouble(), impMax );
					loader.getCache().clearCache();
				}
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
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
	{
		loader.getCache().clearCache();
		final RandomAccessibleInterval< T > img = loader.getImage( view );
		return Converters.convert( img, new RealUnsignedShortConverter< T >( impMin, impMax ), new UnsignedShortType() );
	}

	@Override
	public UnsignedShortType getImageType()
	{
		return new UnsignedShortType();
	}
}
