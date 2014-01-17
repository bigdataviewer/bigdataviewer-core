package bdv.img.hdf5;

import static bdv.img.hdf5.Util.getCellsPath;
import static bdv.img.hdf5.Util.reorder;

import java.io.StringWriter;

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

	public static volatile String previousCellsPath = "";

	@Override
	public VolatileShortArray loadArray( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min )
	{
		final MDShortArray array;
		long t0 = System.currentTimeMillis();
		long t1;
		long t2;
		final String cellsPath = getCellsPath( timepoint, setup, level );
		final String prevCellsPath;
		synchronized ( hdf5Reader )
		{
			t1 = System.currentTimeMillis() - t0;
			t0 = System.currentTimeMillis();
			array = hdf5Reader.readShortMDArrayBlockWithOffset( getCellsPath( timepoint, setup, level ), reorder( dimensions ), reorder( min ) );
			t2 = System.currentTimeMillis() - t0;
			prevCellsPath = previousCellsPath;
			previousCellsPath = cellsPath;
		}
		if ( t1 > 5 )
		{
			final StringWriter sw = new StringWriter();
			sw.write( "waited " + t1 + " ms for hdf5Reader lock\n" );
			sw.write( "after   " + prevCellsPath + "\n" );
			sw.write( "loading " + cellsPath + "\n" );
			final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			boolean found = false;
			for ( final StackTraceElement elem : trace )
			{
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "get" ) )
				{
					found = true;
					sw.write( "Hdf5CellCache.get\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "load" ) )
				{
					found = true;
					sw.write( "Hdf5CellCache.load\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Fetcher" ) && elem.getMethodName().equals( "run" ) )
				{
					found = true;
					sw.write( "Fetcher.run\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache" ) && elem.getMethodName().equals( "loadOrEnqueue" ) )
				{
					found = true;
					sw.write( "!!!!!!!!!! VolatileGlobalCellCache.loadOrEnqueue\n" );
					break;
				}
			}
			if ( !found )
			{
				for ( final StackTraceElement elem : trace )
					sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
			}
			System.out.println( sw.toString() );
		}
		if ( t2 > 20 )
		{
			final StringWriter sw = new StringWriter();
			sw.write( "waited " + t2 + " ms for hdf5 readShortMDArrayBlockWithOffset\n" );
			sw.write( "after   " + prevCellsPath + "\n" );
			sw.write( "loading " + cellsPath + "\n" );
			final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			boolean found = false;
			for ( final StackTraceElement elem : trace )
			{
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "get" ) )
				{
					found = true;
					sw.write( "Hdf5CellCache.get\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Hdf5CellCache" ) && elem.getMethodName().equals( "load" ) )
				{
					found = true;
					sw.write( "Hdf5CellCache.load\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache$Fetcher" ) && elem.getMethodName().equals( "run" ) )
				{
					found = true;
					sw.write( "Fetcher.run\n" );
					break;
				}
				if ( elem.getClassName().equals( "bdv.img.cache.VolatileGlobalCellCache" ) && elem.getMethodName().equals( "loadOrEnqueue" ) )
				{
					found = true;
					sw.write( "!!!!!!!!!! VolatileGlobalCellCache.loadOrEnqueue\n" );
					break;
				}
			}
			if ( !found )
			{
				for ( final StackTraceElement elem : trace )
					sw.write( elem.getClassName() + "." + elem.getMethodName() + "\n" );
			}
			System.out.println( sw.toString() );
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
}
