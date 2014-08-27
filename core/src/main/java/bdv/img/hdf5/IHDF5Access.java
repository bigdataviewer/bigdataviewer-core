package bdv.img.hdf5;

public interface IHDF5Access
{
	public DimsAndExistence getDimsAndExistence( final ViewLevelId id );

	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final short[] dataBlock ) throws InterruptedException;
}
