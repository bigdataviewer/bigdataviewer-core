package bdv.viewer;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.Type;
import net.imglib2.type.mask.Masked;
import net.imglib2.type.mask.interpolation.MaskedClampingNLinearInterpolatorFactory;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

public class MaskUtils
{
	/**
	 * Zero-extend (value and mask are set to zero) and interpolate a {@code RandomAccessibleInterval<Masked<NumericType>>}.
	 */
	public static < M extends Masked< ? extends NumericType< ? > > & Type< M > > RealRandomAccessible< M > extendAndInterpolateMasked( final RandomAccessibleInterval< M > source,
			final Interpolation method )
	{
		final InterpolatorFactory< M, RandomAccessible< M > > factory = ( method == Interpolation.NEARESTNEIGHBOR )
				? new NearestNeighborInterpolatorFactory<>()
				: new MaskedClampingNLinearInterpolatorFactory<>();
		final M zero = source.getType().createVariable();
		zero.setMask( 0 );
		zero.value().setZero();
		return Views.interpolate( Views.extendValue( source, zero ), factory );
	}

	/**
	 * Combine {@code T} values from {@code img} and alpha values from {@code
	 * mask} into a {@code RandomAccessibleInterval<Masked<T>>}.
	 *
	 * @param img
	 * 		value image
	 * @param mask
	 * 		mask image
	 * @param <T>
	 * 		value type
	 * @param <R>
	 * 		mask type
	 *
	 * @return RandomAccessibleInterval with combined {@code Masked<T>} pixels
	 */
	public static < T extends Type< T >, R extends RealType< R > > RandomAccessibleInterval< ? extends Masked< T > > mask(
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< R > mask )
	{
		final T type = img.getType();
		final R maskType = mask.getType();
		return Converters.convert2(
				img, mask,
				( v, m, t ) -> t.delegate( v, m ),
				() -> new DelegateMaskedType<>( type.createVariable(), maskType.createVariable() ) );
	}

	// TODO: only used for MaskUtils.mask() currently, but probably should move to imglib2 core
	static class DelegateMaskedType< T extends Type< T >, M extends RealType< M > > implements Type< DelegateMaskedType< T, M > >, Masked< T >
	{
		private T value;

		private M mask;

		DelegateMaskedType( final T value, final M mask )
		{
			this.value = value;
			this.mask = mask;
		}

		void delegate( final T value, final M mask )
		{
			this.value = value;
			this.mask = mask;
		}

		// --- Masked< T > ---

		@Override
		public T value()
		{
			return value;
		}

		@Override
		public void setValue( final T value )
		{
			this.value.set( value );
		}

		@Override
		public double mask()
		{
			return mask.getRealDouble();
		}

		@Override
		public void setMask( final double mask )
		{
			this.mask.setReal( mask );
		}

		// --- Type< M > ---

		@Override
		public void set( final DelegateMaskedType< T, M > c )
		{
			setValue( c.value() );
			setMask( c.mask() );
		}

		@Override
		public DelegateMaskedType< T, M > copy()
		{
			final DelegateMaskedType< T, M > copy = createVariable();
			copy.set( Cast.unchecked( this ) );
			return copy;
		}

		@Override
		public DelegateMaskedType< T, M > createVariable()
		{
			return new DelegateMaskedType<>( value.createVariable(), mask.createVariable() );
		}

		// --- ValueEquals< M > ---

		@Override
		public boolean valueEquals( final DelegateMaskedType< T, M > other )
		{
			return mask() == other.mask() && value().valueEquals( other.value() );
		}
	}
}
