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

public class ViewerStateTest
{
	@Test
	public void addRemoveSource()
	{
		final DefaultViewerState state = new DefaultViewerState();
		final SourceAndConverter< ? > s = createSource();

		state.getSources().add( s );
		Assert.assertEquals( state.getSources(), Collections.singletonList( s ) );

		state.getSources().remove( s );
		Assert.assertEquals( state.getSources(), Collections.emptyList() );
	}

	@Test
	public void addRemoveGroup()
	{
		final DefaultViewerState state = new DefaultViewerState();
		final SourceGroup g = new SourceGroup();

		state.getGroups().add( g );
		Assert.assertEquals( state.getGroups(), Collections.singletonList( g ) );

		state.getGroups().remove( g );
		Assert.assertEquals( state.getGroups(), Collections.emptyList() );
	}

	@Test
	public void setGroupName()
	{
		final DefaultViewerState state = new DefaultViewerState();
		final SourceGroup g = new SourceGroup();
		state.getGroups().add( g );

		final String a_group_name = "a group name";
		state.getGroups().setName( g, a_group_name );
		Assert.assertEquals( state.getGroups().getName( g ), a_group_name );
	}

	@Test
	public void addSourceToGroup()
	{
		final DefaultViewerState state = new DefaultViewerState();
		final SourceGroup g = new SourceGroup();
		state.getGroups().add( g );
		final SourceAndConverter< ? > s = createSource();
		state.getSources().add( s );

		state.getGroups().addSourceToGroup( s, g );
		Assert.assertEquals( state.getGroups().getSourcesIn( g ), Collections.singleton( s ) );

		state.getGroups().removeSourceFromGroup( s, g );
		Assert.assertEquals( state.getGroups().getSourcesIn( g ), Collections.emptySet() );

		state.getGroups().getSourcesIn( g ).add( s );
		Assert.assertEquals( state.getGroups().getSourcesIn( g ), Collections.singleton( s ) );

		state.getGroups().getSourcesIn( g ).remove( s );
		Assert.assertEquals( state.getGroups().getSourcesIn( g ), Collections.emptySet() );
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
