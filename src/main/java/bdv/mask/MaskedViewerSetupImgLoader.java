package bdv.mask;

import bdv.ViewerSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.mask.MaskedRealType;
import net.imglib2.type.mask.VolatileMaskedRealType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * TODO: javadoc
 * <p>
 * Wraps {@code ViewerSetupImgLoader<S,U>}
 * and adds a constant mask.
 *
 * @param <S>
 * @param <U>
 * @param <M>
 *
 * @author Tobias Pietzsch
 */
public class MaskedViewerSetupImgLoader<
		S extends RealType< S >,
		U extends Volatile< S >,
		M extends RealType< M > >
	implements ViewerSetupImgLoader<
		MaskedRealType< S, M >,
		VolatileMaskedRealType< MaskedRealType< S, M > > >
{
	private final ViewerSetupImgLoader< S, U > imgLoader;

	private final M mask;

	private final MaskedRealType< S, M > type;

	private final VolatileMaskedRealType< MaskedRealType< S, M > > volatileType;

	public MaskedViewerSetupImgLoader( final ViewerSetupImgLoader< S, U > imgLoader, final M mask )
	{
		this.imgLoader = imgLoader;
		this.mask = mask;
		this.type = createType();
		this.volatileType = createVolatileType();
	}

	private MaskedRealType< S, M > createType()
	{
		return new MaskedRealType<>( imgLoader.getImageType().createVariable(), mask.copy() );
	}

	private VolatileMaskedRealType< MaskedRealType< S, M > > createVolatileType()
	{
		return new VolatileMaskedRealType<>( createType() );
	}

	@Override
	public MaskedRealType< S, M > getImageType()
	{
		return type;
	}

	@Override
	public VolatileMaskedRealType< MaskedRealType< S, M > > getVolatileImageType()
	{
		return volatileType;
	}

	@Override
	public RandomAccessibleInterval< VolatileMaskedRealType< MaskedRealType< S, M > > > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final VolatileMaskedRealType< MaskedRealType< S, M > > maskedType = createVolatileType();
		final RandomAccessibleInterval< U > img = imgLoader.getVolatileImage( timepointId, level, hints );
		final RandomAccessibleInterval< VolatileMaskedRealType< MaskedRealType< S, M > > > maskedImg =
				Views.interval(
						Converters.convert(
								img,
								new VolatileRealToMaskedRealTypeConverter<>(),
								maskedType ),
						img );
		return maskedImg;
	}

	@Override
	public RandomAccessibleInterval< MaskedRealType< S, M > > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		final MaskedRealType< S, M > maskedType = createType();
		final RandomAccessibleInterval< S > img = imgLoader.getImage( timepointId, level, hints );
		final RandomAccessibleInterval< MaskedRealType< S, M > > maskedImg =
				Views.interval(
						Converters.convert(
								img,
								new RealToMaskedRealTypeConverter<>(),
								maskedType ),
						img );
		return maskedImg;
	}

	@Override
	public RandomAccessibleInterval< MaskedRealType< S, M > > getImage( final int timepointId, final ImgLoaderHint... hints )
	{
		return getImage( timepointId, 0, hints );
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return imgLoader.getMipmapResolutions();
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return imgLoader.getMipmapTransforms();
	}

	@Override
	public int numMipmapLevels()
	{
		return imgLoader.numMipmapLevels();
	}
}
