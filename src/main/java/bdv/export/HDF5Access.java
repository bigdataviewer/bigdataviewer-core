package bdv.export;

import static bdv.img.hdf5.Util.reorder;
import bdv.img.hdf5.Util;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

class HDF5Access implements IHDF5Access
{
	private final IHDF5Writer hdf5Writer;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedOffset = new long[ 3 ];

	private String datasetPath;

	public HDF5Access( final IHDF5Writer hdf5Writer )
	{
		this.hdf5Writer = hdf5Writer;
	}

	@Override
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
	{
		hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupIdPartition ), mipmapInfo.getResolutions() );
		hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupIdPartition ), mipmapInfo.getSubdivisions() );
	}

	@Override
	public void createAndOpenDataset( final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features )
	{
		hdf5Writer.int16().createMDArray( path, reorder( dimensions ), reorder( cellDimensions ), features );
		this.datasetPath = path;
	}

	@Override
	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset )
	{
		reorder( blockDimensions, reorderedDimensions );
		reorder( offset, reorderedOffset );
		final MDShortArray array = new MDShortArray( data, reorderedDimensions );
		hdf5Writer.int16().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
	}

	@Override
	public void closeDataset()
	{}

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