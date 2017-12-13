/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

//for the HDF5AccessHack
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dwrite;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT8;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT64;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT16;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT32;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT64;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_DOUBLE;

//for the HDF5Access
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDDoubleArray;


class Hdf5BlockWriterPixelTypes
{
	interface PixelTypeMaintainer
	{
		//for the HDF5AccessHack
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data);

		//for the HDF5Access
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features);
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset);
	}

	// ------------------------------------------------------
	static class ByteTypeMaintainer implements PixelTypeMaintainer
	{
		@Override
		public
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data)
		{
			return H5Dwrite(dataset_id, H5T_NATIVE_INT8, mem_space_id, file_space_id, H5P_DEFAULT, (byte[])data);
		}

		@Override
		public
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features)
		{
			hdf5Writer.int8().createMDArray( path, dimensions, cellDimensions, features );
		}

		@Override
		public
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset)
		{
			final MDByteArray array = new MDByteArray( (byte[])data, reorderedDimensions );
			hdf5Writer.int8().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
		}
	}

	static class UnsignedByteTypeMaintainer implements PixelTypeMaintainer
	{
		@Override
		public
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data)
		{
			return H5Dwrite(dataset_id, H5T_NATIVE_UINT8, mem_space_id, file_space_id, H5P_DEFAULT, (byte[])data);
		}

		@Override
		public
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features)
		{
			hdf5Writer.int8().createMDArray( path, dimensions, cellDimensions, features );
		}

		@Override
		public
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset)
		{
			final MDByteArray array = new MDByteArray( (byte[])data, reorderedDimensions );
			hdf5Writer.int8().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
		}
	}

	// ------------------------------------------------------
	static class ShortTypeMaintainer implements PixelTypeMaintainer
	{
		@Override
		public
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data)
		{
			return H5Dwrite(dataset_id, H5T_NATIVE_INT16, mem_space_id, file_space_id, H5P_DEFAULT, (short[])data);
		}

		@Override
		public
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features)
		{
			hdf5Writer.int16().createMDArray( path, dimensions, cellDimensions, features );
		}

		@Override
		public
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset)
		{
			final MDShortArray array = new MDShortArray( (short[])data, reorderedDimensions );
			hdf5Writer.int16().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
		}
	}

	static class UnsignedShortTypeMaintainer implements PixelTypeMaintainer
	{
		@Override
		public
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data)
		{
			return H5Dwrite(dataset_id, H5T_NATIVE_UINT16, mem_space_id, file_space_id, H5P_DEFAULT, (short[])data);
		}

		@Override
		public
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features)
		{
			hdf5Writer.int16().createMDArray( path, dimensions, cellDimensions, features );
		}

		@Override
		public
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset)
		{
			final MDShortArray array = new MDShortArray( (short[])data, reorderedDimensions );
			hdf5Writer.int16().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
		}
	}

	// ------------------------------------------------------







	/*
		//TODO VLADO: : H5Dwrite() exists for: byte[], double[], float[], int[], long[], short[]
		//Byte
		H5Dwrite( dataSetId, H5T_NATIVE_INT8, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		//Short
		H5Dwrite( dataSetId, H5T_NATIVE_INT16, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		//Int
		H5Dwrite( dataSetId, H5T_NATIVE_INT32, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		//Long
		H5Dwrite( dataSetId, H5T_NATIVE_INT64, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		H5Dwrite( dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
		H5Dwrite( dataSetId, H5T_NATIVE_DOUBLE, memorySpaceId, fileSpaceId, H5P_DEFAULT, data );
	*/
}
