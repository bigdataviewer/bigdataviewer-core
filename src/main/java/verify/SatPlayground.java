package verify;

import static verify.Literal.not;
import static verify.Literal.var;
//import static verify.SatPlayground.State.*;
import static verify.SatPlayground.State.A;
import static verify.SatPlayground.State.B;
import static verify.SatPlayground.State.C;
import static verify.SatPlayground.State.D;
import static verify.SatPlayground.State.E;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class SatPlayground
{
	public static enum State
	{
		A,B,C,D,E;
	}

	public static int[][] defaultTransitions = new int[][] {
		{ 1, 1, 1, 1, 1 },
		{ 0, 1, 1, 1, 1 },
		{ 0, 0, 1, 1, 1 },
		{ 0, 0, 0, 1, 1 },
		{ 0, 0, 0, 0, 1 },
	};

	public static final LiteralIndexBimap map = new LiteralIndexBimap();

	public static void addDefaultConstraints( final ISolver solver, final int t ) throws ContradictionException
	{
		final State[] ss = State.values();
		solver.addExactly(
				map.clause(
						Arrays.stream( ss )
								.map( s -> var( s, t ) )
								.collect( Collectors.toList() ) ),
				1 );

		if ( t > 0 )
		{
			for ( int r = 0; r < ss.length; ++r )
			{
				for ( int c = 0; c < ss.length; ++c )
				{
					if ( defaultTransitions[ r ][ c ] == 0 )
					{
						solver.addClause( map.clause(
								not( var( ss[ r ], t - 1 ) ),
								not( var( ss[ c ], t ) ) ) );
					}
				}
			}
		}
	}

	static class Program
	{
		public Program then( final Program p )
		{
			return new Program();
		}

		public void verify( final State a, final State b, final State c )
		{
			// TODO Auto-generated method stub

		}
	}

	public static Program getValue( final Object destination )
	{
		return new Program();
	}

	public static Program synchronize()
	{
		return new Program();
	}

	public static Program ifelse( final List< State > condition, final Program pTrue, final Program pFalse )
	{
		return new Program();
	}

	public static Program assertStates( final State... states )
	{
		return new Program();
	}

	public static void main( final String[] args ) throws ContradictionException, TimeoutException
	{
		final Literal p_A0 = var( A, 0 );
		final Literal p_B0 = var( B, 0 );
		final Literal p_C0 = var( C, 0 );


		final String v = "v";
		final Program p =
				getValue( v ).then(
				synchronize() ).then(
				ifelse(
					Arrays.asList( B ),
					assertStates( B ),
					assertStates( A, C, D, E ) ) );

		p.verify( A, B, C );

		final ISolver solver = SolverFactory.newDefault();
//		solver.addClause( new VecInt( new int[] {  1     } ) );
//		solver.addClause( new VecInt( new int[] { -1,  2 } ) );
//		solver.addClause( new VecInt( new int[] { -2, -3 } ) );
//		solver.addClause( new VecInt( new int[] {  3,  4,  5 } ) );

//		solver.addClause( map.clause( var( A, 0 ) ) );
//		solver.addClause( map.clause( not( var( A, 0 ) ), var( B, 0 ) ) );
//		solver.addClause( map.clause( not( var( B, 0 ) ), not( var( C, 0 ) ) ) );
//		solver.addClause( map.clause( var( C, 0 ), var( D, 0 ), var( E, 0 ) ) );

		addDefaultConstraints( solver, 0 );
		addDefaultConstraints( solver, 1 );

		final IProblem problem = solver;
		final boolean satisfiable = problem.isSatisfiable();
		System.out.println( satisfiable );

		final ModelIterator mi = new ModelIterator( solver );
		boolean unsat = true;
		while ( mi.isSatisfiable() )
		{
			unsat = false;
			final int[] model = mi.model();
			System.out.println( map.model( model ) );
		}


		if ( false )
		{
			System.out.println( var( A, 1 ) );
			System.out.println( var( B, 0 ) );
			System.out.println( var( C, 2 ) );
			System.out.println( var( D, 3 ) );
			System.out.println( var( E, 1 ) );
		}
	}
}
