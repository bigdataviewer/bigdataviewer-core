package bdv.export;

import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;

interface IHDF5Access
{
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo );

	public void createAndOpenDataset( final String path, long[] dimensions, int[] cellDimensions, HDF5IntStorageFeatures features );

	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset );

	public void closeDataset();

	public void close();
}