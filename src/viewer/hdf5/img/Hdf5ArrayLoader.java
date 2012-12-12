package viewer.hdf5.img;

public interface Hdf5ArrayLoader< A >
{
	public A loadArray( final int timepoint, final int setup, final int level, int[] dimensions, long[] min );
}
