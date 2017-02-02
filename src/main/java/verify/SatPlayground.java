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
import static verify.State.F;
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

		protected void printPossibleStates( final int padlen )
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
			final String padded = String.format("%1$-" + padlen + "s", name );
			System.out.println( "(" + t + ") " + padded + " : " + possible );
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
			recursivelyPrintPossibleStates( maxNameLength() );
		}

		protected void recursivelyPrintPossibleStates( final int padlen )
		{
			init();
			printPossibleStates( padlen );
			if ( DEBUG || DEBUGAT.contains( t ) )
			{
				debugprintPossibleStates();
				System.out.println();
			}
			for ( final Program child : children )
				child.recursivelyPrintPossibleStates( padlen );
		}

		public Program root()
		{
			return ( parent == null )
					? this
					: parent.root();
		}

		private int maxNameLength()
		{
			int maxNameLength = name.length();
			for ( final Program child : children )
				maxNameLength = Math.max( maxNameLength, child.maxNameLength() );
			return maxNameLength;
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

	static class BranchCondition
	{
		final ValueRef v;

		final Collection< State > states;

		public BranchCondition(
				final ValueRef v,
				final Collection< State > states )
		{
			this.v = v;
			this.states = states;
		}
	}

	static class IfSeq
	{
		final String name;

		final Program program;

		public IfSeq( final String name, final Program program )
		{
			this.name = name;
			this.program = program;
		}
	}

	static class ElseSeq
	{
		final String name;

		final Program program;

		public ElseSeq( final String name, final Program program )
		{
			this.name = name;
			this.program = program;
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
			this ( new BranchCondition( null, ifStates ), ifName, ifProg, elseName, elseProg );
		}

		public Branch(
				final Collection< State > ifStates,
				final int at_t,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			this ( new BranchCondition( new ValueRef( at_t ), ifStates ), ifName, ifProg, elseName, elseProg );
		}

		public Branch(
				final Collection< State > ifStates,
				final ValueRef valueRef,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			this( new BranchCondition( valueRef, ifStates ), ifName, ifProg, elseName, elseProg );
		}

		public Branch(
				final BranchCondition condition,
				final String ifName,
				final Program ifProg,
				final String elseName,
				final Program elseProg )
		{
			super( "(--branch--)" );
			this.ref = condition.v;

			final Collection< State > ifStates = condition.states;
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
		protected void printPossibleStates( final int padlen )
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



// ====================================================================================================================
// ====================================================================================================================
// ====================================================================================================================
// ====================================================================================================================
// ====================================================================================================================

	/**
	 *
	 */
	static class Init extends Program
	{
		public Init( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.add( new StateSetConstraint( 0, A, B, C, D ) );
		}
	}

	/**
	 *
	 */
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
			state.add( new ConditionalBlockConstraint( B, t, transition( B, D ) ) );
			state.add( new ConditionalBlockConstraint( C, t, transition( C, D ) ) );
		}

		@Override
		protected void init()
		{
			super.init();
			ref.t = t;
		}
	}

	/**
	 *
	 */
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
			state.block( transition( A, C ) );
			state.block( transition( B, C ) );
		}
	}

	/**
	 *
	 */
	static class SetInvalid extends Program
	{
		private final ValueRef ref;

		public SetInvalid( final ValueRef ref, final String name )
		{
			super( name );
			this.ref = ref;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.force( transition( A, B ) );

			state.block( transition( B, D ) );
		}

		@Override
		protected void init()
		{
			super.init();
			ref.t = t;
		}
	}

	/**
	 *
	 */
	static class SetValid extends Program
	{
		private final ValueRef ref;

		public SetValid( final ValueRef ref, final String name )
		{
			super( name );
			this.ref = ref;
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.force( transition( A, C ) );
			state.force( transition( B, C ) );

			state.block( transition( C, D ) );
		}

		@Override
		protected void init()
		{
			super.init();
			ref.t = t;
		}
	}

	/**
	 *
	 */
	static class RemoveEntryFromMap extends Program
	{
		public RemoveEntryFromMap( final String name )
		{
			super( name );
		}

		@Override
		protected void modifyState( final ProblemState state ) throws ContradictionException
		{
			state.force( transition( D, E ) );
		}
	}



	public static Program nop( final String name )
	{
		return new Nop( name );
	}

	public static Program init( final String name )
	{
		return new Init( name );
	}

	public static Program getv( final String name, final ValueRef v )
	{
		return new GetValue( v, name );
	}

	public static Program seti( final String name, final ValueRef v )
	{
		return new SetInvalid( v, name );
	}

	public static Program setv( final String name, final ValueRef v )
	{
		return new SetValid( v, name );
	}

	public static Program lock( final String name )
	{
		return new Lock( name );
	}

	public static Program remove( final String name )
	{
		return new RemoveEntryFromMap( name );
	}

	public static Program branch( final BranchCondition c, final IfSeq ifseq, final ElseSeq elseseq )
	{
		return new Branch( c, ifseq.name, ifseq.program, elseseq.name, elseseq.program );
	}

	public static BranchCondition cond( final ValueRef v, final State ... states )
	{
		return new BranchCondition( v, Arrays.asList( states ) );
	}

	public static BranchCondition cond( final State ... states )
	{
		return new BranchCondition( null, Arrays.asList( states ) );
	}

	public static IfSeq ifseq( final String name, final Program ... steps )
	{
		return new IfSeq( name, seq( steps ) );
	}

	public static ElseSeq elseseq( final String name, final Program ... steps )
	{
		return new ElseSeq( name, seq( steps ) );
	}

	public static Program seq( final Program ... steps )
	{
		if ( steps.length == 0 )
		{
			return nop( "empty" );
		}
		else
		{
			for ( int i = 1; i < steps.length; ++i )
				steps[ i - 1 ].then( steps[ i ] );
			return steps[ 0 ];
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
		final ValueRef v4 = new ValueRef();

		System.out.println( "CacheHints == VOLATILE" );
		System.out.println( "======================" );
		Program p = seq
		(
			init(                                "entry = computeIfAbsent();" ),
			getv(                                "v = entry.getValue();", v1 ),
			branch( cond( v1, A, D, E, F ),
				ifseq(                           "if ( v == null ) {",
					lock(                        "    synchronized ( entry ) {" ),
					branch( cond( A ),
						ifseq(                   "        if ( loaded == NOTLOADED ) {",
							nop(                 "            v = loader.getInvalid();" ),
							seti(                "            entry.setInvalid( v );", v2 ),
							nop(                 "            enqueue();" ),
							nop(                 "            return v;" )
						),
						elseseq(                 "        } else {",
							branch( cond( B ),
								ifseq(           "            if ( loaded == INVALID ) {",
									getv(        "                v = entry.getValue();", v3 ),
									branch( cond( v3, B, C ),
										ifseq(   "                if ( v != null ) { ",
											nop( "                    enqueue();" ),
											nop( "                    return v;" )
										),
										elseseq( "                } else { // v == null ",
											remove( "                    map.remove( key, entry );" ),
											nop( "                    return get( key, loader, hints );" )
										)
									)
								),
								elseseq(         "            } else { // loaded == VALID",
									getv(        "                v = entry.getValue();", v4 ),
									branch( cond( v4, B, C ),
										ifseq(   "                if ( v != null ) { ",
											nop( "                     return v;")
										),
										elseseq( "                } else { // v == null ",
											remove( "                    map.remove( key, entry );" ),
											nop( "                    return get( key, loader, hints );" )
										)
									)
								)
							)
						)
					)
				),
				elseseq(           "} else { // v != null ",
					nop(           "    if ( !v.isValid() ) { enqueue(); } " ),
					nop(           "    return v; // strong ref (1)" )
				)
			)
		);
		p.recursivelyPrintPossibleStates();

		System.out.println();
		System.out.println();

		System.out.println( "CacheHints == DONTLOAD" );
		System.out.println( "======================" );
		p = seq
		(
			init(                                "entry = computeIfAbsent();" ),
			getv(                                "v = entry.getValue();", v1 ),
			branch( cond( v1, A, D, E, F ),
				ifseq(                           "if ( v == null ) {",
					lock(                        "    synchronized ( entry ) {" ),
					branch( cond( A ),
						ifseq(                   "        if ( loaded == NOTLOADED ) {",
							nop(                 "            v = loader.getInvalid();" ),
							seti(                "            entry.setInvalid( v );", v2 ),
							nop(                 "            return v;" )
						),
						elseseq(                 "        } else { // loaded == INVALID || VALID ",
							getv(        "                v = entry.getValue();", v4 ),
							branch( cond( v4, B, C ),
								ifseq(   "                if ( v != null ) { ",
									nop( "                     return v;")
								),
								elseseq( "                } else { // v == null ",
									remove( "                    map.remove( key, entry );" ),
									nop( "                    return get( key, loader, hints );" )
								)
							)
						)
					)
				),
				elseseq(           "} else { // v != null ",
					nop(           "    return v; // strong ref (1)" )
				)
			)
		);
		p.recursivelyPrintPossibleStates();

		System.out.println();
		System.out.println();

		System.out.println( "CacheHints == BUDGETED" );
		System.out.println( "======================" );


		System.out.println();
		System.out.println();

		System.out.println( "CacheHints == BLOCKING" );
		System.out.println( "======================" );
		p = seq
		(
			init(                                "entry = computeIfAbsent();" ),
			getv(                                "v = entry.getValue();", v1 ),
			branch( cond( v1, A, D, E, F ),
				ifseq(                           "if ( v == null ) {",
					lock(                        "    synchronized ( entry ) {" ),
					branch( cond( A ),
						ifseq(                   "        if ( loaded == NOTLOADED ) {",
							nop(                 "            v = loader.getInvalid();" ),
							seti(                "            entry.setInvalid( v );", v2 ),
							nop(                 "            return v;" )
						),
						elseseq(                 "        } else { // loaded == INVALID || VALID ",
							getv(        "                v = entry.getValue();", v4 ),
							branch( cond( v4, B, C ),
								ifseq(   "                if ( v != null ) { ",
									nop( "                     return v;")
								),
								elseseq( "                } else { // v == null ",
									remove( "                    map.remove( key, entry );" ),
									nop( "                    return get( key, loader, hints );" )
								)
							)
						)
					)
				),
				elseseq(           "} else { // v != null ",
					nop(           "    return v; // strong ref (1)" )
				)
			)
		);
		p.recursivelyPrintPossibleStates();

	}
}
