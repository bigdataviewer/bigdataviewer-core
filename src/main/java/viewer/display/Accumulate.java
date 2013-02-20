package viewer.display;

import java.util.List;

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.NumericType;

public abstract class Accumulate< T extends NumericType< T> > extends AbstractEuclideanSpace implements RandomAccessible< T >
{
	final List< RandomAccessible< T > > sources;

	final T type;

	private static < T > int getNumDimensions( final List< RandomAccessible< T > > sources )
	{
		assert !sources.isEmpty();
		// TODO: do all sources have same dimensionality?
		return sources.get( 0 ).numDimensions();
	}

	public Accumulate( final List< RandomAccessible< T > > sources, final T type )
	{
		super( getNumDimensions( sources ) );
		this.sources = sources;
		this.type = type;
	}

	protected abstract void accumulate( final RandomAccess< T >[] accesses, final T target );

	public final class AccumulateRandomAccess implements RandomAccess< T >
	{
		final RandomAccess< T >[] accesses;

		final T target;

		@SuppressWarnings( "unchecked" )
		public AccumulateRandomAccess()
		{
			accesses = new RandomAccess[ sources.size() ];
			for ( int i = 0; i < sources.size(); ++i )
				accesses[ i ] = sources.get( i ).randomAccess();
			target = type.createVariable();
		}

		@SuppressWarnings( "unchecked" )
		public AccumulateRandomAccess( final Interval interval )
		{
			accesses = new RandomAccess[ sources.size() ];
			for ( int i = 0; i < sources.size(); ++i )
				accesses[ i ] = sources.get( i ).randomAccess( interval );
			target = type.createVariable();
		}

		private AccumulateRandomAccess( final AccumulateRandomAccess a )
		{
			accesses = a.accesses.clone();
			for ( int i = 0; i < accesses.length; ++i )
				accesses[ i ] = accesses[ i ].copyRandomAccess();
			target = a.target.copy();
		}

		@Override
		public void localize( final int[] position )
		{
			accesses[ 0 ].localize( position );
		}

		@Override
		public void localize( final long[] position )
		{
			accesses[ 0 ].localize( position );
		}

		@Override
		public int getIntPosition( final int d )
		{
			return accesses[ 0 ].getIntPosition( d );
		}

		@Override
		public long getLongPosition( final int d )
		{
			return accesses[ 0 ].getLongPosition( d );
		}

		@Override
		public void localize( final float[] position )
		{
			accesses[ 0 ].localize( position );
		}

		@Override
		public void localize( final double[] position )
		{
			accesses[ 0 ].localize( position );
		}

		@Override
		public float getFloatPosition( final int d )
		{
			return accesses[ 0 ].getFloatPosition( d );
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return accesses[ 0 ].getDoublePosition( d );
		}

		@Override
		public int numDimensions()
		{
			return n;
		}

		@Override
		public void fwd( final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.fwd( d );
		}

		@Override
		public void bck( final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.bck( d );
		}

		@Override
		public void move( final int distance, final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.move( distance, d );
		}

		@Override
		public void move( final long distance, final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.move( distance, d );
		}

		@Override
		public void move( final Localizable localizable )
		{
			for ( final RandomAccess< T > a : accesses )
				a.move( localizable );
		}

		@Override
		public void move( final int[] distance )
		{
			for ( final RandomAccess< T > a : accesses )
				a.move( distance );
		}

		@Override
		public void move( final long[] distance )
		{
			for ( final RandomAccess< T > a : accesses )
				a.move( distance );
		}

		@Override
		public void setPosition( final Localizable localizable )
		{
			for ( final RandomAccess< T > a : accesses )
				a.setPosition( localizable );
		}

		@Override
		public void setPosition( final int[] position )
		{
			for ( final RandomAccess< T > a : accesses )
				a.setPosition( position );
		}

		@Override
		public void setPosition( final long[] position )
		{
			for ( final RandomAccess< T > a : accesses )
				a.setPosition( position );
		}

		@Override
		public void setPosition( final int position, final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.setPosition( position, d );
		}

		@Override
		public void setPosition( final long position, final int d )
		{
			for ( final RandomAccess< T > a : accesses )
				a.setPosition( position, d );
		}

		@Override
		public T get()
		{
			accumulate( accesses, target );
			return target;
		}

		@Override
		public AccumulateRandomAccess copy()
		{
			return new AccumulateRandomAccess( this );
		}

		@Override
		public AccumulateRandomAccess copyRandomAccess()
		{
			return copy();
		}
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		return new AccumulateRandomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		return new AccumulateRandomAccess( interval );
	}
}
