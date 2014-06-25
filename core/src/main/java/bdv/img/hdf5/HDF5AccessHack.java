package bdv.img.hdf5;

import static bdv.img.hdf5.Util.reorder;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dopen;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dread;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Screate_simple;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sget_simple_extent_dims;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_hyperslab;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5S_MAX_RANK;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5S_SELECT_SET;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.H5T_NATIVE_INT16;

import java.lang.reflect.Field;

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.hdf5lib.H5D;

public class HDF5AccessHack implements IHDF5Access
{
	private final IHDF5Reader hdf5Reader;

	private final int fileId;

	private final int numericConversionXferPropertyListID;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	public HDF5AccessHack( final IHDF5Reader hdf5Reader ) throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		this.hdf5Reader = hdf5Reader;

		final Class< ? > k = Class.forName( "ch.systemsx.cisd.hdf5.HDF5Reader" );
		final Field f = k.getDeclaredField( "baseReader" );
		f.setAccessible( true );
		final Object baseReader = f.get( hdf5Reader );

		final Class< ? > k2 = Class.forName( "ch.systemsx.cisd.hdf5.HDF5BaseReader" );
		final Field f2 = k2.getDeclaredField( "fileId" );
		f2.setAccessible( true );
		fileId = ( ( Integer ) f2.get( baseReader ) ).intValue();

		final Field f3 = k2.getDeclaredField( "h5" );
		f3.setAccessible( true );
		final Object h5 = f3.get( baseReader );

		final Class< ? > k4 = Class.forName( "ch.systemsx.cisd.hdf5.HDF5" );
		final Field f4 = k4.getDeclaredField( "numericConversionXferPropertyListID" );
		f4.setAccessible( true );
		numericConversionXferPropertyListID = ( ( Integer ) f4.get( h5 ) ).intValue();
	}

	@Override
	public synchronized DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		final String cellsPath = Util.getCellsPath( id );
		final long[] realDimensions = new long[ 3 ];
		boolean exists = false;
		try
		{
			final int dataSetId = H5D.H5Dopen( fileId, cellsPath, H5P_DEFAULT );
			final int fileSpaceId = H5Dget_space( dataSetId );
			final long[] dimensions = new long[ H5S_MAX_RANK ];
			final long[] maxDimensions = new long[ H5S_MAX_RANK ];
			final int rank = H5Sget_simple_extent_dims( fileSpaceId, dimensions, maxDimensions );
			System.arraycopy( dimensions, 0, realDimensions, 0, rank );
			H5Sclose( fileSpaceId );
			H5Dclose( dataSetId );
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
		if ( Thread.interrupted() )
			throw new InterruptedException();
		final String cellsPath = Util.getCellsPath( timepoint, setup, level );
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final int dataSetId = H5Dopen( fileId, cellsPath, H5P_DEFAULT );
		final int fileSpaceId = H5Dget_space( dataSetId );
		final int memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		final short[] dataBlock = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		H5Sselect_hyperslab( fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataSetId, H5T_NATIVE_INT16, memorySpaceId, fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );
		H5Sclose( fileSpaceId );
		H5Dclose( dataSetId );

		return dataBlock;
	}
}
