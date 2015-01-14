package imaris;

import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import bdv.img.cache.CacheArrayLoader;

public class ImarisVolatileByteArrayLoader implements CacheArrayLoader< VolatileByteArray >
{
	private final IHDF5Access hdf5Access;

	private VolatileByteArray theEmptyArray;

	public ImarisVolatileByteArrayLoader( final IHDF5Access hdf5Access )
	{
		this.hdf5Access = hdf5Access;
		theEmptyArray = new VolatileByteArray( 32 * 32 * 32, false );
	}

	@Override
	public VolatileByteArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final byte[] array = hdf5Access.readByteMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min );
		return new VolatileByteArray( array, true );
	}

	@Override
	public VolatileByteArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
			theEmptyArray = new VolatileByteArray( numEntities, false );
		return theEmptyArray;
	}

	@Override
	public int getBytesPerElement()
	{
		return 1;
	}
}
