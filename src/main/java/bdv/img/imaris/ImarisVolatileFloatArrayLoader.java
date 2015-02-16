package bdv.img.imaris;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import bdv.img.cache.CacheArrayLoader;

public class ImarisVolatileFloatArrayLoader implements CacheArrayLoader< VolatileFloatArray >
{
	private final IHDF5Access hdf5Access;

	private VolatileFloatArray theEmptyArray;

	public ImarisVolatileFloatArrayLoader( final IHDF5Access hdf5Access )
	{
		this.hdf5Access = hdf5Access;
		theEmptyArray = new VolatileFloatArray( 32 * 32 * 32, false );
	}

	@Override
	public VolatileFloatArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final float[] array = hdf5Access.readFloatMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
		return new VolatileFloatArray( array, true );
	}

	@Override
	public VolatileFloatArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileFloatArray( numEntities, false );
		return theEmptyArray;
	}

	@Override
	public int getBytesPerElement()
	{
		return 2;
	}
}
