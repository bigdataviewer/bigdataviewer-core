package bdv.mask;

import net.imglib2.converter.Converter;
import net.imglib2.type.mask.AbstractMaskedRealType;
import net.imglib2.type.numeric.RealType;

/**
 * Converts a {@code RealType} to a {@code MaskedRealType}.
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
public class RealToMaskedRealTypeConverter< V extends RealType< V >, T extends AbstractMaskedRealType< V, ?, T > >
	implements Converter< V, T >
{
	@Override
	public void convert( final V input, final T output )
	{
		output.value().set( input );
	}
}
