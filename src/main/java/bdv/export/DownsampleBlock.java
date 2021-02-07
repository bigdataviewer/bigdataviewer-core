/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.export;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.ClassCopyProvider;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

public interface DownsampleBlock< T extends RealType< T > >
{
	enum DownsamplingMethod
	{
		Average,
		CentralPixel
	}

	void downsampleBlock( final RandomAccess< T > in, final Cursor< T > out, final int[] dimensions );

	static < T extends RealType< T > > DownsampleBlock< T > create(
			final int[] blockDimensions,
			final int[] downsamplingFactors,
			final DownsamplingMethod downsamplingMethod,
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		return DownsampleBlockInstances.create( blockDimensions, downsamplingFactors, downsamplingMethod, pixelTypeClass, inAccessClass );
	}
}

class DownsampleBlockInstances
{
	@SuppressWarnings( "rawtypes" )
	private static ClassCopyProvider< DownsampleBlock > provider;

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > DownsampleBlock< T > create(
			final int[] blockDimensions,
			final int[] downsamplingFactors,
			DownsampleBlock.DownsamplingMethod downsamplingMethod,
			final Class< ? > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		if ( provider == null )
		{
			synchronized ( DownsampleBlockInstances.class )
			{
				if ( provider == null )
				{
					switch ( downsamplingMethod )
					{
						case Average:
							provider = new ClassCopyProvider<>( AverageDownsampler.class, DownsampleBlock.class, int[].class, int[].class );
							break;
						case CentralPixel:
							provider = new ClassCopyProvider<>( CentralPixelDownsampler.class, DownsampleBlock.class, int[].class, int[].class );
							break;
					}
				}
			}
		}

		final int numDimensions = blockDimensions.length;

		Object key = Arrays.asList( numDimensions, pixelTypeClass, inAccessClass );
		return provider.newInstanceForKey( key, blockDimensions, downsamplingFactors );
	}

	public static class AverageDownsampler< T extends RealType< T > > implements DownsampleBlock< T >
	{
		private final int n;

		private final int[] downsamplingFactors;

		private final double scale;

		private final double[] accumulator;

		private final RandomAccess< DoubleType > acc;

		public AverageDownsampler(
				final int[] blockDimensions,
				final int[] downsamplingFactors )
		{
			n = blockDimensions.length;
			if ( n < 1 || n > 3 )
				throw new IllegalArgumentException();

			this.downsamplingFactors = downsamplingFactors;
			scale = 1.0 / Intervals.numElements( downsamplingFactors );

			accumulator = new double[ ( int ) Intervals.numElements( blockDimensions ) ];

			final long[] dims = new long[ n ];
			Arrays.setAll( dims, d -> blockDimensions[ d ] );
			acc = ArrayImgs.doubles( accumulator, dims ).randomAccess();
		}

		@Override
		public void downsampleBlock(
				final RandomAccess< T > in,
				final Cursor< T > out, // must be flat iteration order
				final int[] dimensions )
		{
			clearAccumulator();

			if ( n == 3 )
			{
				downsampleBlock3D( acc, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], in );
				writeOutput3D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], acc );
			} else if ( n == 2 )
			{
				downsampleBlock2D( acc, dimensions[ 0 ], dimensions[ 1 ], in );
				writeOutput2D( out, dimensions[ 0 ], dimensions[ 1 ], acc );
			} else
			{
				downsampleBlock1D( acc, dimensions[ 0 ], in );
				writeOutput1D( out, dimensions[ 0 ], acc );
			}
		}

		private void clearAccumulator()
		{
			Arrays.fill( accumulator, 0, accumulator.length, 0 );
		}

		private void downsampleBlock3D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of output (resp accumulator) image
				final int asy,
				final int asz,
				final RandomAccess< T > in )
		{
			final int bsz = downsamplingFactors[ 2 ];
			final int sz = asz * bsz;
			for ( int z = 0, bz = 0; z < sz; ++z )
			{
				downsampleBlock2D( acc, asx, asy, in );
				in.fwd( 2 );
				if ( ++bz == bsz )
				{
					bz = 0;
					acc.fwd( 2 );
				}
			}
			in.move( -sz, 2 );
			acc.move( -asz, 2 );
		}

		private void downsampleBlock2D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of output (resp accumulator) image
				final int asy,
				final RandomAccess< T > in )
		{
			final int bsy = downsamplingFactors[ 1 ];
			final int sy = asy * bsy;
			for ( int y = 0, by = 0; y < sy; ++y )
			{
				downsampleBlock1D( acc, asx, in );
				in.fwd( 1 );
				if ( ++by == bsy )
				{
					by = 0;
					acc.fwd( 1 );
				}
			}
			in.move( -sy, 1 );
			acc.move( -asy, 1 );
		}

		private void downsampleBlock1D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of output (resp accumulator) image
				final RandomAccess< T > in )
		{
			final int bsx = downsamplingFactors[ 0 ];
			final int sx = asx * bsx;
			for ( int x = 0, bx = 0; x < sx; ++x )
			{
				acc.get().set( acc.get().get() + in.get().getRealDouble() );
				in.fwd( 0 );
				if ( ++bx == bsx )
				{
					bx = 0;
					acc.fwd( 0 );
				}
			}
			in.move( -sx, 0 );
			acc.move( -asx, 0 );
		}

		private void writeOutput3D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of output (resp accumulator) image
				final int asy,
				final int asz,
				final RandomAccess< DoubleType > acc )
		{
			for ( int z = 0; z < asz; ++z )
			{
				writeOutput2D( out, asx, asy, acc );
				acc.fwd( 2 );
			}
			acc.move( -asz, 2 );
		}

		private void writeOutput2D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of output (resp accumulator) image
				final int asy,
				final RandomAccess< DoubleType > acc )
		{
			for ( int y = 0; y < asy; ++y )
			{
				writeOutput1D( out, asx, acc );
				acc.fwd( 1 );
			}
			acc.move( -asy, 1 );
		}

		private void writeOutput1D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of output (resp accumulator) image
				final RandomAccess< DoubleType > acc )
		{
			final double scale = this.scale;
			for ( int x = 0; x < asx; ++x )
			{
				out.next().setReal( acc.get().get() * scale );
				acc.fwd( 0 );
			}
			acc.move( -asx, 0 );
		}
	}

	public static class CentralPixelDownsampler< T extends RealType< T > > implements DownsampleBlock< T >
	{
		private final int n;

		private final int[] downsamplingFactors;

		private final double[] accumulator;

		private final RandomAccess< DoubleType > acc;

		public CentralPixelDownsampler(
				final int[] blockDimensions,
				final int[] downsamplingFactors )
		{
			n = blockDimensions.length;
			if ( n < 1 || n > 3 )
				throw new IllegalArgumentException();

			this.downsamplingFactors = downsamplingFactors;

			accumulator = new double[ ( int ) Intervals.numElements( blockDimensions ) ];

			final long[] dims = new long[ n ];
			Arrays.setAll( dims, d -> blockDimensions[ d ] );
			acc = ArrayImgs.doubles( accumulator, dims ).randomAccess();
		}

		@Override
		public void downsampleBlock(
				final RandomAccess< T > in,
				final Cursor< T > out, // must be flat iteration order
				final int[] dimensions )
		{
			clearAccumulator();

			if ( n == 3 )
			{
				downsampleBlock3D( acc, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], in );
				writeOutput3D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], acc );
			} else if ( n == 2 )
			{
				downsampleBlock2D( acc, dimensions[ 0 ], dimensions[ 1 ], in );
				writeOutput2D( out, dimensions[ 0 ], dimensions[ 1 ], acc );
			} else
			{
				downsampleBlock1D( acc, dimensions[ 0 ], in );
				writeOutput1D( out, dimensions[ 0 ], acc );
			}
		}

		private void clearAccumulator()
		{
			Arrays.fill( accumulator, 0, accumulator.length, 0 );
		}

		private void downsampleBlock3D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of accumulator image
				final int asy,
				final int asz,
				final RandomAccess< T > in )
		{
			final int bsz = downsamplingFactors[ 2 ];
			in.move( bsz / 2, 2 );
			for ( int oz = 0; oz < asz; ++oz )
			{
				downsampleBlock2D( acc, asx, asy, in );
				in.move( bsz, 2 );
				acc.fwd( 2 );
			}
			in.move( - bsz * asz - bsz / 2, 2 );
			acc.move( -asz, 2 );
		}

		private void downsampleBlock2D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of accumulator image
				final int asy,
				final RandomAccess< T > in )
		{
			final int d = 1;
			final int bsy = downsamplingFactors[ d ];
			in.move( bsy / 2, d );
			for ( int oy = 0; oy < asy; ++oy )
			{
				downsampleBlock1D( acc, asx, in );
				in.move( bsy, d  );
				acc.fwd( d  );
			}
			in.move( - bsy * asy - bsy / 2, d );
			acc.move( -asy, d );
		}

		private void downsampleBlock1D(
				final RandomAccess< DoubleType > acc,
				final int asx, // size of output image
				final RandomAccess< T > in )
		{
			final int d = 0;
			final int bsx = downsamplingFactors[ d ];
			in.move( bsx / 2, d );
			for ( int ox = 0; ox < asx; ++ox )
			{
				acc.get().set( in.get().getRealDouble() );
				in.move( bsx, d  );
				acc.fwd( d  );
			}
			in.move( - bsx * asx - bsx / 2, d );
			acc.move( -asx, d );
		}

		private void writeOutput3D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of accumulator image
				final int asy,
				final int asz,
				final RandomAccess< DoubleType > acc )
		{
			for ( int z = 0; z < asz; ++z )
			{
				writeOutput2D( out, asx, asy, acc );
				acc.fwd( 2 );
			}
			acc.move( -asz, 2 );
		}

		private void writeOutput2D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of output image
				final int asy,
				final RandomAccess< DoubleType > acc )
		{
			for ( int y = 0; y < asy; ++y )
			{
				writeOutput1D( out, asx, acc );
				acc.fwd( 1 );
			}
			acc.move( -asy, 1 );
		}

		private void writeOutput1D(
				final Cursor< T > out, // must be flat iteration order
				final int asx, // size of output (resp accumulator) image
				final RandomAccess< DoubleType > acc )
		{
			for ( int x = 0; x < asx; ++x )
			{
				out.next().setReal( acc.get().get() );
				acc.fwd( 0 );
			}
			acc.move( -asx, 0 );
		}
	}
}


