package viewer.hdf5.img;

import static viewer.hdf5.Util.getCellsPath;
import static viewer.hdf5.Util.reorder;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class ShortArrayLoader implements Hdf5ArrayLoader< ShortArray >
{
	final IHDF5Reader hdf5Reader;

	public ShortArrayLoader( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
	}

	@Override
	public ShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
	{
		synchronized( hdf5Reader )
		{
			final MDShortArray array = hdf5Reader.readShortMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorder( dimensions ), reorder( min ) );
			return new ShortArray( array.getAsFlatArray() );
		}
	}

	@Override
	public ShortArray emptyArray( final int[] dimensions )
	{
		int numEntities = 1;
		for ( int i = 0; i < dimensions.length; ++i )
			numEntities *= dimensions[ i ];
		return new ShortArray( numEntities );
	}

	@Override
	public int getBytesPerElement() {
		return 2;
	}
}
