package verify;

import verify.SatPlayground.State;

public class Literal implements Comparable< Literal >
{
	protected final State s;

	protected final int t;

	protected final boolean negated;

	public Literal( final State s, final int t )
	{
		this( s, t, false );
	}

	public Literal( final State s, final int t, final boolean negated )
	{
		assert ( t >= 0 );
		this.s = s;
		this.t = t;
		this.negated = negated;
	}

	public Literal getVariable()
	{
		return negated ? new Literal( s, t ) : this;
	}

	public boolean negated()
	{
		return negated;
	}

	@Override
	public int hashCode()
	{
		return 31 * ( 31 * s.hashCode() + t ) + Boolean.hashCode( negated );
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( ! ( obj instanceof Literal ) )
			return false;
		final Literal other = ( Literal ) obj;
		return ( other.negated == this.negated )
				&& ( other.t == this.t )
				&& ( other.s == this.s );
	}

	@Override
	public String toString()
	{
		return ( negated ? "¬": "" ) + "p_" + s + t;
	}

	@Override
	public int compareTo( final Literal o )
	{
		int c = t - o.t;
		if ( c == 0 )
		{
			c = s.compareTo( o.s );
			if ( c == 0 )
			{
				c = ( negated ? 1 : 0 ) - ( o.negated ? 1 : 0 );
			}
		}
		return c;
	}

	public static Literal var( final State s, final int t )
	{
		return new Literal( s, t );
	}

	public static Literal not( final Literal literal )
	{
		return new Literal( literal.s, literal.t, !literal.negated );
	}
}