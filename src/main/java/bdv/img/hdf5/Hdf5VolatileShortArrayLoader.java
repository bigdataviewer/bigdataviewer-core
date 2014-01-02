package bdv.img.hdf5;

import static bdv.img.hdf5.Util.getCellsPath;
import static bdv.img.hdf5.Util.reorder;
import bdv.img.cache.CacheArrayLoader;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Hdf5VolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{
	private final IHDF5Reader hdf5Reader;

	private VolatileShortArray theEmptyArray;

	public Hdf5VolatileShortArrayLoader( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
		theEmptyArray = new VolatileShortArray( 32 * 32 * 32, false );
	}

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
	{
		synchronized( hdf5Reader )
		{
			final MDShortArray array = hdf5Reader.readShortMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorder( dimensions ), reorder( min ) );
			return new VolatileShortArray( array.getAsFlatArray(), true );
		}
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
	public int getBytesPerElement() {
		return 2;
	}
}
