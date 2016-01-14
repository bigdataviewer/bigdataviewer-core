/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import static bdv.img.hdf5.Util.reorder;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dopen;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dwrite;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Screate_simple;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_hyperslab;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;

import java.lang.reflect.Field;

import bdv.img.hdf5.Util;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

class HDF5AccessHack implements IHDF5Access
{
	private final IHDF5Writer hdf5Writer;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedOffset = new long[ 3 ];

	private final int fileId;

	private int dataSetId;

	private int fileSpaceId;

	public HDF5AccessHack( final IHDF5Writer hdf5Writer ) throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		this.hdf5Writer = hdf5Writer;

		final Class< ? > k = Class.forName( "ch.systemsx.cisd.hdf5.HDF5Writer" );
		final Field f = k.getDeclaredField( "baseWriter" );
		f.setAccessible( true );
		final Object baseWriter = f.get( hdf5Writer );

		final Class< ? > k2 = Class.forName( "ch.systemsx.cisd.hdf5.HDF5BaseReader" );
		final Field f2 = k2.getDeclaredField( "fileId" );
		f2.setAccessible( true );
		fileId = ( ( Integer ) f2.get( baseWriter ) ).intValue();
	}

	@Override
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
	{
		hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupIdPartition ), mipmapInfo.getResolutions() );
		hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupIdPartition ), mipmapInfo.getSubdivisions() );
	}

	@Override
	public void closeDataset()
	{
		H5Sclose( fileSpaceId );
		H5Dclose( dataSetId );
	}

	@Override
	public void createAndOpenDataset( final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features )
	{
		hdf5Writer.int16().createMDArray( path, reorder( dimensions ), reorder( cellDimensions ), features );
		dataSetId = H5Dopen( fileId, path, H5P_DEFAULT );
		fileSpaceId = H5Dget_space( dataSetId );
	}

	@Override
	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset )
	{
		reorder( blockDimensions, reorderedDimensions );
		reorder( offset, reorderedOffset );
		final int memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( fileSpaceId, H5S_SELECT_SET, reorderedOffset, null, reorderedDimensions, null );
		H5Dwrite( dataSetId, H5T_NATIVE_INT16, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		H5Sclose( memorySpaceId );
	}

	@Override
	public void close()
	{
		hdf5Writer.close();
	}

	@Override
	public IHDF5Writer getIHDF5Writer()
	{
		return hdf5Writer;
	}
}
