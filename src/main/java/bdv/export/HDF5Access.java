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
package bdv.export;

import bdv.img.hdf5.Util;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5FileLevelReadOnlyHandler;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import hdf.hdf5lib.exceptions.HDF5LibraryException;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.RawCompression;

import static bdv.img.hdf5.Util.memTypeId;
import static bdv.img.hdf5.Util.reorder;
import static hdf.hdf5lib.H5.H5Dclose;
import static hdf.hdf5lib.H5.H5Dget_space;
import static hdf.hdf5lib.H5.H5Dopen;
import static hdf.hdf5lib.H5.H5Dwrite;
import static hdf.hdf5lib.H5.H5Fclose;
import static hdf.hdf5lib.H5.H5Fopen;
import static hdf.hdf5lib.H5.H5Sclose;
import static hdf.hdf5lib.H5.H5Screate_simple;
import static hdf.hdf5lib.H5.H5Sselect_hyperslab;
import static hdf.hdf5lib.H5.H5open;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDWR;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_DATASET;
import static hdf.hdf5lib.HDF5Constants.H5O_TYPE_GROUP;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;

class HDF5Access
{
	private final IHDF5Writer hdf5Writer;

	private final long fileId;

	private static final int MAX_OPEN_DATASETS = 48;

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
			try
			{
				final int objectTypeId = "/".equals( pathName ) ? H5O_TYPE_GROUP : HDFHelper.H5Oget_info_by_name( fileId, pathName, false ).type;
				return objectTypeId == H5O_TYPE_DATASET;
			}
			catch ( HDF5LibraryException e )
			{
				return false;
			}
		}
	}

	private final OpenDataSetCache openDataSetCache;

	public HDF5Access( final IHDF5Writer hdf5Writer )
	{
		this.hdf5Writer = hdf5Writer;

		// TODO: Do see ch.systemsx.cisd.hdf5.HDF5.createFileAccessPropertyListId for version bounds checking
		long fileAccessPropertyListId = H5P_DEFAULT;

		IHDF5FileLevelReadOnlyHandler fileHandler = hdf5Writer.file();
		boolean performNumericConversions = fileHandler.isPerformNumericConversions();
		File file = fileHandler.getFile();

		// Make sure library is initialized. This can be called multiple times.
		H5open();

		// Make sure to close the fileID created below. See close()
		fileId = H5Fopen(file.getAbsolutePath(), H5F_ACC_RDWR, fileAccessPropertyListId);

		openDataSetCache = new OpenDataSetCache();
	}

	public void writeMipmapDescription( final int setupId, final ExportMipmapInfo mipmapInfo )
	{
		hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupId ), mipmapInfo.getResolutions() );
		hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupId ), mipmapInfo.getSubdivisions() );
	}

	public void writeDataType( final int setupId, final DataType dataType )
	{
		hdf5Writer.string().setAttr( Util.getSetupPath( setupId ), "dataType", dataType.toString() );
	}

	public void createDataset(
			final String pathName,
			final long[] dimensions,
			final int[] blockSize,
			final DataType dataType,
			final Compression compression )
	{
		final HDF5IntStorageFeatures intCompression;
		final HDF5IntStorageFeatures uintCompression;
		final HDF5FloatStorageFeatures floatCompression;
		if (compression instanceof RawCompression ) {
			floatCompression = HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION;
			intCompression = HDF5IntStorageFeatures.INT_NO_COMPRESSION;
			uintCompression = HDF5IntStorageFeatures.INT_NO_COMPRESSION_UNSIGNED;
		} else {
			floatCompression = HDF5FloatStorageFeatures.FLOAT_SHUFFLE_DEFLATE;
			intCompression = HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE;
			uintCompression = HDF5IntStorageFeatures.INT_AUTO_SCALING_DEFLATE_UNSIGNED;
		}

		switch ( dataType )
		{
		case UINT8:
			hdf5Writer.uint8().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), uintCompression );
			break;
		case UINT16:
			hdf5Writer.uint16().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), uintCompression );
			break;
		case UINT32:
			hdf5Writer.uint32().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), uintCompression );
			break;
		case UINT64:
			hdf5Writer.uint64().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), uintCompression );
			break;
		case INT8:
			hdf5Writer.int8().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), intCompression );
			break;
		case INT16:
			hdf5Writer.int16().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), intCompression );
			break;
		case INT32:
			hdf5Writer.int32().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), intCompression );
			break;
		case INT64:
			hdf5Writer.int64().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), intCompression );
			break;
		case FLOAT32:
			hdf5Writer.float32().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), floatCompression );
			break;
		case FLOAT64:
			hdf5Writer.float64().createMDArray( pathName, reorder( dimensions ), reorder( blockSize ), floatCompression );
			break;
		case OBJECT:
			throw new IllegalArgumentException();
		}
	}

	public < T > void writeBlock(
			final String pathName,
			final DatasetAttributes datasetAttributes,
			final DataBlock< T > data )
	{
		final int n = data.getSize().length;
		final long[] reorderedDimensions = reorder( data.getSize(), new long[ n ] );
		final long[] reorderedOffset = reorderMultiply( data.getGridPosition(), datasetAttributes.getBlockSize(), new long[ n ] );

		try ( OpenDataSet dataset = openDataSetCache.getDataSet( pathName ) )
		{
			final long memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
			final long fileSpaceId = H5Dget_space( dataset.dataSetId );
			H5Sselect_hyperslab( fileSpaceId, H5S_SELECT_SET, reorderedOffset, null, reorderedDimensions, null );
			final long memTypeId = memTypeId( datasetAttributes.getDataType() );
			H5Dwrite( dataset.dataSetId, memTypeId, memorySpaceId, fileSpaceId, H5P_DEFAULT, data.getData() );
			H5Sclose( fileSpaceId );
			H5Sclose( memorySpaceId );
		}
	}

	private static long[] reorderMultiply( final long[] in1, final int[] in2, final long[] out )
	{
		assert in1.length == in2.length && in2.length == out.length;
		final int n = in1.length;
		Arrays.setAll( out, d -> in1[ n - 1 - d ] * in2[ n - 1 - d ] );
		return out;
	}

	public void closeAllDataSets()
	{
		openDataSetCache.clear();
	}

	public void close()
	{
		closeAllDataSets();
		int status;
		status = H5Fclose(fileId);
		if ( status < 0 )
		{
			System.err.println( "Error closing file" );
		}
		hdf5Writer.close();
	}

	public IHDF5Writer getIHDF5Writer()
	{
		return hdf5Writer;
	}
}
