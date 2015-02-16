package bdv.img.imaris;

import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.ViewLevelId;

public interface IHDF5Access
{
	public DimsAndExistence getDimsAndExistence( final ViewLevelId id );

	public byte[] readByteMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public byte[] readByteMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final byte[] dataBlock ) throws InterruptedException;

	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final short[] dataBlock ) throws InterruptedException;

	public float[] readFloatMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException;

	public float[] readFloatMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final float[] dataBlock ) throws InterruptedException;

	public String readImarisAttributeString( final String objectPath, final String attributeName );

	public String readImarisAttributeString( final String objectPath, final String attributeName, final String defaultValue );
}
