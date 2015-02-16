package bdv.img.imaris;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import bdv.img.cache.CacheArrayLoader;

public class ImarisVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private final IHDF5Access hdf5Access;

	private VolatileShortArray theEmptyArray;

	public ImarisVolatileShortArrayLoader( final IHDF5Access hdf5Access )
	{
		this.hdf5Access = hdf5Access;
		theEmptyArray = new VolatileShortArray( 32 * 32 * 32, false );
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final short[] array = hdf5Access.readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
		return new VolatileShortArray( array, true );
	}

	@Override
	public VolatileShortArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileShortArray( numEntities, false );
		return theEmptyArray;
	}

	@Override
	public int getBytesPerElement()
	{
		return 2;
	}
}
