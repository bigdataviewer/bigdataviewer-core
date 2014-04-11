package bdv.img.virtualstack;

import ij.ImagePlus;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import bdv.img.cache.CacheArrayLoader;

public class VirtualStackVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private final VolatileShortArray theEmptyArray;

	private final ImagePlus imp;

	public VirtualStackVolatileShortArrayLoader( final ImagePlus imp )
	{
		theEmptyArray = new VolatileShortArray( imp.getWidth() * imp.getHeight(), false );
		this.imp = imp;
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final int channel = setup + 1;
		final int slice = ( int ) min[ 2 ] + 1;
		final int frame = timepoint + 1;
		final short[] data = ( short[] ) imp.getStack().getProcessor( imp.getStackIndex( channel, slice, frame ) ).getPixels();
		return new VolatileShortArray( data, true );
	}

	@Override
	public VolatileShortArray emptyArray( final int[] dimensions )
	{
		return theEmptyArray;
	}

	@Override
	public int getBytesPerElement()
	{
		return 2;
	}
}
