package bdv.util;

/**
 * A range with {@code minBound <= maxBound}.
 * <p>
 * {@link #join(Bounds) join(other)} will derive a new {@code Bounds}, with
 * {@code minBound} the minimum of {@code this.getMinBound()} and
 * {@code other.getMinBound()} and so on.
 *
 * @author Tobias Pietzsch
 */
public final class Bounds
{
	private final double minBound;
	private final double maxBound;

	public Bounds( final double minBound, final double maxBound )
	{
		if ( minBound > maxBound )
			throw new IllegalArgumentException();

		this.minBound = minBound;
		this.maxBound = maxBound;
	}

	public double getMinBound()
	{
		return minBound;
	}

	public double getMaxBound()
	{
		return maxBound;
	}

	public Bounds join( final Bounds other )
	{
		final double newMinBound = Math.min( minBound, other.minBound );
		final double newMaxBound = Math.max( maxBound, other.maxBound );
		return new Bounds( newMinBound, newMaxBound );
	}

	@Override
	public String toString()
	{
		return "Bounds[ " + minBound + ", " + maxBound + " ]";
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;

		final Bounds that = ( Bounds ) o;

		if ( Double.compare( that.minBound, minBound ) != 0 )
			return false;
		return Double.compare( that.maxBound, maxBound ) == 0;
	}

	@Override
	public int hashCode()
	{
		int result;
		long temp;
		temp = Double.doubleToLongBits( minBound );
		result = ( int ) ( temp ^ ( temp >>> 32 ) );
		temp = Double.doubleToLongBits( maxBound );
		result = 31 * result + ( int ) ( temp ^ ( temp >>> 32 ) );
		return result;
	}
}
