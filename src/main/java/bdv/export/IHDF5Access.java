package bdv.export;

import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

interface IHDF5Access
{
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo );

	public void createAndOpenDataset( final String path, long[] dimensions, int[] cellDimensions, HDF5IntStorageFeatures features );

	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset );

	public void closeDataset();

	public void close();

	// this is for sharing with Hdf5ImageLoader for loopback loader when exporting
	public IHDF5Writer getIHDF5Writer();
}