package verify;

import static verify.Literal.not;
import static verify.Literal.var;
//import static verify.SatPlayground.State.*;
import static verify.SatPlayground.State.A;
import static verify.SatPlayground.State.B;
import static verify.SatPlayground.State.C;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
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


	public interface Constraint
	{
		public void add( final ISolver solver, final int t ) throws ContradictionException;
	}

	public static class HoldStateConstraint implements Constraint
	{
		private final State state;

		private final int from_t;

		public HoldStateConstraint( final State state, final int from_t )
		{
			this.state = state;
			this.from_t = from_t;
		}

		@Override
		public void add( final ISolver solver, final int t ) throws ContradictionException
		{
			assert ( t > from_t );
			solver.addClause( map.clause(
					not( var( state, from_t ) ),
					var( state, t ) ) );
		}
	}

	static class ProblemState
	{
		ArrayList< Constraint > constraints = new ArrayList<>();

		ISolver solver = SolverFactory.newDefault();

		void applyConstraints( final int t ) throws ContradictionException
		{
			for ( final Constraint constraint : constraints )
				constraint.add( solver, t );

			addDefaultConstraints( solver, t );
		}
	}

	static abstract class Program
	{
		protected Program child = null;

		protected Program parent = null;

		protected int t = 0;

		protected String name;

		public Program( final String name )
		{
			this.name = name;
		}

		public Program then( final Program p )
		{
			child = p;
			p.parent = this;
			return p;
		}

		protected void init()
		{
			t = ( parent == null )
					? 0
					: ( parent.t + 1 );
			if ( child != null )
				child.init();
		}

		protected abstract void modifyState( final ProblemState state ) throws ContradictionException;

		// recursively called by children
		public ProblemState getState() throws ContradictionException
		{
			final ProblemState state = ( parent == null )
					? new ProblemState()
					: parent.getState();
			modifyState( state );
			return state;
		}

		protected void printPossibleStates()
		{
			final State[] ss = State.values();
			final ArrayList< State > possible = new ArrayList<>();
			for ( final State s : ss )
			{
				try
				{
					final ProblemState state = getState();
					state.solver.addClause( map.clause( var( s, t ) ) );
					if ( state.solver.isSatisfiable() )
					{
						possible.add( s );
					}
				}
				catch ( final ContradictionException e )
				{}
				catch ( final TimeoutException e )
				{
					e.printStackTrace();
				}
			}
			System.out.println( "(" + t + ") " + name + " : " + possible );
		}

		protected void printPossibleTraces()
		{
			boolean unsat = true;
			try
			{
				final ProblemState state = getState();
				final ModelIterator mi = new ModelIterator( state.solver );
				while ( mi.isSatisfiable() )
				{
					unsat = false;
					final int[] model = mi.model();
					System.out.println( map.model( model ) );
				}
			}
			catch ( final ContradictionException e )
			{}
			catch ( final TimeoutException e )
			{
				e.printStackTrace();
			}
			if ( unsat )
				System.out.println( "no possible trace" );
		}

		public void recursivelyPrintPossibleStates()
		{
			init();
			printPossibleStates();
			if ( child != null )
				child.recursivelyPrintPossibleStates();
		}
	}

	static class Init extends Program
	{
		public Init( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.solver.addClause( map.clause( var( A, 0 ), var( B, 0 ), var( C, 0 ) ) );
			state.applyConstraints( t );
		}
	}

	static class GetValue extends Program
	{
		private final Object ref;

		public GetValue( final Object ref, final String name )
		{
			super( name );
			this.ref = ref;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.constraints.add( new HoldStateConstraint( B, t ) );
			state.applyConstraints( t );
		}
	}

	static class Lock extends Program
	{
		public Lock( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.constraints.add( new HoldStateConstraint( A, t ) );
			state.applyConstraints( t );
		}
	}

	static class Branch extends Program
	{
		public Branch( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			// TODO Auto-generated method stub

		}
	}


	public static void main( final String[] args ) throws ContradictionException, TimeoutException
	{
		final String v = "v";
		final Program p = new Init(  "entry = computeIfAbsent(); " );
		p.then( new GetValue( v,     "v = entry.getValue();      " ) )
		 .then( new Lock(            "synchronized( entry ) {    " ) );

		p.recursivelyPrintPossibleStates();
		p.child.child.printPossibleTraces();






//		final ISolver solver = SolverFactory.newDefault();
//		solver.addClause( map.clause( var( A, 0 ) ) );
//		solver.addClause( map.clause( not( var( A, 0 ) ), var( B, 0 ) ) );
//		solver.addClause( map.clause( not( var( B, 0 ) ), not( var( C, 0 ) ) ) );
//		solver.addClause( map.clause( var( C, 0 ), var( D, 0 ), var( E, 0 ) ) );
//		addDefaultConstraints( solver, 0 );
//		addDefaultConstraints( solver, 1 );
//		final IProblem problem = solver;
//		final boolean satisfiable = problem.isSatisfiable();
//		System.out.println( satisfiable );
//		final ModelIterator mi = new ModelIterator( solver );
//		boolean unsat = true;
//		while ( mi.isSatisfiable() )
//		{
//			unsat = false;
//			final int[] model = mi.model();
//			System.out.println( map.model( model ) );
//		}
	}
}
