package bdv.mask;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.mask.AbstractMaskedRealType;
import net.imglib2.type.mask.VolatileMaskedRealType;
import net.imglib2.type.numeric.RealType;

/**
 * Converts a {@code Volatile<RealType>} to a {@code MaskedRealType}.
 * It works by just setting the value of the {@code MaskedRealType} to the input value, and leaving the mask untouched.
 * <p>
 * For example, this is useful to mask a {@code RandomAccessibleInterval} with
 * constant 1. The result can then be extended with a constant value mask 0. The
 * result is a {@code RandomAccessible} that is fully opaque inside the original
 * interval and fully transparent outside.
 *
 * @param <V>
 * @param <T>
 */
public class VolatileRealToMaskedRealTypeConverter<
		S extends RealType< S >,
		V extends Volatile< S >,
		T extends AbstractMaskedRealType< S, ?, T > >
	implements Converter< V, VolatileMaskedRealType< T > >
{
	@Override
	public void convert( final V input, final VolatileMaskedRealType< T > output )
	{
		output.setValid( input.isValid() );
		output.get().value().set( input.get() );
	}
}
