package verify;

import static verify.Literal.b;
import static verify.Literal.f;
import static verify.Literal.not;
import static verify.Literal.p;
import static verify.Literal.q;
import static verify.State.A;
import static verify.State.B;
import static verify.State.C;
import static verify.State.D;
import static verify.State.E;
import static verify.Transition.allTransitions;
import static verify.Transition.transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class SatPlayground
{
	public static final LiteralIndexBimap map = new LiteralIndexBimap();

	public static void addDefaultConstraints(
			final ISolver solver,
			final int t,
			final Collection< Transition > pblocked,
			final Collection< Transition > forced ) throws ContradictionException
	{
		// sum_s( p_st ) == 1
		final State[] ss = State.values();
		solver.addExactly(
				map.clause(
						Arrays.stream( ss )
								.map( s -> p( s, t ) )
								.collect( Collectors.toList() ) ),
				1 );

		if ( t != 0 )
		{
			// sum_s( q_stv ) == 1
			for ( int vv = 0; vv < ss.length; ++vv )
			{
				final int v = vv;
				solver.addExactly(
						map.clause(
								Arrays.stream( ss )
										.map( s -> q( s, t, v ) )
										.collect( Collectors.toList() ) ),
						1 );
			}

			// p_st- -> q_st0
			// q_stmax -> p_st
			for ( final State s : ss )
			{
				solver.addClause( map.clause(
						not( p( s, t - 1 ) ), q( s, t, 0 ) ) );
				solver.addClause( map.clause(
						not( q( s, t, ss.length - 1 ) ), p( s, t ) ) );
			}

			// (!f_ss`t & b_ss`t) -> (q_stv- -> !q_s`tv)
			for ( int v = 1; v < ss.length; ++v )
				for ( final Transition tr : allTransitions() )
					solver.addClause( map.clause(
							f( tr, t ), not( b( tr, t ) ), not( q( tr.from, t, v - 1 ) ), not( q( tr.to, t, v ) ) ) );

			// (f_ss`t -> (p_st- -> p_s`t)
			for ( final Transition tr : allTransitions() )
				solver.addClause( map.clause(
						not( f( tr, t ) ), not( p( tr.from, t - 1 ) ), p( tr.to, t ) ) );
		}

		if ( t > 0 )
		{
			final Set< Transition > blocked = new HashSet<>( pblocked );
			for ( int r = 0; r < ss.length; ++r )
				for ( int c = 0; c < ss.length; ++c )
					if ( State.defaultTransitions[ r ][ c ] == 0 )
						blocked.add( transition( ss[ r ], ss[ c ] ) );

			for ( final Transition tr : blocked )
				solver.addClause( map.clause( b( tr, t ) ) );
//			for ( final Transition tr : allTransitions() )
//				if ( blocked.contains( tr ) )
//					solver.addClause( map.clause( b( tr, t ) ) );
//				else
//					solver.addClause( map.clause( not ( b( tr, t ) ) ) );

			for ( final Transition tr : allTransitions() )
				if ( forced.contains( tr ) )
					solver.addClause( map.clause( f( tr, t ) ) );
				else
					solver.addClause( map.clause( not ( f( tr, t ) ) ) );
		}
	}

	public interface Constraint
	{
		public void add( final ISolver solver, final int t ) throws ContradictionException;
	}

	/**
	 * <em>state(</em>{@code at_t}</em>) ∈ </em>{@code states}.
	 */
	public static class StateSetConstraint implements Constraint
	{
		private final ArrayList< State > states;

		private final int at_t;

		private final VecInt clause;

		/**
		 * <em>state(</em>{@code at_t}</em>) ∈ </em>{@code states}.
		 */
		public StateSetConstraint( final int at_t, final State ... states )
		{
			this( at_t, Arrays.asList( states ) );
		}

		/**
		 * <em>state(</em>{@code at_t}</em>) ∈ </em>{@code states}.
		 */
		public StateSetConstraint( final int at_t, final Collection< State > states )
		{
			this.states = new ArrayList<>( states );
			this.at_t = at_t;
			final ArrayList< Literal > literals = new ArrayList<>();
			for ( final State state : states )
				literals.add( p( state, at_t ) );
			clause = map.clause( literals );
		}

		@Override
		public void add( final ISolver solver, final int t ) throws ContradictionException
		{
//			if ( t == at_t )
				solver.addClause( clause );
		}

		@Override
		public String toString()
		{
			return "! at t=" + at_t + ": s∈" + states;
		}
	}

	/**
	 * <em>state(</em>{@code at_t}</em>) = </em>{@code state}<em> ⇒ block transitions</em>.
	 */
	public static class ConditionalBlockConstraint implements Constraint
	{
		private final State state;

		private final int at_t;

		private final Collection< Transition > block;

		/**
		 * <em>state(</em>{@code at_t}</em>) = </em>{@code state}<em> ⇒ block transitions</em>.
		 */
		public ConditionalBlockConstraint( final State state, final int at_t, final Transition ... block )
		{
			this( state, at_t, Arrays.asList( block ) );
		}

		/**
		 * <em>state(</em>{@code at_t}</em>) = </em>{@code state}<em> ⇒ block transitions</em>.
		 */
		public ConditionalBlockConstraint( final State state, final int at_t, final Collection< Transition > block )
		{
			this.state = state;
			this.at_t = at_t;
			this.block = block;
		}

		@Override
		public void add( final ISolver solver, final int t ) throws ContradictionException
		{
			assert ( t > at_t );
			for ( final Transition tr : block )
				solver.addClause( map.clause(
						not( p( state, at_t ) ), b( tr, t ) ) );
		}

		@Override
		public String toString()
		{
			return "! s(" + at_t + ")=" + state + " ⇒ block " + block;
		}
	}

	static class ProblemState
	{
		ArrayList< Literal > forceVars = new ArrayList<>();

		ArrayList< Constraint > constraints = new ArrayList<>();

		HashSet< Transition > blocked = new HashSet<>();

		HashSet< Transition > forced = new HashSet<>();

		ISolver solver = SolverFactory.newDefault();

		void add( final Constraint c )
		{
			constraints.add( c );
		}

		void block( final Transition tr )
		{
			blocked.add( tr );
		}

		void unblock( final Transition tr )
		{
			blocked.remove( tr );
		}

		void force( final Transition tr )
		{
			forced.add( tr );
		}

		void apply( final int t ) throws ContradictionException
		{
			addDefaultConstraints( solver, t, blocked, forced );
			for ( final Constraint constraint : constraints )
				constraint.add( solver, t );
		}
	}

	static abstract class Program
	{
		protected ArrayList< Program > children;

		protected Program parent = null;

		protected int t = 0;

		protected String name;

		public Program( final String name )
		{
			this.name = name;
			children = new ArrayList<>();
		}

		public Program then( final Program p )
		{
			children.clear();
			children.add( p );
			p.parent = this;
			return p;
		}

		protected void init()
		{
			t = ( parent == null )
					? 0
					: ( parent.t + 1 );
			for ( final Program child : children )
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
			state.apply( t );
			state.forced.clear();
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
					state.solver.addClause( map.clause( p( s, t ) ) );
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

		protected void debugprintPossibleStates()
		{
			try
			{
				final ProblemState state = getState();
				for ( final Constraint constraint : state.constraints )
					System.out.println( "// " + constraint );
			}
			catch ( final ContradictionException e )
			{
				System.err.println( "unsatisfiable state" );
			}
		}

		protected void printPossibleTraces()
		{
			System.out.println( "(" + t + ") " + name + " --> " );
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
			if ( DEBUG || DEBUGAT.contains( t ) )
			{
				debugprintPossibleStates();
				System.out.println();
			}
			for ( final Program child : children )
				child.recursivelyPrintPossibleStates();
		}

		public Program root()
		{
			return ( parent == null )
					? this
					: parent.root();
		}
	}

	static class ValueRef
	{
		int t;

		public ValueRef( final int t )
		{
			this.t = t;
		}

		public ValueRef()
		{
			this.t = -1;
		}

		@Override
		public String toString()
		{
			return "" + t;
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
			state.add( new StateSetConstraint( 0, A, B, C ) );
		}
	}

	static class GetValue extends Program
	{
		private final ValueRef ref;

		public GetValue( final ValueRef ref, final String name )
		{
			super( name );
			this.ref = ref;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.add( new ConditionalBlockConstraint( B, t, transition( B, C ) ) );
		}

		@Override
		protected void init()
		{
			super.init();
			ref.t = t;
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
			state.block( transition( A, B ) );
		}
	}

	static class SetValue extends Program
	{
		private final ValueRef ref;

		public SetValue( final ValueRef ref, final String name )
		{
			super( name );
			this.ref = ref;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.block( transition( B, C ) );
			state.force( transition( A, B ) );
		}

		@Override
		protected void init()
		{
			super.init();
			ref.t = t;
		}
	}

	static class RemoveEntryFromMap extends Program
	{
		public RemoveEntryFromMap( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.force( transition( C, D ) );
		}
	}

	static class Nop extends Program
	{
		public Nop( final String name)
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{}
	}

	static class Branch extends Program
	{
		final ValueRef ref;

		public Branch(
				final Collection< State > ifStates,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			this ( ifStates, null, ifName, ifProg, elseName, elseProg );
		}

		public Branch(
				final Collection< State > ifStates,
				final int at_t,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			this ( ifStates, new ValueRef( at_t ), ifName, ifProg, elseName, elseProg );
		}

		public Branch(
				final Collection< State > ifStates,
				final ValueRef valueRef,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			super( "Branch (this should never be printed)" );
			this.ref = valueRef;

			final ArrayList< State > elseStates = new ArrayList<>( Arrays.asList( State.values() ) );
			elseStates.removeAll( ifStates );

			final Program pTrue = new Program( ifName )
			{
				@Override
				protected void modifyState( final ProblemState state ) throws ContradictionException
				{
					final int at_t = ( ref != null ) ? ref.t : t;
					state.constraints.add( new StateSetConstraint( at_t, ifStates ) );
				}
			}.then( ifProg.root() ).root();
			children.add( pTrue );
			pTrue.parent = this;

			final Program pFalse = new Program( elseName )
			{
				@Override
				protected void modifyState( final ProblemState state ) throws ContradictionException
				{
					final int at_t = ( ref != null ) ? ref.t : t;
					state.constraints.add( new StateSetConstraint( at_t, elseStates ) );
				}
			}.then( elseProg.root() ).root();
			children.add( pFalse );
			pFalse.parent = this;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{}

		@Override
		protected void init()
		{
			t = parent.t;
			for ( final Program child : children )
				child.init();
		}

		@Override
		protected void printPossibleStates()
		{}

		@Override
		protected void debugprintPossibleStates()
		{
			System.out.println( "// ref = " + ref );
//			super.debugprintPossibleStates();
		}

		@Override
		public Program then( final Program p )
		{
			throw new UnsupportedOperationException( "it's difficult enough without this" );
		}
	}

	static final boolean DEBUG = false;

//	static final ArrayList< Integer > DEBUGAT = new ArrayList<>( Arrays.asList( 3, 4 ) );
	static final ArrayList< Integer > DEBUGAT = new ArrayList<>();

	public static void main( final String[] args ) throws ContradictionException, TimeoutException
	{
		final ValueRef v1 = new ValueRef();
		final ValueRef v2 = new ValueRef();
		final ValueRef v3 = new ValueRef();

		final Program p =
		        new Init(                  "entry = computeIfAbsent();            " )
		 .then( new GetValue( v1,          "v = entry.getValue();                 " ) )
		 .then( new Branch( Arrays.asList( A, C, D, E ), v1,
		                                   "if ( v == null ) {                    ",
		            new Lock(              "    synchronized ( entry )            " )
		     .then( new Branch( Arrays.asList( B, C, D, E ),
		                                   "    if ( entry.loaded ) {             ",
		                new GetValue( v2,  "        v = entry.getValue();         " )
		         .then( new Branch( Arrays.asList( A, C, D, E ), v2,
		                                   "        if ( v == null ) {            ",
		                    new RemoveEntryFromMap(
		                                   "            map.remove( key, entry ); " )
		             .then( new Nop(       "            return get( key, loader );" ) ),
		                                   "        } else {                      ",
		                    new Nop(       "            return v;                 " )
		                ) ),
		                                   "    } else {                          ",
		                new Nop(           "        v = loader.call();            " )
		         .then( new SetValue( v3,  "        entry.setValue( v );          " ) )
		         .then( new Nop(           "        return v;                     " ) )
		            ) ),
		                                   "} else {                              ",
		        new Nop(                   "    return v;                         " ) ) )
		 .root();

		p.recursivelyPrintPossibleStates();

//		System.out.println();
//		System.out.println( "traces:" );
//		p.children.get( 0 ).children.get( 0 ).children.get( 0 ).children.get( 0 ).children.get( 0 ).printPossibleTraces();






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
