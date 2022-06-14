/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.mask;

import java.util.Arrays;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.loops.ClassCopyProvider;
import net.imglib2.type.mask.AbstractMaskedRealType;
import net.imglib2.type.mask.VolatileMaskedRealType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.ValuePair;

public interface VolatileMaskedRealARGBColorConverter< T extends AbstractMaskedRealType< ?, ?, T > > extends ColorConverter, Converter< VolatileMaskedRealType< T >, ARGBType >
{
	static < T extends AbstractMaskedRealType< ?, ?, T > > VolatileMaskedRealARGBColorConverter< T > create( final VolatileMaskedRealType< T > type, final double min, final double max )
	{
		return InstancesV.create( type, min, max );
	}
}

class InstancesV
{
	@SuppressWarnings( "rawtypes" )
	private static ClassCopyProvider< VolatileMaskedRealARGBColorConverter > provider;

	@SuppressWarnings( "unchecked" )
	public static < T extends AbstractMaskedRealType< ?, ?, T > > VolatileMaskedRealARGBColorConverter< T > create( final VolatileMaskedRealType< T > type, final double min, final double max )
	{
		if ( provider == null )
		{
			synchronized ( Instances.class )
			{
				if ( provider == null )
					provider = new ClassCopyProvider<>( Imp.class, VolatileMaskedRealARGBColorConverter.class, double.class, double.class );
			}
		}
		final Object key = Arrays.asList( type.get().value().getClass(), type.get().mask().getClass() );
		return provider.newInstanceForKey( key, min, max );
	}

	public static class Imp< T extends AbstractMaskedRealType< ?, ?, T > > implements VolatileMaskedRealARGBColorConverter< T >
	{
		private double min = 0;

		private double max = 1;

		private final ARGBType color = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );

		private double scaleR;

		private double scaleG;

		private double scaleB;

		private int black;

		public Imp( final double min, final double max )
		{
			this.min = min;
			this.max = max;
			update();
		}

		@Override
		public ARGBType getColor()
		{
			return color.copy();
		}

		@Override
		public void setColor( final ARGBType c )
		{
			color.set( c );
			update();
		}

		@Override
		public boolean supportsColor()
		{
			return true;
		}

		@Override
		public double getMin()
		{
			return min;
		}

		@Override
		public double getMax()
		{
			return max;
		}

		@Override
		public void setMax( final double max )
		{
			this.max = max;
			update();
		}

		@Override
		public void setMin( final double min )
		{
			this.min = min;
			update();
		}

		private void update()
		{
			final double scale = 1.0 / ( max - min );
			final int value = color.get();
			scaleR = ARGBType.red( value ) * scale;
			scaleG = ARGBType.green( value ) * scale;
			scaleB = ARGBType.blue( value ) * scale;
			black = ARGBType.rgba( 0, 0, 0, 0 );
		}

		@Override
		public void convert( final VolatileMaskedRealType< T > input, final ARGBType output )
		{
			final double v = input.get().value().getRealDouble() - min;
			final double alpha = input.get().mask().getRealDouble();
			if ( v < 0 )
			{
				output.set( black );
			}
			else
			{
				final int r0 = ( int ) ( scaleR * v + 0.5 );
				final int g0 = ( int ) ( scaleG * v + 0.5 );
				final int b0 = ( int ) ( scaleB * v + 0.5 );
				final int a0 = ( int ) ( alpha * 255 + 0.5 );
				final int r = Math.min( 255, r0 );
				final int g = Math.min( 255, g0 );
				final int b = Math.min( 255, b0 );
				final int a = Math.min( 255, a0 );
				output.set( ARGBType.rgba( r, g, b, a ) );
			}
		}
	}
}
