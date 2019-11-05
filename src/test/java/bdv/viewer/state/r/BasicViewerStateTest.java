package bdv.viewer.state.r;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.Assert;
import org.junit.Test;

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

	// -- helpers --

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
