package verify;

import static verify.Literal.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sat4j.core.VecInt;

import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class LiteralIndexBimap
{
	private final TObjectIntMap< Literal > varToId;

	private final ArrayList< Literal > idToVar;

	public LiteralIndexBimap()
	{
		varToId = new TObjectIntHashMap<>( Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, 0 );
		idToVar = new ArrayList<>();
		idToVar.add( null );
	}

	public synchronized Literal literal( final int i )
	{
		if ( i == 0 || Math.abs( i ) >= idToVar.size() )
			throw new IllegalArgumentException();
		return i < 0
				? not( idToVar.get( -i ) )
				: idToVar.get( i );
	}

	public synchronized int id( final Literal l )
	{
		final Literal v = l.negated() ? not( l ) : l;
		int i = varToId.get( v );
		if ( i == 0 )
		{
			i = idToVar.size();
			idToVar.add( v );
			varToId.put( v, i );
		}
		return l.negated() ? -i : i;
	}

	public VecInt clause( final Literal... literals )
	{
		return clause( Arrays.asList( literals ) );
	}

	public VecInt clause( final List< Literal > literals )
	{
		final int size = literals.size();
		final int[] clause = new int[ size ];
		for ( int i = 0; i < size; i++ )
			clause[ i ] = id( literals.get( i ) );
		return new VecInt( clause );
	}

	public List< Literal > model( final int[] model )
	{
		final ArrayList< Literal > list = new ArrayList<>();
		for ( int i = 0; i < model.length; i++ )
			if ( model[ i ] > 0 )
				list.add( literal( model[ i ] ) );
		list.sort( null );
		return list;
	}
}