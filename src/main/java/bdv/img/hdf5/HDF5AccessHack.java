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
package bdv.img.hdf5;

import static bdv.img.hdf5.Util.reorder;
import static hdf.hdf5lib.H5.H5open;
import static hdf.hdf5lib.H5.H5Dclose;
import static hdf.hdf5lib.H5.H5Dget_space;
import static hdf.hdf5lib.H5.H5Dopen;
import static hdf.hdf5lib.H5.H5Dread;
import static hdf.hdf5lib.H5.H5Sclose;
import static hdf.hdf5lib.H5.H5Screate_simple;
import static hdf.hdf5lib.H5.H5Sget_simple_extent_dims;
import static hdf.hdf5lib.H5.H5Sselect_hyperslab;
import static hdf.hdf5lib.H5.H5Fopen;
import static hdf.hdf5lib.H5.H5Fclose;
import static hdf.hdf5lib.H5.H5Pclose;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static hdf.hdf5lib.HDF5Constants.H5S_MAX_RANK;
import static hdf.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static hdf.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import ch.systemsx.cisd.hdf5.hdf5lib.HDFHelper;
import ch.systemsx.cisd.hdf5.IHDF5FileLevelReadOnlyHandler;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import ch.systemsx.cisd.hdf5.IHDF5Reader;

/**
 * Access chunked data-sets through lower-level HDF5. This avoids opening and
 * closing the dataset for each chunk when accessing through jhdf5 (This is a
 * huge bottleneck when accessing many small chunks).
 *
 * The HDF5 fileId is extracted from a jhdf5 HDF5Reader using reflection to
 * avoid having to do everything ourselves.
 *
 * @author Tobias Pietzsch
 */
class HDF5AccessHack implements IHDF5Access
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

		public OpenDataSet( final String cellsPath )
		{
			dataSetId = H5Dopen( fileId, cellsPath, H5P_DEFAULT );
			fileSpaceId = H5Dget_space( dataSetId );
		}

		public void close()
		{
			H5Sclose( fileSpaceId );
			H5Dclose( dataSetId );
		}
	}

	private class OpenDataSetCache extends LinkedHashMap< ViewLevelId, OpenDataSet >
	{
		private static final long serialVersionUID = 1L;

		public OpenDataSetCache()
		{
			super( MAX_OPEN_DATASETS, 0.75f, true );
		}

		@Override
		protected boolean removeEldestEntry( final Entry< ViewLevelId, OpenDataSet > eldest )
		{
			if ( size() > MAX_OPEN_DATASETS )
			{
				eldest.getValue().close();
				return true;
			}
			else
				return false;
		}

		public OpenDataSet getDataSet( final ViewLevelId id )
		{
			OpenDataSet openDataSet = super.get( id );
			if ( openDataSet == null )
			{
				openDataSet = new OpenDataSet( Util.getCellsPath( id ) );
				put( id, openDataSet );
			}
			return openDataSet;
		}
	}

	private final OpenDataSetCache openDataSetCache;

	public HDF5AccessHack( final IHDF5Reader hdf5Reader ) throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
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

	@Override
	public synchronized DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		final long[] realDimensions = new long[ 3 ];
		boolean exists = false;
		try
		{
			final OpenDataSet dataset = openDataSetCache.getDataSet( id );
			final long[] dimensions = new long[ H5S_MAX_RANK ];
			final long[] maxDimensions = new long[ H5S_MAX_RANK ];
			final int rank = H5Sget_simple_extent_dims( dataset.fileSpaceId, dimensions, maxDimensions );
			System.arraycopy( dimensions, 0, realDimensions, 0, rank );
			exists = true;
		}
		catch ( final Exception e )
		{}
		if ( exists )
			return new DimsAndExistence( reorder( realDimensions ), true );
		else
			return new DimsAndExistence( new long[] { 1, 1, 1 }, false );
	}

	@Override
	public synchronized short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final short[] dataBlock = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min, dataBlock );
		return dataBlock;
	}

	@Override
	public synchronized short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final short[] dataBlock ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final OpenDataSet dataset = openDataSetCache.getDataSet( new ViewLevelId( timepoint, setup, level ) );
		final long memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, H5T_NATIVE_INT16, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );

		return dataBlock;
	}

	@Override
	public float[] readShortMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final float[] dataBlock = new float[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		readShortMDArrayBlockWithOffsetAsFloat( timepoint, setup, level, dimensions, min, dataBlock );
		return dataBlock;
	}

	@Override
	public float[] readShortMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final float[] dataBlock ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final OpenDataSet dataset = openDataSetCache.getDataSet( new ViewLevelId( timepoint, setup, level ) );
		final long memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );
		HDF5Access.unsignedShort( dataBlock );
		return dataBlock;
	}

	@Override
	public void closeAllDataSets()
	{
		for ( final OpenDataSet dataset : openDataSetCache.values() )
			dataset.close();
		openDataSetCache.clear();
	}

	@Override
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

//	@Override
//	protected void finalize() throws Throwable
//	{
//		try
//		{
//			for ( final OpenDataSet dataset : openDataSetCache.values() )
//				dataset.close();
//			openDataSetCache.clear();
//			System.out.println("img.hdf5.HDF5AccessHack.finalize()");
//			hdf5Reader.close();
//		}
//		finally
//		{
//			super.finalize();
//		}
//	}
}
