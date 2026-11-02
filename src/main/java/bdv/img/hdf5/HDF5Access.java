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

import ch.systemsx.cisd.hdf5.IHDF5FileLevelReadOnlyHandler;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.H5;
import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import hdf.hdf5lib.structs.H5O_info_t;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;

import static bdv.img.hdf5.Util.reorder;
import static hdf.hdf5lib.H5.H5Dclose;
import static hdf.hdf5lib.H5.H5Dget_space;
import static hdf.hdf5lib.H5.H5Dopen;
import static hdf.hdf5lib.H5.H5Dread;
import static hdf.hdf5lib.H5.H5Fclose;
import static hdf.hdf5lib.H5.H5Fopen;
import static hdf.hdf5lib.H5.H5Pclose;
import static hdf.hdf5lib.H5.H5Sclose;
import static hdf.hdf5lib.H5.H5Screate_simple;
import static hdf.hdf5lib.H5.H5Sget_simple_extent_dims;
import static hdf.hdf5lib.H5.H5Sget_simple_extent_ndims;
import static hdf.hdf5lib.H5.H5Sselect_hyperslab;
import static hdf.hdf5lib.H5.H5open;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_DATASET;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static net.imglib2.util.Util.long2int;

/**
 * Access chunked data-sets through lower-level HDF5. This avoids opening and
 * closing the dataset for each chunk when accessing through jhdf5 (This is a
 * huge bottleneck when accessing many small chunks).
 *
 * The HDF5 fileId is extracted from a jhdf5 HDF5Reader to avoid having to do
 * everything ourselves.
 * <p>
 * Where possible, blocks that are exactly one chunk are not read with
 * {@code H5Dread} but by reading the chunk's bytes from the file and decoding
 * them in Java (see {@link RawChunkReader}). {@code H5Dread} calls are
 * serialized by the HDF5 library lock, so with several fetcher threads this
 * is much faster. Set the system property {@value #RAW_CHUNK_READS_PROPERTY}
 * to {@code false} to always use {@code H5Dread}.
 *
 * @author Tobias Pietzsch
 */
class HDF5Access implements AutoCloseable
{
	private final IHDF5Reader hdf5Reader;

	private final long fileId;

	private final long numericConversionXferPropertyListID;

	private static final int MAX_OPEN_DATASETS = 48;

	/** System property that turns reading chunks directly from the file off ({@code false}) or on (default). */
	static final String RAW_CHUNK_READS_PROPERTY = "bdv.hdf5.rawChunkReads";

	private final boolean rawChunkReads;

	/** Per data set path; empty if the data set cannot be read directly. Created on first use. */
	private final Map< String, Optional< RawChunkReader > > rawChunkReaders = new ConcurrentHashMap<>();

	private FileChannel channel;

	/** Number of blocks read directly from chunks, see {@link #rawChunkReads()}. */
	private final AtomicLong rawBlocks = new AtomicLong();

	private class OpenDataSet implements AutoCloseable
	{
		final AtomicInteger refcount;

		final long dataSetId;

		public OpenDataSet( final String pathName )
		{
			refcount = new AtomicInteger( 1 );
			dataSetId = H5Dopen( fileId, pathName, H5P_DEFAULT );
		}

		public void retain()
		{
			if ( refcount.getAndIncrement() <= 0 )
				throw new IllegalStateException();
		}

		@Override
		public void close()
		{
			if ( refcount.decrementAndGet() == 0 )
				H5Dclose( dataSetId );
		}
	}

	private class OpenDataSetCache
	{
		private final Map< String, OpenDataSet > cache;

		public OpenDataSetCache()
		{
			cache = new LinkedHashMap< String, OpenDataSet >( MAX_OPEN_DATASETS, 0.75f, true )
			{
				@Override
				protected boolean removeEldestEntry( final Map.Entry< String, OpenDataSet > eldest )
				{
					if ( size() > MAX_OPEN_DATASETS )
					{
						final OpenDataSet dataSet = eldest.getValue();
						if ( dataSet != null )
							dataSet.close();
						return true;
					}
					else
						return false;
				}
			};
		}

		public synchronized OpenDataSet getDataSet( final String pathName )
		{
			OpenDataSet dataSet = cache.get( pathName );
			if ( dataSet == null && datasetExists( pathName ) )
			{
				dataSet = new OpenDataSet( pathName );
				cache.put( pathName, dataSet );
			}
			if ( dataSet != null )
				dataSet.retain();
			return dataSet;
		}

		public synchronized void clear()
		{
			cache.values().forEach( OpenDataSet::close );
			cache.clear();
		}

		private boolean datasetExists( final String pathName )
		{
			if ( "/".equals( pathName ) )
				return false;

			try
			{
				final H5O_info_t info = H5.H5Oget_info_by_name( fileId, pathName, HDF5Constants.H5O_INFO_BASIC, HDF5Constants.H5P_DEFAULT );
				return info.type == H5O_TYPE_DATASET;
			}
			catch ( HDF5LibraryException e )
			{
				return false;
			}
		}
	}

	private final OpenDataSetCache openDataSetCache;

	public HDF5Access( final IHDF5Reader hdf5Reader )
	{
		this( hdf5Reader, !"false".equalsIgnoreCase( System.getProperty( RAW_CHUNK_READS_PROPERTY ) ) );
	}

	HDF5Access( final IHDF5Reader hdf5Reader, final boolean rawChunkReads )
	{
		this.hdf5Reader = hdf5Reader;
		this.rawChunkReads = rawChunkReads;

		// TODO: Do see ch.systemsx.cisd.hdf5.HDF5.createFileAccessPropertyListId for version bounds checking
		long fileAccessPropertyListId = H5P_DEFAULT;

		IHDF5FileLevelReadOnlyHandler fileHandler = hdf5Reader.file();
		boolean performNumericConversions = fileHandler.isPerformNumericConversions();
		File file = fileHandler.getFile();

		// Make sure library is initialized. This can be called multiple times.
		H5open();

		// See ch.systemsx.cisd.hdf5.HDF5 constructor
		// Make sure to close the numericConversionXferPropertyListID property list created below. See close()
		if (performNumericConversions)
		{
			this.numericConversionXferPropertyListID = HDFHelper.H5Pcreate_xfer_abort_overflow();
		} else {
			this.numericConversionXferPropertyListID = HDFHelper.H5Pcreate_xfer_abort();
		}

		// Make sure to close the fileID created below. See close()
		fileId = H5Fopen(file.getAbsolutePath(), H5F_ACC_RDONLY, fileAccessPropertyListId);

		openDataSetCache = new OpenDataSetCache();
	}

	public DimsAndExistence getDimsAndExistence( final String pathName )
	{
		try( OpenDataSet dataset = openDataSetCache.getDataSet( pathName ) )
		{
			if ( dataset == null )
			{
				return new DimsAndExistence( new long[] { 1, 1, 1 }, null, false );
			}
			else
			{
				final long fileSpaceId = H5Dget_space( dataset.dataSetId );
				final int nDims = H5Sget_simple_extent_ndims( fileSpaceId );
				final long[] dimensions = new long[ nDims ];
				H5Sget_simple_extent_dims( fileSpaceId, dimensions, null );
				H5Sclose( fileSpaceId );

				int[] chunkSizes = null;
				final long creationPropertyList = H5.H5Dget_create_plist( dataset.dataSetId );
				if ( H5.H5Pget_layout( creationPropertyList ) == HDF5Constants.H5D_CHUNKED )
				{
					final long[] longChunkSizes = new long[ nDims ];
					H5.H5Pget_chunk( creationPropertyList, nDims, longChunkSizes );
					chunkSizes = long2int( longChunkSizes );
				}
				H5.H5Pclose( creationPropertyList );

				return new DimsAndExistence( reorder( dimensions ), reorder( chunkSizes ), true );
			}
		}
	}

	public void closeAllDataSets()
	{
		openDataSetCache.clear();
	}

	@Override
	public void close()
	{
		closeAllDataSets();
		closeChannel();
		rawChunkReaders.clear();
		int status = H5Pclose( numericConversionXferPropertyListID );
		if ( status < 0 )
		{
			throw new RuntimeException( "Error closing property list" );
		}
		status = H5Fclose( fileId );
		if ( status < 0 )
		{
			throw new RuntimeException( "Error closing file" );
		}
		hdf5Reader.close();
	}

	/** Number of blocks that were read directly from chunks instead of with {@code H5Dread}. */
	long rawChunkReads()
	{
		return rawBlocks.get();
	}

	private synchronized FileChannel channel() throws IOException
	{
		if ( channel == null )
			channel = FileChannel.open( hdf5Reader.file().getFile().toPath(), StandardOpenOption.READ );
		return channel;
	}

	private synchronized void closeChannel()
	{
		if ( channel != null )
		{
			try
			{
				channel.close();
			}
			catch ( final IOException e )
			{
				// nothing useful to do
			}
			channel = null;
		}
	}

	/**
	 * The reader for the chunks of the data set, or {@code null} if it cannot be
	 * read directly or does not have the memory type {@code memTypeId}.
	 */
	private RawChunkReader rawChunkReader( final String pathName, final DataType dataType, final long memTypeId )
	{
		final Optional< RawChunkReader > reader = rawChunkReaders.computeIfAbsent( pathName, path -> {
			try
			{
				return Optional.ofNullable( RawChunkReader.tryCreate( hdf5Reader, channel(), fileId, path, dataType, memTypeId, numericConversionXferPropertyListID ) );
			}
			catch ( final IOException e )
			{
				return Optional.empty();
			}
		} );
		return reader.isPresent() && reader.get().memTypeId() == memTypeId ? reader.get() : null;
	}

	public DataBlock< ? > readBlock(
			final String pathName,
			final DataType dataType,
			final long memTypeId,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();

		final long[] reorderedDimensions = Util.reorder( dimensions, new long[ dimensions.length ] );
		final long[] reorderedMin = Util.reorder( min );

		// TODO: using min for DataBlock.gridPosition is wrong. Should divide min by blockSize instead.
		//       it's ok for now, because we dont use it, but should be changed when this is pushed down to n5-hdf5
		final DataBlock< ? > block = dataType.createDataBlock( dimensions, min );

		if ( rawChunkReads )
		{
			final RawChunkReader raw = rawChunkReader( pathName, dataType, memTypeId );
			if ( raw != null )
			{
				final int[] reorderedSize = new int[ dimensions.length ];
				for ( int d = 0; d < dimensions.length; ++d )
					reorderedSize[ d ] = ( int ) reorderedDimensions[ d ];
				try
				{
					if ( raw.read( reorderedSize, reorderedMin, block.getData() ) )
					{
						rawBlocks.incrementAndGet();
						return block;
					}
				}
				catch ( final IOException e )
				{
					// fall through to H5Dread, which reports the problem if there is one
				}
			}
		}

		try ( OpenDataSet dataset = openDataSetCache.getDataSet( pathName ) )
		{
			final long memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
			final long fileSpaceId = H5Dget_space( dataset.dataSetId );
			H5Sselect_hyperslab( fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
			H5Dread( dataset.dataSetId, memTypeId, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, block.getData() );
			H5Sclose( fileSpaceId );
			H5Sclose( memorySpaceId );
		}

		return block;
	}
}
