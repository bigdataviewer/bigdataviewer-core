package viewer.hdf5.img;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import viewer.hdf5.img.Hdf5ImgCells.CellCache;

public class Hdf5GlobalCellCache< A >
{
	final int numTimepoints = 10;

	final int numSetups = 3;

	final int numLevels = 3;

	class Key
	{
		final int timepoint;

		final int setup;

		final int level;

		final int index;

		public Key( final int timepoint, final int setup, final int level, final int index )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.index = index;

			final long value = ( ( index * numLevels + level ) * numSetups + setup ) * numTimepoints + timepoint;
			hashcode = ( int ) ( value ^ ( value >>> 32 ) );
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof Hdf5GlobalCellCache.Key ) )
				return false;
			final Key that = ( Key ) other;
			return ( this.timepoint == that.timepoint ) && ( this.setup == that.setup ) && ( this.level == that.level ) && ( this.index == that.index );
		}

		final int hashcode;

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	class Entry
	{
		final protected Key key;
		final protected Hdf5Cell< A > data;

		public Entry( final Key key, final Hdf5Cell< A > data )
		{
			this.key = key;
			this.data = data;
		}

		@Override
		public void finalize()
		{
			synchronized ( softReferenceCache )
			{
//				System.out.println( "finalizing..." );
				softReferenceCache.remove( key );
//				System.out.println( softReferenceCache.size() + " tiles chached." );
			}
		}
	}

	final protected HashMap< Key, SoftReference< Entry > > softReferenceCache = new HashMap< Key, SoftReference< Entry > >();

	final protected Hdf5ArrayLoader< A > loader;

	public Hdf5GlobalCellCache( final Hdf5ArrayLoader< A > loader )
	{
		this.loader = loader;
	}

	public Hdf5Cell< A > getGlobalIfCached( final int timepoint, final int setup, final int level, final int index )
	{
		final Key k = new Key( timepoint, setup, level, index );
		final SoftReference< Entry > ref = softReferenceCache.get( k );
		if ( ref != null )
		{
			final Entry entry = ref.get();
			if ( entry != null )
				return entry.data;
		}
		return null;
	}

	public class Hdf5CellCache implements CellCache< A >
	{
		final int timepoint;

		final int setup;

		final int level;

		public Hdf5CellCache( final int timepoint, final int setup, final int level )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
		}

		@Override
		public Hdf5Cell< A > get( final int index )
		{
			return getGlobalIfCached( timepoint, setup, level, index );
		}

		@Override
		public Hdf5Cell< A > load( final int index, final int[] cellDims, final long[] cellMin )
		{
			final Hdf5Cell< A > cell = new Hdf5Cell< A >( cellDims, cellMin, loader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
			final Key k = new Key( timepoint, setup, level, index );
			softReferenceCache.put( k, new SoftReference< Entry >( new Entry( k, cell ) ) );
			return cell;
		}
	}
}
