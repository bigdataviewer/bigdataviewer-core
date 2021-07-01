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

import gnu.trove.iterator.TLongLongIterator;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TLongSet;
import net.imglib2.*;
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
		Centre,
		Mode
	}

	void downsampleBlock( final RandomAccess< T > in, final Cursor< T > out, final int[] dimensions );

	static < T extends RealType< T > > DownsampleBlock< T > create(
			final int[] blockDimensions,
			final int[] downsamplingFactors,
			final DownsamplingMethod downsamplingMethod,
			final Class< ? > pixelTypeClass,
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
						case Centre:
							provider = new ClassCopyProvider<>( CentreDownsampler.class, DownsampleBlock.class, int[].class, int[].class );
							break;
						case Mode:
							provider = new ClassCopyProvider<>( ModeDownsampler.class, DownsampleBlock.class, int[].class, int[].class );
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

	public static class CentreDownsampler< T extends RealType< T > > implements DownsampleBlock< T >
	{
		private final int n;

		private final int[] downsamplingFactors;

		private final double[] accumulator;

		private final RandomAccess< DoubleType > acc;

		public CentreDownsampler(
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
			final int d = 2;
			final int bsz = downsamplingFactors[ d ];
			in.move( bsz / 2, d );
			for ( int az = 0; az < asz; ++az )
			{
				downsampleBlock2D( acc, asx, asy, in );
				in.move( bsz, d );
				acc.fwd( d );
			}
			in.move( -bsz * asz - bsz / 2, d );
			acc.move( -asz, d );
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
			for ( int ay = 0; ay < asy; ++ay )
			{
				downsampleBlock1D( acc, asx, in );
				in.move( bsy, d );
				acc.fwd( d );
			}
			in.move( -bsy * asy - bsy / 2, d );
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
			for ( int ax = 0; ax < asx; ++ax )
			{
				acc.get().set( in.get().getRealDouble() );
				in.move( bsx, d );
				acc.fwd( d );
			}
			in.move( -bsx * asx - bsx / 2, d );
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

	public static class ModeDownsampler< T extends RealType< T > > implements DownsampleBlock< T >
	{
		private final int n;

		private final int[] downsamplingFactors;

		private final TLongLongHashMapRandomAccess acc;

		public ModeDownsampler(
				final int[] blockDimensions,
				final int[] downsamplingFactors )
		{
			n = blockDimensions.length;
			if ( n < 1 || n > 3 )
				throw new IllegalArgumentException();

			this.downsamplingFactors = downsamplingFactors;

			final int[] dims = new int[ n ];
			Arrays.setAll( dims, d -> blockDimensions[ d ] );

			acc = new TLongLongHashMapRandomAccess( dims );
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
			acc.init();
		}

		private void downsampleBlock3D(
				final TLongLongHashMapRandomAccess acc,
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
				final TLongLongHashMapRandomAccess acc,
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
				final TLongLongHashMapRandomAccess acc,
				final int asx, // size of output (resp accumulator) image
				final RandomAccess< T > in )
		{
			final int bsx = downsamplingFactors[ 0 ];
			final int sx = asx * bsx;
			for ( int x = 0, bx = 0; x < sx; ++x )
			{
				final long value = (long) in.get().getRealDouble();
				final TLongLongHashMap map = acc.get();
				final long newCount = map.get( value ) + 1;
				map.put( value, newCount  );

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
				final TLongLongHashMapRandomAccess acc )
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
				final TLongLongHashMapRandomAccess acc )
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
				final TLongLongHashMapRandomAccess acc )
		{
			for ( int x = 0; x < asx; ++x )
			{
				long label = getLabel( acc );
				out.next().setReal( label );
				acc.fwd( 0 );
			}
			acc.move( -asx, 0 );
		}

		private long getLabel( TLongLongHashMapRandomAccess acc )
		{
			final TLongLongIterator iterator = acc.get().iterator();
			long maxCount = 0;
			long label = 0;
			while ( iterator.hasNext() )
			{
				iterator.advance();
				if ( iterator.value() > maxCount )
				{
					maxCount = iterator.value();
					label = iterator.key();
				}
			}
			return label;
		}
	}

	public static class TLongLongHashMapRandomAccess extends AbstractLocalizableInt implements RandomAccess< TLongLongHashMap >
	{
		private TLongLongHashMap[][][] maps;
		private final int[] dims;

		public TLongLongHashMapRandomAccess( int[] dims )
		{
			super( dims.length );

			this.dims = dims;
			init();
		}

		public void init()
		{
			if( dims.length == 1 )
				maps = new TLongLongHashMap[ dims[ 0 ] ][ 1 ][ 1 ];
			else if ( dims.length == 2 )
				maps = new TLongLongHashMap[ dims[ 0 ] ][ dims[ 1 ] ][ 1 ];
			else if ( dims.length == 3 )
				maps = new TLongLongHashMap[ dims[ 0 ] ][ dims[ 1 ] ][ dims[ 2 ] ];
			else
				throw new UnsupportedOperationException( "The number of dimensions must be  <= 3" );

			for ( int x = 0; x < dims[ 0 ]; x++ )
				for ( int y = 0; y < dims[ 1 ]; y++ )
					for ( int z = 0; z < dims[ 2 ]; z++ )
						maps[ x ][ y ][ z ] = new TLongLongHashMap();
		}

		@Override
		public RandomAccess< TLongLongHashMap > copyRandomAccess()
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void fwd( int d )
		{
			position[ d ]++;
		}

		@Override
		public void bck( int d )
		{
			position[ d ]--;
		}

		@Override
		public void move( int distance, int d )
		{
			position[ d ] += distance;
		}

		@Override
		public void move( long distance, int d )
		{
			position[ d ] += distance;
		}

		@Override
		public void move( Localizable distance )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void move( int[] distance )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void move( long[] distance )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void setPosition( Localizable position )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void setPosition( int[] position )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void setPosition( long[] position )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void setPosition( int position, int d )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void setPosition( long position, int d )
		{
			throw new UnsupportedOperationException(  );
		}

		@Override
		public TLongLongHashMap get()
		{
			return maps[ position[ 0 ] ][ position[ 1 ] ][ position[ 2 ] ];
		}

		@Override
		public Sampler< TLongLongHashMap > copy()
		{
			throw new UnsupportedOperationException(  );
		}
	}
}


