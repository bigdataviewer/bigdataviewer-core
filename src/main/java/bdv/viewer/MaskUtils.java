package bdv.viewer;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.Type;
import net.imglib2.type.mask.Masked;
import net.imglib2.type.mask.interpolation.MaskedClampingNLinearInterpolatorFactory;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class MaskUtils
{
	/**
	 * Zero-extend (value and mask are set to zero) and interpolate a {@code RandomAccessibleInterval<Masked<NumericType>>}.
	 */
	public static < M extends Masked< ? extends NumericType< ? > > & Type< M > > RealRandomAccessible< M > extendAndInterpolateMasked( final RandomAccessibleInterval< M > source, final Interpolation method )
	{
		final InterpolatorFactory< M, RandomAccessible< M > > factory = ( method == Interpolation.NEARESTNEIGHBOR )
				? new NearestNeighborInterpolatorFactory<>()
				: new MaskedClampingNLinearInterpolatorFactory<>();
		final M zero = source.getType().createVariable();
		zero.setMask( 0 );
		zero.value().setZero();
		return Views.interpolate( Views.extendValue( source, zero ), factory );
	}
}
