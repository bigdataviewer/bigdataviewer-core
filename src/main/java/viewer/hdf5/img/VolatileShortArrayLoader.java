package viewer.hdf5.img;

import static viewer.hdf5.Util.getCellsPath;
import static viewer.hdf5.Util.reorder;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class VolatileShortArrayLoader implements Hdf5ArrayLoader< VolatileShortArray >
{
	final IHDF5Reader hdf5Reader;

	public VolatileShortArrayLoader( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
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
		return new VolatileShortArray( numEntities, false );
	}

	@Override
	public int getBytesPerElement() {
		return 2;
	}
}
