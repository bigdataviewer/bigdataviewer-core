package viewer.hdf5.img;


public interface Hdf5ArrayLoader< A >
{
	public int getBytesPerElement();

	public A loadArray( final int timepoint, final int setup, final int level, int[] dimensions, long[] min );

	public A emptyArray( final int[] dimensions );
}
