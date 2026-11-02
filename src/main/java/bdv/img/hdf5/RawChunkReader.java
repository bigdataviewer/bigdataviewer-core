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

import ch.systemsx.cisd.hdf5.HDF5ChunkIndex;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import hdf.hdf5lib.H5;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.janelia.saalfeldlab.n5.DataType;

import static hdf.hdf5lib.HDF5Constants.H5D_CHUNKED;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;

/**
 * Reads blocks of one chunked HDF5 data set by getting the chunk's compressed
 * bytes directly from the file and decoding them in Java, instead of calling
 * {@code H5Dread}.
 * <p>
 * The location of every chunk is read once with {@code H5Dchunk_iter}
 * ({@link HDF5ChunkIndex}). Reading and decoding a chunk then takes no HDF5
 * library call, so it is not serialized by the library lock and many threads
 * can read and decode in parallel.
 * <p>
 * Only the common case is handled; everything else is left to {@code H5Dread}
 * ({@link #read} returns {@code false}):
 * <ul>
 * <li>the data set is chunked, its filters are deflate and/or shuffle, and its
 * data type is equal to the memory type (so no conversion is needed);</li>
 * <li>the block starts at a chunk boundary and lies within one chunk;</li>
 * <li>the chunk has been written (otherwise the fill value applies) and no
 * filter was skipped for it.</li>
 * </ul>
 * Because a data set's chunk index is read only once, this assumes that the
 * data set does not change while it is open for reading.
 * <p>
 * {@link #tryCreate} reads one chunk both ways and refuses to create a reader
 * if the results differ, which guards against file layouts where the chunk
 * addresses are not plain file offsets.
 */
class RawChunkReader
{
	private static final int H5Z_FILTER_DEFLATE = 1;

	private static final int H5Z_FILTER_SHUFFLE = 2;

	/** Largest number of chunks in the grid for which a lookup table is kept. */
	private static final long MAX_GRID_SIZE = 1L << 26;

	private final FileChannel channel;

	private final long memTypeId;

	private final int rank;

	/** data set dimensions, file order */
	private final long[] dims;

	/** chunk dimensions, file order */
	private final int[] chunkDims;

	private final int elementSize;

	private final int elementsPerChunk;

	/** filter ids in the order they were applied when writing */
	private final int[] filters;

	/** strides for the chunk grid, file order */
	private final long[] gridStrides;

	/** file address of each chunk of the grid, or -1 if the chunk was not written */
	private final long[] addresses;

	private final int[] sizes;

	private final int[] filterMasks;

	private final ThreadLocal< Scratch > scratch = ThreadLocal.withInitial( Scratch::new );

	private class Scratch
	{
		final Inflater inflater = new Inflater();

		byte[] stored = new byte[ 0 ];

		byte[] decoded = new byte[ elementsPerChunk * elementSize ];

		byte[] other = new byte[ elementsPerChunk * elementSize ];

		byte[] block = new byte[ 0 ];
	}

	private RawChunkReader(
			final FileChannel channel,
			final long memTypeId,
			final long[] dims,
			final int[] chunkDims,
			final int elementSize,
			final int[] filters,
			final HDF5ChunkIndex index )
	{
		this.channel = channel;
		this.memTypeId = memTypeId;
		this.rank = dims.length;
		this.dims = dims;
		this.chunkDims = chunkDims;
		this.elementSize = elementSize;
		this.filters = filters;

		int n = 1;
		for ( final int c : chunkDims )
			n *= c;
		elementsPerChunk = n;

		gridStrides = new long[ rank ];
		long gridSize = 1;
		for ( int d = rank - 1; d >= 0; --d )
		{
			gridStrides[ d ] = gridSize;
			gridSize *= gridCount( d );
		}
		addresses = new long[ ( int ) gridSize ];
		sizes = new int[ ( int ) gridSize ];
		filterMasks = new int[ ( int ) gridSize ];
		Arrays.fill( addresses, -1 );
		for ( int i = 0; i < index.getNumberOfChunks(); ++i )
		{
			final int g = gridIndex( index, i );
			addresses[ g ] = index.getAddress( i );
			sizes[ g ] = ( int ) index.getSize( i );
			filterMasks[ g ] = index.getFilterMask( i );
		}
	}

	private long gridCount( final int d )
	{
		return ( dims[ d ] + chunkDims[ d ] - 1 ) / chunkDims[ d ];
	}

	private int gridIndex( final HDF5ChunkIndex index, final int chunk )
	{
		long g = 0;
		for ( int d = 0; d < rank; ++d )
			g += index.getOffset( chunk, d ) / chunkDims[ d ] * gridStrides[ d ];
		return ( int ) g;
	}

	/**
	 * Returns a reader for the data set, or {@code null} if reading it this way
	 * does not apply or does not give the same data as {@code H5Dread}.
	 *
	 * @param fileId
	 *            HDF5 file id, for the {@code H5Dread} check
	 * @param memTypeId
	 *            the HDF5 memory type that blocks will be read as
	 * @param xferPropertyListId
	 *            transfer property list for the {@code H5Dread} check
	 */
	static RawChunkReader tryCreate(
			final IHDF5Reader reader,
			final FileChannel channel,
			final long fileId,
			final String pathName,
			final DataType dataType,
			final long memTypeId,
			final long xferPropertyListId )
	{
		try
		{
			final long dataSetId = H5.H5Dopen( fileId, pathName, H5P_DEFAULT );
			final long[] dims;
			final int[] chunkDims;
			final int elementSize;
			final int[] filters;
			try
			{
				final long dcpl = H5.H5Dget_create_plist( dataSetId );
				try
				{
					if ( H5.H5Pget_layout( dcpl ) != H5D_CHUNKED )
						return null;
					final long space = H5.H5Dget_space( dataSetId );
					try
					{
						final int rank = H5.H5Sget_simple_extent_ndims( space );
						dims = new long[ rank ];
						H5.H5Sget_simple_extent_dims( space, dims, null );
						final long[] longChunkDims = new long[ rank ];
						H5.H5Pget_chunk( dcpl, rank, longChunkDims );
						chunkDims = new int[ rank ];
						for ( int d = 0; d < rank; ++d )
							chunkDims[ d ] = ( int ) longChunkDims[ d ];
					}
					finally
					{
						H5.H5Sclose( space );
					}
					final int numFilters = H5.H5Pget_nfilters( dcpl );
					filters = new int[ numFilters ];
					for ( int i = 0; i < numFilters; ++i )
					{
						filters[ i ] = H5.H5Pget_filter( dcpl, i, new int[ 1 ], new long[] { 8 }, new int[ 8 ], 64, new String[ 1 ], new int[ 1 ] );
						if ( filters[ i ] != H5Z_FILTER_DEFLATE && filters[ i ] != H5Z_FILTER_SHUFFLE )
							return null;
					}
				}
				finally
				{
					H5.H5Pclose( dcpl );
				}

				final long typeId = H5.H5Dget_type( dataSetId );
				try
				{
					// equal types need no conversion, and are in native byte order
					if ( !H5.H5Tequal( typeId, memTypeId ) )
						return null;
					elementSize = ( int ) H5.H5Tget_size( typeId );
				}
				finally
				{
					H5.H5Tclose( typeId );
				}
			}
			finally
			{
				H5.H5Dclose( dataSetId );
			}

			long grid = 1;
			for ( int d = 0; d < dims.length; ++d )
				grid *= ( dims[ d ] + chunkDims[ d ] - 1 ) / chunkDims[ d ];
			if ( dims.length == 0 || grid > MAX_GRID_SIZE )
				return null;

			final HDF5ChunkIndex index = reader.object().getChunkIndex( pathName );
			final RawChunkReader raw = new RawChunkReader( channel, memTypeId, dims, chunkDims, elementSize, filters, index );
			return raw.agreesWithH5Dread( index, fileId, pathName, dataType, xferPropertyListId ) ? raw : null;
		}
		catch ( final RuntimeException e )
		{
			// anything unexpected: do not use this way of reading
			return null;
		}
	}

	/** Compares one chunk read both ways. */
	private boolean agreesWithH5Dread( final HDF5ChunkIndex index, final long fileId, final String pathName,
			final DataType dataType, final long xferPropertyListId )
	{
		if ( index.getNumberOfChunks() == 0 )
			return true; // nothing is read this way
		final int[] size = new int[ rank ];
		final long[] min = new long[ rank ];
		for ( int d = 0; d < rank; ++d )
		{
			min[ d ] = index.getOffset( 0, d );
			size[ d ] = ( int ) Math.min( chunkDims[ d ], dims[ d ] - min[ d ] );
		}
		final Object expected = dataType.createDataBlock( reverse( size ), reverse( min ) ).getData();
		final Object actual = dataType.createDataBlock( reverse( size ), reverse( min ) ).getData();
		final long dataSetId = H5.H5Dopen( fileId, pathName, H5P_DEFAULT );
		try
		{
			final long[] longSize = new long[ rank ];
			for ( int d = 0; d < rank; ++d )
				longSize[ d ] = size[ d ];
			final long memorySpaceId = H5.H5Screate_simple( rank, longSize, null );
			final long fileSpaceId = H5.H5Dget_space( dataSetId );
			H5.H5Sselect_hyperslab( fileSpaceId, H5S_SELECT_SET, min, null, longSize, null );
			H5.H5Dread( dataSetId, memTypeId, memorySpaceId, fileSpaceId, xferPropertyListId, expected );
			H5.H5Sclose( fileSpaceId );
			H5.H5Sclose( memorySpaceId );
		}
		finally
		{
			H5.H5Dclose( dataSetId );
		}
		try
		{
			return read( size, min, actual ) && equalArrays( expected, actual );
		}
		catch ( final IOException | RuntimeException e )
		{
			return false;
		}
	}

	private static long[] reverse( final long[] a )
	{
		final long[] r = new long[ a.length ];
		for ( int i = 0; i < a.length; ++i )
			r[ i ] = a[ a.length - 1 - i ];
		return r;
	}

	private static int[] reverse( final int[] a )
	{
		final int[] r = new int[ a.length ];
		for ( int i = 0; i < a.length; ++i )
			r[ i ] = a[ a.length - 1 - i ];
		return r;
	}

	private static boolean equalArrays( final Object a, final Object b )
	{
		if ( a instanceof byte[] )
			return Arrays.equals( ( byte[] ) a, ( byte[] ) b );
		if ( a instanceof short[] )
			return Arrays.equals( ( short[] ) a, ( short[] ) b );
		if ( a instanceof int[] )
			return Arrays.equals( ( int[] ) a, ( int[] ) b );
		if ( a instanceof long[] )
			return Arrays.equals( ( long[] ) a, ( long[] ) b );
		if ( a instanceof float[] )
			return Arrays.equals( ( float[] ) a, ( float[] ) b );
		if ( a instanceof double[] )
			return Arrays.equals( ( double[] ) a, ( double[] ) b );
		return false;
	}

	long memTypeId()
	{
		return memTypeId;
	}

	/**
	 * Reads a block into {@code data}, if it can be read this way.
	 *
	 * @param size
	 *            block size, file order (the reverse of BigDataViewer's order)
	 * @param min
	 *            block offset, file order
	 * @param data
	 *            primitive array of the matching type with room for the block
	 * @return {@code false} if the block cannot be read this way; {@code data} is
	 *         then unchanged and the caller should use {@code H5Dread}
	 */
	boolean read( final int[] size, final long[] min, final Object data ) throws IOException
	{
		long g = 0;
		boolean whole = true;
		int blockElements = 1;
		for ( int d = 0; d < rank; ++d )
		{
			if ( min[ d ] % chunkDims[ d ] != 0 || size[ d ] > chunkDims[ d ] || min[ d ] + size[ d ] > dims[ d ] )
				return false;
			g += min[ d ] / chunkDims[ d ] * gridStrides[ d ];
			whole &= size[ d ] == chunkDims[ d ];
			blockElements *= size[ d ];
		}
		if ( addresses[ ( int ) g ] < 0 || filterMasks[ ( int ) g ] != 0 )
			return false;

		final Scratch s = scratch.get();
		final byte[] chunk = decodeChunk( s, ( int ) g );
		final byte[] bytes;
		if ( whole )
			bytes = chunk;
		else
		{
			final int blockBytes = blockElements * elementSize;
			if ( s.block.length < blockBytes )
				s.block = new byte[ blockBytes ];
			copySubBlock( chunk, size, s.block );
			bytes = s.block;
		}
		toArray( bytes, data );
		return true;
	}

	/** The bytes of the whole chunk, in the order of the data set (valid until the next call on this thread). */
	private byte[] decodeChunk( final Scratch s, final int g ) throws IOException
	{
		final int stored = sizes[ g ];
		if ( s.stored.length < stored )
			s.stored = new byte[ stored ];
		final ByteBuffer buffer = ByteBuffer.wrap( s.stored, 0, stored );
		long position = addresses[ g ];
		while ( buffer.hasRemaining() )
		{
			final int n = channel.read( buffer, position );
			if ( n < 0 )
				throw new IOException( "unexpected end of file" );
			position += n;
		}

		byte[] in = s.stored;
		int inLength = stored;
		final int chunkBytes = elementsPerChunk * elementSize;
		if ( filters.length == 0 )
		{
			if ( stored != chunkBytes )
				throw new IOException( "unexpected size of chunk" );
			return in;
		}
		// undo the filters in the reverse order of their application
		for ( int i = filters.length - 1; i >= 0; --i )
		{
			final byte[] out = ( in == s.decoded ) ? s.other : s.decoded;
			if ( filters[ i ] == H5Z_FILTER_DEFLATE )
			{
				final Inflater inflater = s.inflater;
				inflater.reset();
				inflater.setInput( in, 0, inLength );
				int n = 0;
				try
				{
					while ( n < chunkBytes && !inflater.finished() )
					{
						final int m = inflater.inflate( out, n, chunkBytes - n );
						if ( m == 0 && ( inflater.needsInput() || inflater.needsDictionary() ) )
							break;
						n += m;
					}
				}
				catch ( final DataFormatException e )
				{
					throw new IOException( e );
				}
				if ( n != chunkBytes )
					throw new IOException( "chunk inflated to " + n + " bytes instead of " + chunkBytes );
			}
			else
			{
				// shuffle: all first bytes of the elements, then all second bytes, ...
				if ( inLength != chunkBytes )
					throw new IOException( "unexpected size of shuffled chunk" );
				for ( int b = 0; b < elementSize; ++b )
					for ( int j = 0, k = b; j < elementsPerChunk; ++j, k += elementSize )
						out[ k ] = in[ b * elementsPerChunk + j ];
			}
			in = out;
			inLength = chunkBytes;
		}
		return in;
	}

	/** Copies the block at the start of the chunk (both row-major, chunk has {@link #chunkDims}). */
	private void copySubBlock( final byte[] chunk, final int[] size, final byte[] block )
	{
		final long[] chunkStrides = new long[ rank ];
		long stride = elementSize;
		for ( int d = rank - 1; d >= 0; --d )
		{
			chunkStrides[ d ] = stride;
			stride *= chunkDims[ d ];
		}
		final int rowBytes = size[ rank - 1 ] * elementSize;
		final int[] index = new int[ rank ];
		int dst = 0;
		while ( true )
		{
			long src = 0;
			for ( int d = 0; d < rank - 1; ++d )
				src += index[ d ] * chunkStrides[ d ];
			System.arraycopy( chunk, ( int ) src, block, dst, rowBytes );
			dst += rowBytes;
			int d = rank - 2;
			while ( d >= 0 && ++index[ d ] == size[ d ] )
				index[ d-- ] = 0;
			if ( d < 0 )
				break;
		}
	}

	private static void toArray( final byte[] bytes, final Object data )
	{
		final ByteBuffer buffer = ByteBuffer.wrap( bytes ).order( ByteOrder.nativeOrder() );
		if ( data instanceof byte[] )
		{
			final byte[] a = ( byte[] ) data;
			System.arraycopy( bytes, 0, a, 0, a.length );
		}
		else if ( data instanceof short[] )
			buffer.asShortBuffer().get( ( short[] ) data );
		else if ( data instanceof int[] )
			buffer.asIntBuffer().get( ( int[] ) data );
		else if ( data instanceof long[] )
			buffer.asLongBuffer().get( ( long[] ) data );
		else if ( data instanceof float[] )
			buffer.asFloatBuffer().get( ( float[] ) data );
		else if ( data instanceof double[] )
			buffer.asDoubleBuffer().get( ( double[] ) data );
		else
			throw new IllegalArgumentException( "unsupported array type " + data.getClass() );
	}
}
