package bdv.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.Assert;
import org.junit.Test;

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;
import static bdv.viewer.Interpolation.NLINEAR;
import static bdv.viewer.ViewerStateChange.CURRENT_GROUP_CHANGED;
import static bdv.viewer.ViewerStateChange.CURRENT_SOURCE_CHANGED;
import static bdv.viewer.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.ViewerStateChange.INTERPOLATION_CHANGED;
import static bdv.viewer.ViewerStateChange.NUM_GROUPS_CHANGED;
import static bdv.viewer.ViewerStateChange.NUM_SOURCES_CHANGED;
import static bdv.viewer.ViewerStateChange.VISIBILITY_CHANGED;

public class BasicViewerStateTest
{
	@Test
	public void addRemoveSource()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceAndConverter< ? > s = createSource();

		state.addSource( s );
		Assert.assertEquals( state.getSources(), Collections.singletonList( s ) );

		state.removeSource( s );
		Assert.assertEquals( state.getSources(), Collections.emptyList() );
	}

	@Test
	public void addRemoveGroup()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceGroup g = new SourceGroup();

		state.addGroup( g );
		Assert.assertEquals( state.getGroups(), Collections.singletonList( g ) );

		state.removeGroup( g );
		Assert.assertEquals( state.getGroups(), Collections.emptyList() );
	}

	@Test
	public void setGroupName()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceGroup g = new SourceGroup();
		state.addGroup( g );

		final String a_group_name = "a group name";
		state.setGroupName( g, a_group_name );
		Assert.assertEquals( state.getGroupName( g ), a_group_name );
	}

	@Test
	public void addSourceToGroup()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceGroup g = new SourceGroup();
		state.addGroup( g );
		final SourceAndConverter< ? > s = createSource();
		state.addSource( s );

		state.addSourceToGroup( s, g );
		Assert.assertEquals( state.getSourcesInGroup( g ), Collections.singleton( s ) );

		state.removeSourceFromGroup( s, g );
		Assert.assertEquals( state.getSourcesInGroup( g ), Collections.emptySet() );

		state.addSourceToGroup( s, g );
		Assert.assertEquals( state.getSourcesInGroup( g ), Collections.singleton( s ) );

		state.removeSourceFromGroup( s, g );
		Assert.assertEquals( state.getSourcesInGroup( g ), Collections.emptySet() );
	}

	@Test
	public void sourceOrderComparator()
	{
		final BasicViewerState state = new BasicViewerState();
		final List< SourceAndConverter< ? > > expected = new ArrayList<>();
		for ( int i = 0; i < 10; ++i )
		{
			final SourceAndConverter< ? > source = createSource();
			state.addSource( source );
			expected.add( source );
		}
		final List< SourceAndConverter< ? > > actual = new ArrayList<>( expected );
		Collections.shuffle( actual );

		actual.sort( state.sourceOrder() );

		Assert.assertEquals( expected, actual );
	}

	@Test
	public void sourceIndices()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceAndConverter< ? > s0 = createSource();
		final SourceAndConverter< ? > s1 = createSource();
		final SourceAndConverter< ? > s2 = createSource();

		state.addSource( s0 );
		state.addSource( s1 );
		state.addSource( s2 );
		state.removeSource( s1 );

		Assert.assertEquals( state.getSources().indexOf( s0 ), 0 );
		Assert.assertEquals( state.getSources().indexOf( s2 ), 1 );
		Assert.assertFalse( state.containsSource( s1 ) );
	}

	@Test
	public void groupIndices()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceGroup g0 = new SourceGroup();
		final SourceGroup g1 = new SourceGroup();
		final SourceGroup g2 = new SourceGroup();

		state.addGroup( g0 );
		state.addGroup( g1 );
		state.addGroup( g2 );
		state.removeGroup( g1 );

		Assert.assertEquals( state.getGroups().indexOf( g0 ), 0 );
		Assert.assertEquals( state.getGroups().indexOf( g2 ), 1 );
		Assert.assertFalse( state.containsGroup( g1 ) );
	}

	@Test
	public void addSourceEvents()
	{
		final BasicViewerState state = new BasicViewerState();

		final ReceiveEvents r = new ReceiveEvents( NUM_SOURCES_CHANGED, CURRENT_SOURCE_CHANGED, VISIBILITY_CHANGED );
		state.changeListeners().add( r );

		state.addSource( createSource() );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	@Test
	public void removeSourceEvents()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceAndConverter< ? > s0 = createSource();
		state.addSource( s0 );

		final ReceiveEvents r = new ReceiveEvents( NUM_SOURCES_CHANGED, CURRENT_SOURCE_CHANGED, VISIBILITY_CHANGED );
		state.changeListeners().add( r );

		state.removeSource( s0 );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	@Test
	public void addGroupEvents()
	{
		final BasicViewerState state = new BasicViewerState();
		state.addSource( createSource() );

		final ReceiveEvents r = new ReceiveEvents( NUM_GROUPS_CHANGED, CURRENT_GROUP_CHANGED );
		state.changeListeners().add( r );

		state.addGroup( new SourceGroup() );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	@Test
	public void interpolationEvents1()
	{
		final BasicViewerState state = new BasicViewerState();
		state.setInterpolation( NEARESTNEIGHBOR );

		final ReceiveEvents r = new ReceiveEvents( INTERPOLATION_CHANGED );
		state.changeListeners().add( r );

		state.setInterpolation( NLINEAR );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	@Test
	public void displayModeEvents1()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceAndConverter< ? > s0 = createSource();
		final SourceAndConverter< ? > s1 = createSource();
		state.addSource( s0 );
		state.addSource( s1 );
		state.setSourceActive( s0, true );
		state.setSourceActive( s1, false );
		state.setDisplayMode( SINGLE );

		final ReceiveEvents r = new ReceiveEvents( DISPLAY_MODE_CHANGED );
		state.changeListeners().add( r );

		state.setDisplayMode( FUSED );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	@Test
	public void displayModeEvents2()
	{
		final BasicViewerState state = new BasicViewerState();
		final SourceAndConverter< ? > s0 = createSource();
		final SourceAndConverter< ? > s1 = createSource();
		state.addSource( s0 );
		state.addSource( s1 );
		state.setSourceActive( s0, true );
		state.setSourceActive( s1, true );
		state.setDisplayMode( SINGLE );

		final ReceiveEvents r = new ReceiveEvents( DISPLAY_MODE_CHANGED, VISIBILITY_CHANGED );
		state.changeListeners().add( r );

		state.setDisplayMode( FUSED );

		Assert.assertTrue( r.allReceivedExclusively() );
	}

	// -- helpers --

	static class ReceiveEvents implements ViewerStateChangeListener
	{
		private final ViewerStateChange[] changes;

		private final List< ViewerStateChange > received = new ArrayList<>();

		public ReceiveEvents( final ViewerStateChange... changes )
		{
			this.changes = changes;
		}

		@Override
		public void viewerStateChanged( final ViewerStateChange change )
		{
			received.add( change );
		}

		/**
		 * Check whether all of expected events were received at least once
		 */
		public boolean allReceived()
		{
			final Set< ViewerStateChange > expected = new HashSet<>();
			expected.addAll( Arrays.asList( changes ) );
			expected.removeAll( received );
			return expected.isEmpty();
		}

		/**
		 * Check whether all of expected events were received at least once, and no other event was received
		 */
		public boolean allReceivedExclusively()
		{
			final Set< ViewerStateChange > expected = new HashSet<>();
			expected.addAll( Arrays.asList( changes ) );
			expected.removeAll( received );
			final Set< ViewerStateChange > additional = new HashSet<>();
			additional.addAll( received );
			additional.removeAll( Arrays.asList( changes ) );
			final boolean success = expected.isEmpty() && additional.isEmpty();
			if ( !success )
			{
				if ( !expected.isEmpty() )
					System.out.println( "expected, but not received: " + expected );
				if ( !additional.isEmpty() )
					System.out.println( "received, but not expected: " + additional );
			}
			return success;
		}
	}

	static class TestSource implements Source< Void >
	{
		private static final AtomicInteger id = new AtomicInteger();

		private static String nextName()
		{
			return "source(" + id.getAndIncrement() + ")";
		}

		private final String name;

		private final IntPredicate isPresent;

		public TestSource()
		{
			this( nextName(), i -> false );
		}

		public TestSource( final String name )
		{
			this( name, i -> false );
		}

		public TestSource( final IntPredicate isPresent )
		{
			this( nextName(), isPresent );
		}

		public TestSource( final String name, final IntPredicate isPresent )
		{
			this.name = name;
			this.isPresent = isPresent;
		}

		@Override
		public Void getType()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public VoxelDimensions getVoxelDimensions()
		{
			return null;
		}

		@Override
		public int getNumMipmapLevels()
		{
			return 1;
		}

		@Override
		public boolean isPresent( final int t )
		{
			return isPresent.test( t );
		}

		@Override
		public RandomAccessibleInterval< Void > getSource( final int t, final int level )
		{
			return null;
		}

		@Override
		public RealRandomAccessible< Void > getInterpolatedSource( final int t, final int level, final Interpolation method )
		{
			return null;
		}

		@Override
		public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
		{
			transform.identity();
		}
	}

	static SourceAndConverter< ? > createSource()
	{
		return new SourceAndConverter<>( new TestSource(), null );
	}
}
