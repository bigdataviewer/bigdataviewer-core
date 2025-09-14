package bdv.viewer;

import java.util.Arrays;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.loops.ClassCopyProvider;
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

	/**
	 * TODO javadoc.
	 */
	public static < M extends Masked< T >, T extends Type< T > > RandomAccessibleInterval< T > stripMask( RandomAccessibleInterval< M > rai )
	{
		final M maskedType = rai.getType();
		final T type = maskedType.value();
		return Converters.convert2( rai, FromMaskedConverterFactory.create( maskedType ), type::createVariable );
	}

	/**
	 * TODO javadoc.
	 */
	public static < M extends Masked< T >, T extends Type< T > > RealRandomAccessible< T > stripMask( RealRandomAccessible< M > rra )
	{
		final M maskedType = rra.getType();
		final T type = maskedType.value();
		return Converters.convert2( rra, FromMaskedConverterFactory.create( maskedType ), type::createVariable );
	}

	private static class FromMaskedConverterFactory
	{
		@SuppressWarnings( "unchecked" )
		static < M extends Masked< T >, T extends Type< T > > Converter< M, T > create( M type )
		{
			final Object key = Arrays.asList( type.getClass(), type.value().getClass() );
			return ProviderHolder.provider.newInstanceForKey( key );
		}

		private static final class ProviderHolder
		{
			@SuppressWarnings( "rawtypes" )
			static final ClassCopyProvider< Converter > provider = new ClassCopyProvider<>( Imp.class, Converter.class );
		}

		public static class Imp< M extends Masked< T >, T extends Type< T > > implements Converter< M, T >
		{
			@Override
			public void convert( final M input, final T output )
			{
				output.set( input.value() );
			}
		}
	}


}
