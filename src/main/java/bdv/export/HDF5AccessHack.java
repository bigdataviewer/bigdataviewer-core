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
}