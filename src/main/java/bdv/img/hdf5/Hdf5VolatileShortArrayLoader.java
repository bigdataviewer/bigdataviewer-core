package bdv.img.hdf5;

import static bdv.img.hdf5.Util.getCellsPath;
import static bdv.img.hdf5.Util.reorder;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import bdv.img.cache.CacheArrayLoader;
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

	private final int[] reorderedDimensions = new int[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final MDShortArray array;
		synchronized ( hdf5Reader )
		{
			if ( Thread.interrupted() )
				throw new InterruptedException();
			reorder( dimensions, reorderedDimensions );
			reorder( min, reorderedMin );
			array = hdf5Reader.readShortMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorderedDimensions, reorderedMin );
		}
		return new VolatileShortArray( array.getAsFlatArray(), true );
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

//	PrintStream log = System.out;
//	public static volatile long pStart = System.currentTimeMillis();
//	public static volatile long pEnd = System.currentTimeMillis();
//	public static volatile long tLoad = 0;
//	public static volatile long sLoad = 0;
//
//	@Override
//	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
//	{
//		final MDShortArray array;
//		synchronized ( hdf5Reader )
//		{
//			pStart = System.currentTimeMillis();
//			final long msBetweenLoads = pStart - pEnd;
//			if ( msBetweenLoads > 2 )
//			{
//				log.println( msBetweenLoads + " ms pause before this load." );
//				final StringWriter sw = new StringWriter();
//				final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
//				for ( final StackTraceElement elem : trace )
//					sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
//				log.println( sw.toString() );
//			}
//			final long t0 = System.currentTimeMillis();
//			reorder( dimensions, reorderedDimensions );
//			reorder( min, reorderedMin );
//			array = hdf5Reader.readShortMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorderedDimensions, reorderedMin );
//			pEnd = System.currentTimeMillis();
//			final long t = System.currentTimeMillis() - t0;
//			final long size = array.size();
//			tLoad += t;
//			sLoad += size;
//			if ( sLoad > 1000000 )
//			{
//				log.println( String.format( "%.0f k shorts/sec ", ( ( double ) sLoad / tLoad ) ) );
//				tLoad = 1;
//				sLoad = 1;
//			}
//		}
//		return new VolatileShortArray( array.getAsFlatArray(), true );
//	}
}
