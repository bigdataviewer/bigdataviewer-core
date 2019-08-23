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
	void downsampleBlock( final RandomAccess< T > in, final Cursor< T > out, final int[] dimensions );

	static < T extends RealType< T > > DownsampleBlock< T > create(
			final int[] blockDimensions,
			final int[] downsamplingFactors,
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		return DownsampleBlockInstances.create( blockDimensions, downsamplingFactors, pixelTypeClass, inAccessClass );
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
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		if ( provider == null )
		{
			synchronized ( DownsampleBlockInstances.class )
			{
				if ( provider == null )
					provider = new ClassCopyProvider<>( Imp.class, DownsampleBlock.class, int[].class, int[].class );
			}
		}

		final int numDimensions = blockDimensions.length;

		Object key = Arrays.asList( numDimensions, pixelTypeClass, inAccessClass );
		return provider.newInstanceForKey( key, blockDimensions, downsamplingFactors );
	}

	public static class Imp< T extends RealType< T > > implements DownsampleBlock< T >
	{
		private final int n;

		private final int[] downsamplingFactors;

		private final double scale;

		private final double[] accumulator;

		private final RandomAccess< DoubleType > acc;

		public Imp(
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
			}
			else if ( n == 2 )
			{
				downsampleBlock2D( acc, dimensions[ 0 ], dimensions[ 1 ], in );
				writeOutput2D( out, dimensions[ 0 ], dimensions[ 1 ], acc );
			}
			else
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
}
