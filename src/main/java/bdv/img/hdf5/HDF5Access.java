/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
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
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_GROUP;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_DOUBLE;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT64;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT8;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT16;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT32;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT64;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;
import static net.imglib2.util.Util.long2int;

/**
 * Access chunked data-sets through lower-level HDF5. This avoids opening and
 * closing the dataset for each chunk when accessing through jhdf5 (This is a
 * huge bottleneck when accessing many small chunks).
 *
 * The HDF5 fileId is extracted from a jhdf5 HDF5Reader to avoid having to do
 * everything ourselves.
 *
 * @author Tobias Pietzsch
 */
class HDF5Access
{
	private final IHDF5Reader hdf5Reader;

	private final long fileId;

	private final long numericConversionXferPropertyListID;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	private static final int MAX_OPEN_DATASETS = 48;

	private class OpenDataSet
	{
		final long dataSetId;

		final long fileSpaceId;

		public OpenDataSet( final String pathName )
		{
			dataSetId = H5Dopen( fileId, pathName, H5P_DEFAULT );
			fileSpaceId = H5Dget_space( dataSetId );
		}

		public void close()
		{
			H5Sclose( fileSpaceId );
			H5Dclose( dataSetId );
		}
	}

	private class OpenDataSetCache extends LinkedHashMap< String, OpenDataSet >
	{
		private static final long serialVersionUID = 1L;

		public OpenDataSetCache()
		{
			super( MAX_OPEN_DATASETS, 0.75f, true );
		}

		@Override
		protected boolean removeEldestEntry( final Entry< String, OpenDataSet > eldest )
		{
			if ( size() > MAX_OPEN_DATASETS )
			{
				eldest.getValue().close();
				return true;
			}
			else
				return false;
		}

		public OpenDataSet getDataSet( final String pathName )
		{
			OpenDataSet openDataSet = super.get( pathName );
			if ( openDataSet == null && datasetExists( pathName ) )
			{
				openDataSet = new OpenDataSet( pathName );
				put( pathName, openDataSet );
			}
			return openDataSet;
		}

		private boolean datasetExists( final String pathName )
		{
			final int objectTypeId = "/".equals(pathName) ? H5O_TYPE_GROUP : HDFHelper.H5Oget_info_by_name(fileId, pathName, false).type;
			return objectTypeId == H5O_TYPE_DATASET ;
		}
	}

	private final OpenDataSetCache openDataSetCache;

	public HDF5Access( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;

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

	public synchronized DimsAndExistence getDimsAndExistence( final String pathName )
	{
		final OpenDataSet dataset = openDataSetCache.getDataSet( pathName );
		if ( dataset == null )
		{
			return new DimsAndExistence( new long[] { 1, 1, 1 }, null, false );
		}
		else
		{
			final int nDims = H5Sget_simple_extent_ndims( dataset.fileSpaceId );
			final long[] dimensions = new long[ nDims ];
			H5Sget_simple_extent_dims( dataset.fileSpaceId, dimensions, null );

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

	public void closeAllDataSets()
	{
		for ( final OpenDataSet dataset : openDataSetCache.values() )
			dataset.close();
		openDataSetCache.clear();
	}

	public void close()
	{
		closeAllDataSets();
		int status = 0;
		status = H5Pclose(numericConversionXferPropertyListID);
		if (status < 0) {
			System.err.println("HDF5AccessHack: Error closing property list");
		}
		status = H5Fclose(fileId);
		if (status < 0) {
			System.err.println("HDF5AccessHack: Error closing file");
		}
		hdf5Reader.close();
	}

	public synchronized DataBlock< ? > readBlock(
			final String pathName,
			final DataType dataType,
			final long memTypeId,
			final int[] dimensions,
			final long[] min ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();

		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final DataBlock< ? > block = dataType.createDataBlock( dimensions, min );
		final OpenDataSet dataset = openDataSetCache.getDataSet( pathName );
		final long memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, memTypeId, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, block.getData() );
		H5Sclose( memorySpaceId );

		return block;
	}

	public static long memTypeId( final DataType dataType )
	{
		switch ( dataType )
		{
		case INT8:
			return H5T_NATIVE_INT8;
		case UINT8:
			return H5T_NATIVE_UINT8;
		case INT16:
			return H5T_NATIVE_INT16;
		case UINT16:
			return H5T_NATIVE_UINT16;
		case INT32:
			return H5T_NATIVE_INT32;
		case UINT32:
			return H5T_NATIVE_UINT32;
		case INT64:
			return H5T_NATIVE_INT64;
		case UINT64:
			return H5T_NATIVE_UINT64;
		case FLOAT32:
			return H5T_NATIVE_FLOAT;
		case FLOAT64:
			return H5T_NATIVE_DOUBLE;
		default:
			throw new IllegalArgumentException();
		}
	}
}
