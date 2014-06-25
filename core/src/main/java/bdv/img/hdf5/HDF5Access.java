package bdv.img.hdf5;

import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class HDF5Access
{
	private final IHDF5Reader hdf5Reader;

	private final int[] reorderedDimensions = new int[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	public HDF5Access( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
	}

	public synchronized HDF5DataSetInformation getDataSetInformation( final ViewLevelId id )
	{
		final String cellsPath = Util.getCellsPath( id );
		return hdf5Reader.getDataSetInformation( cellsPath );
	}

	public synchronized short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );
		final MDShortArray array = hdf5Reader.int16().readMDArrayBlockWithOffset( Util.getCellsPath( timepoint, setup, level ), reorderedDimensions, reorderedMin );
		return array.getAsFlatArray();
	}
}
