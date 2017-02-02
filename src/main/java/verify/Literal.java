package verify;

import static verify.State.A;

public class Literal implements Comparable< Literal >
{
	protected final State s;

	protected final State s2;

	protected final int t;

	protected final int t2;

	protected final String type;

	protected final boolean negated;

	private final int hashcode;

	public Literal( final State s, final State s2, final int t, final int t2, final String type, final boolean negated )
	{
		assert ( t >= 0 );
		this.s = s;
		this.s2 = s2;
		this.t = t;
		this.t2 = t2;
		this.type = type;
		this.negated = negated;

		int value = s.hashCode();
		value = 31 * value + s2.hashCode();
		value = 31 * value + t;
		value = 31 * value + t2;
		value = 31 * value + Boolean.hashCode( negated );
		value = 31 * value + type.hashCode();
		hashcode = value;
	}

	public Literal getVariable()
	{
		return negated ? new Literal( s, s2, t, t2, type, false ) : this;
	}

	public boolean negated()
	{
		return negated;
	}

	@Override
	public int hashCode()
	{
		return hashcode;
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( ! ( obj instanceof Literal ) )
			return false;
		final Literal other = ( Literal ) obj;
		return ( other.negated == this.negated )
				&& ( other.t == this.t )
				&& ( other.t2 == this.t2 )
				&& ( other.s == this.s )
				&& ( other.s2 == this.s2 )
				&& ( other.type == this.type );
	}

	@Override
	public String toString()
	{
		return ( negated ? "¬": "" ) + type + "_" + s + t;
	}

	@Override
	public int compareTo( final Literal o )
	{
		int c = t - o.t;
		if ( c != 0 )
			return c;

		c = t2 - o.t2;
		if ( c != 0 )
			return c;

		c = s.compareTo( o.s );
		if ( c != 0 )
			return c;

		c = s2.compareTo( o.s2 );
		if ( c != 0 )
			return c;

		c = type.compareTo( o.type );
		if ( c != 0 )
			return c;

		c = ( negated ? 1 : 0 ) - ( o.negated ? 1 : 0 );
		return c;
	}

	public static Literal p( final State s, final int t )
	{
		return new Literal( s, A, t, 0, "p", false );
	}

	public static Literal q( final State s, final int t, final int v )
	{
		return new Literal( s, A, t, v, "q", false );
	}

	public static Literal f( final State s, final State s2, final int t )
	{
		return new Literal( s, s2, t, 0, "f", false );
	}

	public static Literal f( final Transition tr, final int t )
	{
		return f( tr.from, tr.to, t );
	}

	public static Literal b( final State s, final State s2, final int t )
	{
		return new Literal( s, s2, t, 0, "b", false );
	}

	public static Literal b( final Transition tr, final int t )
	{
		return b( tr.from, tr.to, t );
	}

	public static Literal not( final Literal literal )
	{
		return new Literal( literal.s, literal.s2, literal.t, literal.t2, literal.type, !literal.negated );
	}
}