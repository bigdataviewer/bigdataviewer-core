package verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Transition
{
	final State from;

	final State to;

	public Transition( final State from, final State to )
	{
		this.from = from;
		this.to = to;
	}

	@Override
	public int hashCode()
	{
		return 31 * from.hashCode() + to.hashCode();
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( ! ( obj instanceof Transition ) )
			return false;
		final Transition other = ( Transition ) obj;
		return this.from == other.from && this.to == other.to;
	}

	@Override
	public String toString()
	{
		return "(" + from + "->" + to + ")";
	}

	public static Transition transition( final State from, final State to )
	{
		return new Transition( from, to );
	}

	private static Set< Transition > all;

	public static Set< Transition > allTransitions()
	{
		if ( all == null )
		{
			all = new HashSet<>();
			for ( final State from : State.values() )
				for ( final State to : State.values() )
					all.add( transition( from, to ) );

		}
		return Collections.unmodifiableSet( all );
	}
}