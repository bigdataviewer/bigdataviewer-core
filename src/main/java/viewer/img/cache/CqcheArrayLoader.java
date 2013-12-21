package viewer.img.cache;

public interface CqcheArrayLoader< A >
{
	public int getBytesPerElement();

	public A loadArray( final int timepoint, final int setup, final int level, int[] dimensions, long[] min );

	public A emptyArray( final int[] dimensions );
}
