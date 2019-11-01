package bdv.viewer.state.revised;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.imglib2.realtransform.AffineTransform3D;

public class PlaygroundReadOnlyClean
{
	public static class SourceGroupHandle
	{
	}

	/**
	 * Snapshot of the state of a source group.
	 * Read-Only.
	 * Has a SourceGroupHandle that allows to identify the group across snapshots.
	 */
	public interface SourceGroup
	{
		String getName();

		/**
		 * Get the sources in this group.
		 *
		 * @return unmodifiable set of sources.
		 */
		Set< SourceAndConverter< ? > > getSources();

		/**
		 * Used internally.
		 * The handle is used to identify corresponding source groups across viewer state snapshots.
		 */
		SourceGroupHandle getSourceGroupHandle();

		boolean isActive();

		boolean isCurrent();
	}

	interface SourceGroups extends List< SourceGroup >
	{
		SourceGroup get( SourceGroupHandle handle );

		/**
		 * Get the current SourceGroup.
		 *
		 * @return the current SourceGroup, or {@code null} if this list doesn't contain the current SourceGroup
		 */
		SourceGroup getCurrent();

		List< SourceGroup > getMatching( Predicate< SourceGroup > condition );

//		/**
//		 * @return active SourceGroups in this list
//		 */
//		SourceGroups getActive();
//
//		/**
//		 * @return SourceGroups in this list containing source
//		 */
//		SourceGroups getContaining( SourceAndConverter< ? > source );

//		boolean isActive( SourceGroupHandle handle );
//		boolean isCurrent( SourceGroupHandle handle );
	}

	interface Sources extends List< SourceAndConverter< ? > >
	{
		/**
		 * @return the current source, or {@code null} if this list doesn't contain the current source
		 */
		SourceAndConverter< ? > getCurrent();

		/**
		 * @return active sources
		 */
		Sources getActive();

		/**
		 * @return visible sources
		 */
		Sources getVisible();

		boolean isActive( SourceAndConverter< ? > source );

		boolean isCurrent( SourceAndConverter< ? > source );

		boolean isVisible( SourceAndConverter< ? > source );
	}

	public interface ViewerStateNew
	{
		default AffineTransform3D getViewerTransform()
		{
			final AffineTransform3D t = new AffineTransform3D();
			getViewerTransform( t );
			return t;
		}

		void getViewerTransform( AffineTransform3D t );

		Interpolation getInterpolation();

		DisplayMode getDisplayMode();

		int getNumTimepoints();

		int getCurrentTimepoint();

		// --- SourceGroups ----------------------------------------

		SourceGroups getGroups();

		// --- Sources ---------------------------------------------

		Sources getSources();

//		Comparator< SourceAndConverter< ? > > sourceOrder();
//		Comparator< SourceGroup > groupOrder();
	}


	public static class DefaultViewerState implements ViewerStateNew
	{
		/**
		 * The current number of available timepoints.
		 */
		private int numTimepoints;

		/**
		 * which timepoint is currently shown.
		 */
		private int currentTimepoint;

		/**
		 * Transformation set by the interactive viewer. Transforms from global
		 * coordinate system to viewer coordinate system.
		 */
		private final AffineTransform3D viewerTransform;

		/**
		 * Which interpolation method is currently used to render the display.
		 */
		private Interpolation interpolation;

		/**
		 * The current {@code DisplayMode}.
		 *
		 * In {@code DisplayMode.SINGLE} only the current source is shown. TODO ref to getting current source
		 * In {@code DisplayMode.GROUP} the sources in the current group are blended. TODO ref to getting current group
		 * In {@code DisplayMode.FUSED} all active sources are blended. TODO ref to getting active sources
		 * In {@code DisplayMode.FUSEDROUP} the sources in all active groups are blended. TODO ref to getting active groups
		 *
		 * The currently visible sources can also be obtained through TODO.
		 *
		 * TODO move javadoc to getter
		 */
		private DisplayMode displayMode;

		public DefaultViewerState()
		{
			numTimepoints = 0;
			currentTimepoint = 0;
			viewerTransform = new AffineTransform3D();
			interpolation = Interpolation.NEARESTNEIGHBOR;
			displayMode = DisplayMode.SINGLE;
		}

		@Override
		public void getViewerTransform( final AffineTransform3D t )
		{
			t.set( viewerTransform );
		}

		@Override
		public Interpolation getInterpolation()
		{
			return interpolation;
		}

		@Override
		public DisplayMode getDisplayMode()
		{
			return displayMode;
		}

		@Override
		public int getNumTimepoints()
		{
			return numTimepoints;
		}

		@Override
		public int getCurrentTimepoint()
		{
			return currentTimepoint;
		}

		/**
		 * Returned value is a valid (at least) as long as state is not modified.
		 * If state is modified, it might become invalid or reflect an older snapshot.
		 */
		@Override
		public SourceGroups getGroups()
		{
			return null;
		}

		/**
		 * Returned value is a valid (at least) as long as state is not modified.
		 * If state is modified, it might become invalid or reflect an older snapshot.
		 */
		@Override
		public Sources getSources()
		{
			return null;
		}

		// --- methods to modify state ---

		private final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();

		private SourceAndConverter< ? > currentSource;

		public void addSource( final SourceAndConverter< ? > source )
		{
			if ( source == null || sources.contains( source ) )
				throw new IllegalArgumentException();

			sources.add( source );
			if ( currentSource == null )
				currentSource = source;
		}

		public void removeSource( final SourceAndConverter< ? > source )
		{
			if ( source == null )
				return;

			if ( sources.remove( source ) )
			{
				if ( currentSource.equals( source ) )
					currentSource = sources.isEmpty() ? null : sources.get( 0 );

				// TODO remove from all groups
			}
		}

		// --- groups ---

		private final ArrayList< DefaultSourceGroup > groups = new ArrayList<>();

		private final Map< SourceGroupHandle, DefaultSourceGroup > handleToGroup = new HashMap<>();

		private final Set< SourceGroupHandle > activeGroups = new HashSet<>();

		private SourceGroupHandle currentGroup = null;




		List< SourceAndConverter< ? > > getVisibleSources()
		{
			List< SourceAndConverter< ? > > result = new ArrayList<>();
			switch ( displayMode )
			{
			case SINGLE:
				if ( currentSource != null )
					result.add( currentSource );
				break;
			case GROUP:
				break;
			case FUSED:
				if ( currentGroup != null )
				{
					final Set< SourceAndConverter< ? > > visible = handleToGroup.get( currentGroup ).getSources();
					for ( SourceAndConverter< ? > source : sources )
						if ( visible.contains( source ) )
							result.add( source );
				}
				break;
			case FUSEDGROUP:
				final Set< SourceAndConverter< ? > > visible = new HashSet<>();
				for ( SourceGroupHandle group : activeGroups )
					visible.addAll( handleToGroup.get( group ).getSources() );
				for ( SourceAndConverter< ? > source : sources )
					if ( visible.contains( source ) )
						result.add( source );
				break;
			}
			return result;
		}


	}

	static class DefaultSourceGroups extends WrappedList< SourceGroup > implements SourceGroups
	{
		private final DefaultViewerState state;

		DefaultSourceGroups( final DefaultViewerState state )
		{
			super( Collections.unmodifiableList( state.groups ) );
			this.state = state;
		}

		@Override
		public SourceGroup get( final SourceGroupHandle handle )
		{
			return state.handleToGroup.get( handle );
		}

		@Override
		public SourceGroup getCurrent()
		{
			return state.handleToGroup.get( state.currentGroup );
		}

		@Override
		public List< SourceGroup > getMatching( Predicate< SourceGroup > condition )
		{
			List< SourceGroup > result = new ArrayList<>();
			for ( SourceGroup group : state.groups )
				if ( condition.test( group ) )
					result.add( group );
			return result;
		}
	}

	static class DefaultSourceGroup implements SourceGroup
	{
		private final DefaultViewerState state;

		private final SourceGroupHandle handle;

		private final Set< SourceAndConverter< ? > > sources;

		private final Set< SourceAndConverter< ? > > unmodifiableSources;

		private String name;

		DefaultSourceGroup( final DefaultViewerState state, final SourceGroupHandle handle, final String name )
		{
			this.state = state;
			this.handle = handle;
			this.name = name;
			sources = new HashSet<>();
			unmodifiableSources = Collections.unmodifiableSet( sources );
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public Set< SourceAndConverter< ? > > getSources()
		{
			return unmodifiableSources;
		}

		@Override
		public SourceGroupHandle getSourceGroupHandle()
		{
			return handle;
		}

		@Override
		public boolean isActive()
		{
			return false;
//			return state.isGroupActive( handle );
		}

		@Override
		public boolean isCurrent()
		{
			return false;
//			return state.isGroupCurrent( handle );
		}
	}


}
