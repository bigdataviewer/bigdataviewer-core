package verify;

import static verify.Literal.fvar;
import static verify.Literal.not;
import static verify.Literal.var;
//import static verify.SatPlayground.State.*;
import static verify.SatPlayground.State.A;
import static verify.SatPlayground.State.B;
import static verify.SatPlayground.State.C;
import static verify.SatPlayground.State.D;
import static verify.SatPlayground.State.E;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

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

		for( final State s : ss )
		{
			solver.addClause( map.clause(
					not( fvar( s, t ) ),
					var( s, t ) ) );
		}
	}

	public static void addForcedTransitionConstraints( final ISolver solver, final int t ) throws ContradictionException
	{
		addForcedTransitionConstraints( solver, t, null );
	}

	public static void addForcedTransitionConstraints( final ISolver solver, final int t, final State state ) throws ContradictionException
	{
		final State[] ss = State.values();
		for( final State s : ss )
		{
			if ( s.equals( state ) )
				solver.addClause( map.clause( fvar ( s, t ) ) );
			else
				solver.addClause( map.clause( not( fvar ( s, t ) ) ) );
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
				literals.add( var( state, at_t ) );
			clause = map.clause( literals );
		}

		@Override
		public void add( final ISolver solver, final int t ) throws ContradictionException
		{
			solver.addClause( clause );
		}

		@Override
		public String toString()
		{
			return "! at t=" + at_t + ": s∈" + states;
		}
	}

	/**
	 * <em>state(</em>{@code from_t}</em>) = </em>{@code state}<em> ⇒ state(t) = </em>{@code state}<em>, for t > </em>{@code from_t}.
	 */
	public static class HoldStateConstraint implements Constraint
	{
		private final State state;

		private final int from_t;

		/**
		 * <em>state(</em>{@code from_t}</em>) = </em>{@code state}<em> ⇒ state(t) = </em>{@code state}<em>, for t > </em>{@code from_t}.
		 */
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

		@Override
		public String toString()
		{
			return "! s(" + from_t + ")=" + state + " ⇒ s(t)=" + state;
		}
	}

	public static class TransitionConstraint implements Constraint
	{
		private final State from;

		private final State to;

		private final int to_t;

		public TransitionConstraint( final State from, final State to, final int to_t )
		{
			this.from = from;
			this.to = to;
			this.to_t = to_t;
		}

		@Override
		public void add( final ISolver solver, final int t ) throws ContradictionException
		{
			solver.addClause( map.clause(
					not( var( from, to_t - 1 ) ),
					var( to, to_t ) ) );
		}

		@Override
		public String toString()
		{
			return "! " + from + "(" + (to_t - 1) + ") ⇒ " + to + "(" + to_t + ")";
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
//			addForcedTransitionConstraints( solver, t );
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

	static class Init extends Program
	{
		final ArrayList< Constraint > initialConstraints;

		public Init( final String name )
		{
			super( name );
			initialConstraints = new ArrayList<>();
			initialConstraints.add( new StateSetConstraint( 0, A, B, C ) );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.constraints.addAll( initialConstraints );
			state.applyConstraints( t );
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
			state.constraints.add( new HoldStateConstraint( B, t ) );
			state.applyConstraints( t );
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
			state.constraints.add( new HoldStateConstraint( A, t ) );
			state.applyConstraints( t );
		}
	}

	static class Transition extends Program
	{
		private final Collection< Pair< State, State > > arrows;

		public Transition( final Collection< Pair< State, State > > arrows, final String name )
		{
			super( name );
			this.arrows = arrows;
		}

		public Transition( final State from, final State to, final String name )
		{
			this( Arrays.asList( new ValuePair<>( from, to ) ), name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			for ( final Pair< State, State > arrow : arrows )
				state.constraints.add( new TransitionConstraint( arrow.getA(), arrow.getB(), t ) );
			state.applyConstraints( t );
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
		{
			state.applyConstraints( t );
		}
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

			final Program pTrue = new Program( ifName )
			{
				@Override
				protected void modifyState( final ProblemState state ) throws ContradictionException
				{
					final int at_t = ( ref != null ) ? ref.t : t;
					state.constraints.add( new StateSetConstraint( at_t, ifStates ) );
					state.applyConstraints( t );
				}
			}.then( ifProg.root() ).root();
			children.add( pTrue );
			pTrue.parent = this;

			final ArrayList< State > elseStates = new ArrayList<>( Arrays.asList( State.values() ) );
			elseStates.removeAll( ifStates );
			final Program pFalse = new Program( elseName )
			{
				@Override
				protected void modifyState( final ProblemState state ) throws ContradictionException
				{
					final int at_t = ( ref != null ) ? ref.t : t;
					state.constraints.add( new StateSetConstraint( at_t, elseStates ) );
					state.applyConstraints( t );
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

	static final ArrayList< Integer > DEBUGAT = new ArrayList<>( Arrays.asList( 3, 4 ) );

	public static void main( final String[] args ) throws ContradictionException, TimeoutException
	{
		final ValueRef v1 = new ValueRef();
		final ValueRef v2 = new ValueRef();
//		final Program p =
//		        new Init(            "entry = computeIfAbsent(); " )
//		 .then( new GetValue( v,     "v = entry.getValue();      " ) )
//		 .then( new Lock(            "synchronized( entry ) {    " ) )
//		 .then( new Branch( Arrays.asList( B ), 1,
//		                             "if ( v != null ) {         ",
//		        new Nop(             "    nop if                 " ),
//		                             "} else {                   ",
//		        new Nop(             "    nop else               " ) ) )
//		 .root();

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
		                    new Transition( Arrays.asList(
		                    		new ValuePair<>( A, D ),
		                    		new ValuePair<>( B, D ),
		                    		new ValuePair<>( C, D ) ),
		                                   "            map.remove( key, entry ); " )
		             .then( new Nop(       "            return get( key, loader );" ) ),
		                                   "        } else {                      ",
		                    new Nop(       "            return v;                 " )
		                ) ),
		                                   "    } else {                          ",
		                new Nop(           "        v = loader.call();            " )
		         .then( new Transition( A, B,
		                                   "        entry.setValue( v );          " ) )
//		         .then( new Nop(           "        entry.setValue( v );          " ) )
		         .then( new Nop(           "        return v;                     " ) )
		            ) ),
		                                   "} else {                              ",
		        new Nop(                   "    nop else                          " ) ) )
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
