/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
import net.imglib2.RandomAccess;
import net.imglib2.loops.ClassCopyProvider;
import net.imglib2.type.numeric.RealType;

public interface CopyBlock< T extends RealType< T > >
{
	void copyBlock( final RandomAccess< T > in, final RandomAccess< T > out, final int[] dimensions );

	static < T extends RealType< T > > CopyBlock< T > create(
			final int numDimensions,
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		return CopyBlockInstances.create( numDimensions, pixelTypeClass, inAccessClass );
	}
}

class CopyBlockInstances
{
	@SuppressWarnings( "rawtypes" )
	private static ClassCopyProvider< CopyBlock > provider;

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > CopyBlock< T > create(
			final int numDimensions,
			final Class< ?  > pixelTypeClass,
			final Class< ? > inAccessClass )
	{
		if ( provider == null )
		{
			synchronized ( CopyBlockInstances.class )
			{
				if ( provider == null )
					provider = new ClassCopyProvider<>( Imp.class, CopyBlock.class, int.class );
			}
		}

		Object key = Arrays.asList( numDimensions, pixelTypeClass, inAccessClass );
		return provider.newInstanceForKey( key, numDimensions );
	}

	public static class Imp< T extends RealType< T > > implements CopyBlock< T >
	{
		private final int n;

		public Imp( final int n )
		{
			if ( n < 1 || n > 3 )
				throw new IllegalArgumentException();

			this.n = n;
		}

		@Override
		public void copyBlock(
				final RandomAccess< T > in,
				final RandomAccess< T > out,
				final int[] dimensions )
		{
			if ( n == 3 )
				copyBlock3D( out, dimensions[ 0 ], dimensions[ 1 ], dimensions[ 2 ], in );
			else if ( n == 2 )
				copyBlock2D( out, dimensions[ 0 ], dimensions[ 1 ], in );
			else
				copyBlock1D( out, dimensions[ 0 ], in );
		}

		private void copyBlock3D(
				final RandomAccess< T > out,
				final int sx, // size of output image
				final int sy,
				final int sz,
				final RandomAccess< T > in )
		{
			for ( int z = 0; z < sz; ++z )
			{
				copyBlock2D( out, sx, sy, in );
				out.fwd( 2 );
				in.fwd( 2 );
			}
			out.move( -sz, 2 );
			in.move( -sz, 2 );
		}

		private void copyBlock2D(
				final RandomAccess< T > out,
				final int sx, // size of output image
				final int sy,
				final RandomAccess< T > in )
		{
			for ( int y = 0; y < sy; ++y )
			{
				copyBlock1D( out, sx, in );
				out.fwd( 1 );
				in.fwd( 1 );
			}
			out.move( -sy, 1 );
			in.move( -sy, 1 );
		}

		private void copyBlock1D(
				final RandomAccess< T > out,
				final int sx, // size of output image
				final RandomAccess< T > in )
		{
			for ( int x = 0; x < sx; ++x )
			{
				out.get().set( in.get() );
				out.fwd( 0 );
				in.fwd( 0 );
			}
			out.move( -sx, 0 );
			in.move( -sx, 0 );
		}
	}
}
