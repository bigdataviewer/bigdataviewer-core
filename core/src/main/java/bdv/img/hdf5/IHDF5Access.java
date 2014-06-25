package bdv.img.hdf5;

import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;

public interface IHDF5Access
{
	public HDF5DataSetInformation getDataSetInformation( final ViewLevelId id );

	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;
}
