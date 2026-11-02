/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2026 BigDataViewer developers.
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
package bdv.img.hdf5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Blocks read directly from chunks must be identical to blocks read with
 * {@code H5Dread}, and the ones that cannot be read directly must still be
 * read correctly.
 */
@RunWith( Parameterized.class )
public class RawChunkReaderTest
{
	/** file order: z, y, x; the last chunk in each dimension is partial */
	private static final long[] DIMS = { 20, 37, 45 };

	private static final int[] CHUNK = { 8, 16, 16 };

	@Parameters( name = "{0}" )
	public static Collection< Object[] > parameters()
	{
		final List< Object[] > p = new ArrayList<>();
		for ( final String type : new String[] { "uint16", "float32" } )
			for ( final String filters : new String[] { "none", "deflate", "shuffle+deflate" } )
				p.add( new Object[] { type + " " + filters, type, filters } );
		return p;
	}

	private final String type;

	private final String filters;

	public RawChunkReaderTest( final String name, final String type, final String filters )
	{
		this.type = type;
		this.filters = filters;
	}

	private DataType dataType()
	{
		return type.equals( "uint16" ) ? DataType.UINT16 : DataType.FLOAT32;
	}

	private File createFile( final boolean sparse ) throws IOException
	{
		final File file = File.createTempFile( "raw-chunk-reader", ".h5" );
		file.deleteOnExit();
		final Random random = new Random( 3 );
		try (IHDF5Writer w = HDF5Factory.open( file ))
		{
			final int n = ( int ) ( DIMS[ 0 ] * DIMS[ 1 ] * DIMS[ 2 ] );
			// a smooth part and a noisy part, so that chunks are compressed differently
			final short[] shorts = new short[ n ];
			final float[] floats = new float[ n ];
			for ( int i = 0; i < n; ++i )
			{
				final int v = ( i / 7 ) % 3000 + ( i % 5 == 0 ? random.nextInt( 500 ) : 0 );
				shorts[ i ] = ( short ) v;
				floats[ i ] = v * 0.25f;
			}
			final int[] size = { ( int ) DIMS[ 0 ], ( int ) DIMS[ 1 ], ( int ) DIMS[ 2 ] };
			if ( type.equals( "uint16" ) )
			{
				final HDF5IntStorageFeatures features;
				if ( filters.equals( "none" ) )
					features = HDF5IntStorageFeatures.INT_CHUNKED_UNSIGNED;
				else
					features = new HDF5IntStorageFeatures.HDF5IntStorageFeatureBuilder().unsigned().noScaling().deflateLevel( ( byte ) 2 ).shuffleBeforeDeflate( filters.startsWith( "shuffle" ) ).features();
				w.uint16().createMDArray( "data", DIMS, CHUNK, features );
				if ( sparse )
					w.uint16().writeMDArrayBlockWithOffset( "data", new MDShortArray( blockOf( shorts, 8, 16, 16 ), new int[] { 8, 16, 16 } ), new long[] { 8, 16, 16 } );
				else
					w.uint16().writeMDArrayBlockWithOffset( "data", new MDShortArray( shorts, size ), new long[] { 0, 0, 0 } );
			}
			else
			{
				final HDF5FloatStorageFeatures features = filters.equals( "none" ) ? HDF5FloatStorageFeatures.FLOAT_CHUNKED
						: filters.equals( "deflate" ) ? HDF5FloatStorageFeatures.FLOAT_DEFLATE : HDF5FloatStorageFeatures.FLOAT_SHUFFLE_DEFLATE;
				w.float32().createMDArray( "data", DIMS, CHUNK, features );
				if ( sparse )
					w.float32().writeMDArrayBlockWithOffset( "data", new MDFloatArray( blockOfFloat( floats, 8, 16, 16 ), new int[] { 8, 16, 16 } ), new long[] { 8, 16, 16 } );
				else
					w.float32().writeMDArrayBlockWithOffset( "data", new MDFloatArray( floats, size ), new long[] { 0, 0, 0 } );
			}
		}
		return file;
	}

	private static short[] blockOf( final short[] src, final int a, final int b, final int c )
	{
		final short[] out = new short[ a * b * c ];
		for ( int i = 0; i < out.length; ++i )
			out[ i ] = src[ i ];
		return out;
	}

	private static float[] blockOfFloat( final float[] src, final int a, final int b, final int c )
	{
		final float[] out = new float[ a * b * c ];
		for ( int i = 0; i < out.length; ++i )
			out[ i ] = src[ i ];
		return out;
	}

	private static Object read( final HDF5Access access, final DataType dataType, final int[] sizeXyz, final long[] minXyz ) throws InterruptedException
	{
		final DataBlock< ? > block = access.readBlock( "data", dataType, Util.memTypeId( dataType ), sizeXyz, minXyz );
		return block.getData();
	}

	private static void assertSameData( final Object expected, final Object actual )
	{
		if ( expected instanceof short[] )
			assertArrayEquals( ( short[] ) expected, ( short[] ) actual );
		else
			assertArrayEquals( ( float[] ) expected, ( float[] ) actual, 0 );
	}

	/** x, y, z block sizes and offsets of all chunks of the grid (partial at the boundary). */
	private static List< int[][] > allChunkBlocks()
	{
		final List< int[][] > blocks = new ArrayList<>();
		for ( int z = 0; z < DIMS[ 0 ]; z += CHUNK[ 0 ] )
			for ( int y = 0; y < DIMS[ 1 ]; y += CHUNK[ 1 ] )
				for ( int x = 0; x < DIMS[ 2 ]; x += CHUNK[ 2 ] )
				{
					final int[] size = { ( int ) Math.min( CHUNK[ 2 ], DIMS[ 2 ] - x ), ( int ) Math.min( CHUNK[ 1 ], DIMS[ 1 ] - y ), ( int ) Math.min( CHUNK[ 0 ], DIMS[ 0 ] - z ) };
					blocks.add( new int[][] { size, { x, y, z } } );
				}
		return blocks;
	}

	private static long[] toLong( final int[] a )
	{
		final long[] r = new long[ a.length ];
		for ( int i = 0; i < a.length; ++i )
			r[ i ] = a[ i ];
		return r;
	}

	@Test
	public void testChunkAlignedBlocksAreReadDirectlyAndAreIdentical() throws Exception
	{
		final File file = createFile( false );
		try (HDF5Access direct = new HDF5Access( HDF5Factory.openForReading( file ), true );
				HDF5Access reference = new HDF5Access( HDF5Factory.openForReading( file ), false ))
		{
			final List< int[][] > blocks = allChunkBlocks();
			for ( final int[][] block : blocks )
				assertSameData( read( reference, dataType(), block[ 0 ], toLong( block[ 1 ] ) ), read( direct, dataType(), block[ 0 ], toLong( block[ 1 ] ) ) );
			assertEquals( blocks.size(), direct.rawChunkReads() );
			assertEquals( 0, reference.rawChunkReads() );
		}
	}

	@Test
	public void testOtherBlocksFallBackToH5Dread() throws Exception
	{
		final File file = createFile( false );
		try (HDF5Access direct = new HDF5Access( HDF5Factory.openForReading( file ), true );
				HDF5Access reference = new HDF5Access( HDF5Factory.openForReading( file ), false ))
		{
			final int[][][] blocks = {
					{ { 10, 10, 5 }, { 3, 2, 1 } }, // not aligned to the chunks
					{ { 32, 16, 8 }, { 0, 0, 0 } }, // two chunks wide
					{ { 45, 37, 20 }, { 0, 0, 0 } }, // the whole data set
			};
			for ( final int[][] block : blocks )
				assertSameData( read( reference, dataType(), block[ 0 ], toLong( block[ 1 ] ) ), read( direct, dataType(), block[ 0 ], toLong( block[ 1 ] ) ) );
			assertEquals( 0, direct.rawChunkReads() );
		}
	}

	@Test
	public void testChunksThatWereNeverWrittenFallBackToH5Dread() throws Exception
	{
		final File file = createFile( true );
		try (HDF5Access direct = new HDF5Access( HDF5Factory.openForReading( file ), true );
				HDF5Access reference = new HDF5Access( HDF5Factory.openForReading( file ), false ))
		{
			int expectedDirect = 0;
			for ( final int[][] block : allChunkBlocks() )
			{
				assertSameData( read( reference, dataType(), block[ 0 ], toLong( block[ 1 ] ) ), read( direct, dataType(), block[ 0 ], toLong( block[ 1 ] ) ) );
				if ( block[ 1 ][ 0 ] == 16 && block[ 1 ][ 1 ] == 16 && block[ 1 ][ 2 ] == 8 )
					++expectedDirect; // only the written chunk
			}
			assertEquals( 1, expectedDirect );
			assertEquals( 1, direct.rawChunkReads() );
		}
	}

	@Test
	public void testConcurrentReads() throws Exception
	{
		final File file = createFile( false );
		try (HDF5Access direct = new HDF5Access( HDF5Factory.openForReading( file ), true );
				HDF5Access reference = new HDF5Access( HDF5Factory.openForReading( file ), false ))
		{
			final List< int[][] > blocks = allChunkBlocks();
			final List< Object > expected = new ArrayList<>();
			for ( final int[][] block : blocks )
				expected.add( read( reference, dataType(), block[ 0 ], toLong( block[ 1 ] ) ) );
			final Throwable[] failure = new Throwable[ 1 ];
			final Thread[] threads = new Thread[ 4 ];
			for ( int t = 0; t < threads.length; ++t )
			{
				final int id = t;
				threads[ t ] = new Thread( () -> {
					try
					{
						final Random random = new Random( id );
						for ( int i = 0; i < 300; ++i )
						{
							final int b = random.nextInt( blocks.size() );
							assertSameData( expected.get( b ), read( direct, dataType(), blocks.get( b )[ 0 ], toLong( blocks.get( b )[ 1 ] ) ) );
						}
					}
					catch ( final Throwable e )
					{
						failure[ 0 ] = e;
					}
				} );
				threads[ t ].start();
			}
			for ( final Thread thread : threads )
				thread.join();
			assertEquals( null, failure[ 0 ] );
		}
	}
}
