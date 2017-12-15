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
//import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT32;
//import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT64;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT8;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT16;
//import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT32;
//import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_UINT64;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_FLOAT;
//import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_DOUBLE;

//for the export/HDF5Access
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
//import ch.systemsx.cisd.base.mdarray.MDIntArray;
//import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
//import ch.systemsx.cisd.base.mdarray.MDDoubleArray;

//for the WriteSequenceToHdf5
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;

//to create the right object
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileByteType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.type.volatiles.VolatileFloatType;

//for Hdf5VolatileTypeArrayLoader's methods
import bdv.img.cache.CacheArrayLoader;
import net.imglib2.img.basictypeaccess.volatiles.VolatileArrayDataAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
//import net.imglib2.img.basictypeaccess.volatiles.array.VolatileUnsignedByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
//import net.imglib2.img.basictypeaccess.volatiles.array.VolatileUnsignedShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import bdv.img.hdf5.IHDF5Access;

//to create the SetupImgLoader
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Hdf5ImageLoader.SetupImgLoader;
import bdv.img.hdf5.Hdf5VolatileTypeArrayLoader;
import bdv.img.hdf5.MipmapInfo;

//for the img/hdf5/HDF5Access
import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dread;

public class Hdf5BlockWriterPixelTypes
{
	public interface PixelTypeMaintainer
	{
		// ---------- writing ----------
		///for the export/HDF5AccessHack
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data);

		///for the export/HDF5Access
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features);
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset);

		///for the WriteSequenceToHdf5
		ArrayImg<?,?> createArrayImg(final long[] dims);

		// ---------- reading ----------
		///for the Hdf5ImageLoader.open(); instantiate always in appropriate pairs, e.g., UnsignedShortType and VolatileUnsignedShortType
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo);

		///for the Hdf5ImageLoader.open()
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access);

		///for methods inside the class Hdf5VolatileTypeArrayLoader
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;
		int getBytesPerElement();

		///for the img/hdf5/HDF5Access
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin );

		///for the img/hdf5/HDF5AccessHack
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock );
	}

	/**
	 * A factory to provide the right implementation of the PixelTypeMaintainer
	 * interface given the input pxType parameter. The parameter should extend
	 * imglib2's RealType to be recognized, otherwise an exception is thrown.
	 */
	public static
	PixelTypeMaintainer createPixelMaintainer(final Object pxType)
	{
		//short-cut... (hoping that boolean testing is faster than instanceof)
		final boolean isString = pxType instanceof String;

		if (pxType instanceof ByteType ||
		   (isString && ((String)pxType).startsWith("ByteType")))
		       return new ByteTypeMaintainer();
		else
		if (pxType instanceof UnsignedByteType ||
		   (isString && ((String)pxType).startsWith("UnsignedByteType")))
		       return new UnsignedByteTypeMaintainer();
		else
		if (pxType instanceof ShortType ||
		   (isString && ((String)pxType).startsWith("ShortType")))
		       return new ShortTypeMaintainer();
		else
		if (pxType instanceof UnsignedShortType ||
		   (isString && ((String)pxType).startsWith("UnsignedShortType")))
		       return new UnsignedShortTypeMaintainer();
		else
		if (pxType instanceof FloatType ||
		   (isString && ((String)pxType).startsWith("FloatType")))
		       return new FloatTypeMaintainer();
		else
			throw new IllegalArgumentException("Unrecognized pixel type, cannot save HDF5");
	}

	// ------------------------------------------------------
	static
	class ByteTypeMaintainer implements PixelTypeMaintainer
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

		@Override
		public
		ArrayImg<?,?> createArrayImg(final long[] dims)
		{
			return ArrayImgs.bytes(dims);
		}

		@Override
		public
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo)
		{
			return loader.new SetupImgLoader<>(setupId, mipmapInfo, new ByteType(), new VolatileByteType());
		}

		@Override
		public
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access)
		{
			return new Hdf5VolatileTypeArrayLoader<VolatileByteArray>( hdf5Access, this );
		}

		@Override
		public
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
		throws InterruptedException
		{
			final byte[] array = hdf5Access.readByteMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
			return new VolatileByteArray( array, true );
		}

		@Override
		public
		int getBytesPerElement() { return 1; }

		@Override
		public
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin )
		{
			return hdf5Reader.int8().readMDArrayBlockWithOffset( path, reorderedDimensions, reorderedMin );
		}

		@Override
		public
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock )
		{
			return H5Dread( dataSetId, H5T_NATIVE_INT8, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, (byte[])dataBlock );
		}
	}

	static
	class UnsignedByteTypeMaintainer implements PixelTypeMaintainer
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

		@Override
		public
		ArrayImg<?,?> createArrayImg(final long[] dims)
		{
			return ArrayImgs.unsignedBytes(dims);
		}

		@Override
		public
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo)
		{
			return loader.new SetupImgLoader<>(setupId, mipmapInfo, new UnsignedByteType(), new VolatileUnsignedByteType());
		}

		@Override
		public
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access)
		{
			//TODO UNSIGNED
			return new Hdf5VolatileTypeArrayLoader<VolatileByteArray>( hdf5Access, this );
		}

		@Override
		public
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
		throws InterruptedException
		{
			//TODO UNSIGNED
			final byte[] array = hdf5Access.readByteMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
			return new VolatileByteArray( array, true );
		}

		@Override
		public
		int getBytesPerElement() { return 1; }

		@Override
		public
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin )
		{
			return hdf5Reader.int8().readMDArrayBlockWithOffset( path, reorderedDimensions, reorderedMin );
		}

		@Override
		public
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock )
		{
			return H5Dread( dataSetId, H5T_NATIVE_UINT8, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, (byte[])dataBlock );
		}
	}

	// ------------------------------------------------------
	static
	class ShortTypeMaintainer implements PixelTypeMaintainer
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

		@Override
		public
		ArrayImg<?,?> createArrayImg(final long[] dims)
		{
			return ArrayImgs.shorts(dims);
		}

		@Override
		public
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo)
		{
			return loader.new SetupImgLoader<>(setupId, mipmapInfo, new ShortType(), new VolatileShortType());
		}

		@Override
		public
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access)
		{
			return new Hdf5VolatileTypeArrayLoader<VolatileShortArray>( hdf5Access, this );
		}

		@Override
		public
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
		throws InterruptedException
		{
			final short[] array = hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
			return new VolatileShortArray( array, true );
		}

		@Override
		public
		int getBytesPerElement() { return 2; }

		@Override
		public
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin )
		{
			return hdf5Reader.int16().readMDArrayBlockWithOffset( path, reorderedDimensions, reorderedMin );
		}

		@Override
		public
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock )
		{
			return H5Dread( dataSetId, H5T_NATIVE_INT16, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, (short[])dataBlock );
		}
	}

	static
	class UnsignedShortTypeMaintainer implements PixelTypeMaintainer
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

		@Override
		public
		ArrayImg<?,?> createArrayImg(final long[] dims)
		{
			return ArrayImgs.unsignedShorts(dims);
		}

		@Override
		public
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo)
		{
			return loader.new SetupImgLoader<>(setupId, mipmapInfo, new UnsignedShortType(), new VolatileUnsignedShortType());
		}

		@Override
		public
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access)
		{
			//TODO UNSIGNED
			return new Hdf5VolatileTypeArrayLoader<VolatileShortArray>( hdf5Access, this );
		}

		@Override
		public
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
		throws InterruptedException
		{
			//TODO UNSIGNED
			final short[] array = hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
			return new VolatileShortArray( array, true );
		}

		@Override
		public
		int getBytesPerElement() { return 2; }

		@Override
		public
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin )
		{
			return hdf5Reader.int16().readMDArrayBlockWithOffset( path, reorderedDimensions, reorderedMin );
		}

		@Override
		public
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock )
		{
			return H5Dread( dataSetId, H5T_NATIVE_UINT16, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, (short[])dataBlock );
		}
	}

	// ------------------------------------------------------
	static
	class FloatTypeMaintainer implements PixelTypeMaintainer
	{
		@Override
		public
		int h5Dwrite(int dataset_id, int mem_space_id, int file_space_id, Object data)
		{
			return H5Dwrite(dataset_id, H5T_NATIVE_FLOAT, mem_space_id, file_space_id, H5P_DEFAULT, (float[])data);
		}

		@Override
		public
		void createAndOpenDataset(final IHDF5Writer hdf5Writer, final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features)
		{
			//final HDF5FloatStorageFeatures fFeatures = HDF5FloatStorageFeatures.FLOAT_NO_COMPRESSION;
			//should translate the original HDF5IntStorageFeatures to HDF5FloatStorageFeatures, TODO VLADO confirm it preserves e.g. deflation option
			final HDF5FloatStorageFeatures fFeatures = HDF5FloatStorageFeatures.build(features).features();
			hdf5Writer.float32().createMDArray( path, dimensions, cellDimensions, fFeatures );
		}

		@Override
		public
		void hdf5writer(final IHDF5Writer hdf5Writer, Object data, final long[] reorderedDimensions, final String datasetPath, final long[] reorderedOffset)
		{
			final MDFloatArray array = new MDFloatArray( (float[])data, reorderedDimensions );
			hdf5Writer.float32().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
		}

		@Override
		public
		ArrayImg<?,?> createArrayImg(final long[] dims)
		{
			return ArrayImgs.floats(dims);
		}

		@Override
		public
		SetupImgLoader<?,?> createSetupImgLoader(final Hdf5ImageLoader loader, final int setupId, final MipmapInfo mipmapInfo)
		{
			return loader.new SetupImgLoader<>(setupId, mipmapInfo, new FloatType(), new VolatileFloatType());
		}

		@Override
		public
		CacheArrayLoader<?> createHdf5VolatileTypeArrayLoader(final IHDF5Access hdf5Access)
		{
			return new Hdf5VolatileTypeArrayLoader<VolatileFloatArray>( hdf5Access, this );
		}

		@Override
		public
		VolatileArrayDataAccess<?> loadArray(final IHDF5Access hdf5Access, final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
		throws InterruptedException
		{
			final float[] array = hdf5Access.readFloatMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
			return new VolatileFloatArray( array, true );
		}

		@Override
		public
		int getBytesPerElement() { return 4; }

		@Override
		public
		MDAbstractArray<?> readMDArrayBlockWithOffset( final IHDF5Reader hdf5Reader, final String path, final int[] reorderedDimensions, final long[] reorderedMin )
		{
			return hdf5Reader.float32().readMDArrayBlockWithOffset( path, reorderedDimensions, reorderedMin );
		}

		@Override
		public
		int h5Dread( final int dataSetId, final int memorySpaceId, final int fileSpaceId, final int numericConversionXferPropertyListID, final Object dataBlock )
		{
			return H5Dread( dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, (float[])dataBlock );
		}
	}
}
