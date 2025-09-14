/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
