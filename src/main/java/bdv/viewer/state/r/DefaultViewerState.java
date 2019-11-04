package bdv.viewer.state.r;

import bdv.util.Affine3DHelpers;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

import static bdv.viewer.state.r.ViewerStateChange.CURRENT_GROUP_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.CURRENT_SOURCE_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.CURRENT_TIMEPOINT_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.GROUP_ACTIVITY_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.GROUP_NAME_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.INTERPOLATION_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_GROUPS_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_SOURCES_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.NUM_TIMEPOINTS_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.SOURCE_ACTVITY_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.VIEWER_TRANSFORM_CHANGED;
import static bdv.viewer.state.r.ViewerStateChange.VISIBILITY_CHANGED;

public class DefaultViewerState implements ViewerState_Interface
{
	public interface ViewerStateChangeListener
	{
		void viewerStateChanged( ViewerStateChange change );
	}

	private final Listeners.List< ViewerStateChangeListener > listeners = new Listeners.List<>();

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
	 * TODO move javadoc to getter/setter in interface
	 */
	private DisplayMode displayMode;

	// -- sources --

	private final ArrayList< SourceAndConverter< ? > > sources;

	private final Set< SourceAndConverter< ? > > activeSources;

	private SourceAndConverter< ? > currentSource;

	private final DefaultSources wrappedSourcesList;

	// -- groups --

	private final ArrayList< SourceGroup > groups;

	private final Map< SourceGroup, GroupData > groupDatas;

	private final Set< SourceGroup > activeGroups;

	private SourceGroup currentGroup;

	private final DefaultSourceGroups wrappedGroupsList;

	public DefaultViewerState()
	{
		numTimepoints = 0;
		currentTimepoint = 0;
		viewerTransform = new AffineTransform3D();
		interpolation = Interpolation.NEARESTNEIGHBOR;
		displayMode = DisplayMode.SINGLE;
		sources = new ArrayList<>();
		activeSources = new HashSet<>();
		wrappedSourcesList = new DefaultSources();
		groups = new ArrayList<>();
		groupDatas = new HashMap<>();
		activeGroups = new HashSet<>();
		wrappedGroupsList = new DefaultSourceGroups();
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

	@Override
	public void getViewerTransform( final AffineTransform3D t )
	{
		t.set( viewerTransform );
	}

	// -- modification --

	@Override
	public void setInterpolation( final Interpolation i )
	{
		if ( interpolation != i )
		{
			interpolation = i;
			notifyListeners( INTERPOLATION_CHANGED );
		}
	}

	@Override
	public void setDisplayMode( final DisplayMode mode )
	{
		if ( displayMode != mode )
		{
			displayMode = mode;
			notifyListeners( DISPLAY_MODE_CHANGED );
		}
	}

	@Override
	public void setNumTimepoints( final int n )
	{
		if ( numTimepoints != n )
		{
			numTimepoints = n;
			notifyListeners( NUM_TIMEPOINTS_CHANGED );
		}
	}

	@Override
	public void setCurrentTimepoint( final int t )
	{
		if ( currentTimepoint != t )
		{
			currentTimepoint = t;
			notifyListeners( CURRENT_TIMEPOINT_CHANGED );
		}
	}

	@Override
	public void setViewerTransform( final AffineTransform3D t )
	{
		if ( !Affine3DHelpers.equals( viewerTransform, t ) )
		{
			viewerTransform.set( t );
			notifyListeners( VIEWER_TRANSFORM_CHANGED );
		}
	}

	//
	// -- wrappers --
	//

	/**
	 * Returned value is a valid (at least) as long as state is not modified.
	 * If state is modified, it might become invalid or reflect an older snapshot.
	 */
	@Override
	public SourceGroups getGroups()
	{
		return wrappedGroupsList;
	}

	/**
	 * Returned value is a valid (at least) as long as state is not modified.
	 * If state is modified, it might become invalid or reflect an older snapshot.
	 */
	@Override
	public Sources getSources()
	{
		return wrappedSourcesList;
	}

	private class WrappedActiveSources extends WrappedSet< SourceAndConverter< ? > >
	{
		WrappedActiveSources()
		{
			super( Collections.unmodifiableSet( activeSources ) );
		}

		@Override
		public boolean add( final SourceAndConverter< ? > source )
		{
			return DefaultViewerState.this.setActive( source, true );
		}

		@Override
		public boolean remove( final Object o )
		{
			if ( o instanceof SourceAndConverter )
				return DefaultViewerState.this.setActive( ( SourceAndConverter< ? > ) o, false );
			else
				return false;
		}
	}

	private class DefaultSources extends WrappedList< SourceAndConverter< ? > > implements Sources
	{
		private final Set< SourceAndConverter< ? > > wrappedActiveSources = new WrappedActiveSources();

		DefaultSources()
		{
			super( Collections.unmodifiableList( sources ) );
		}

		@Override
		public SourceAndConverter< ? > getCurrent()
		{
			return currentSource;
		}

		@Override
		public Set< SourceAndConverter< ? > > getActive()
		{
			return wrappedActiveSources;
		}

		@Override
		public Set< SourceAndConverter< ? > > getVisible()
		{
			return DefaultViewerState.this.getVisibleSources();
		}

		@Override
		public boolean isActive( final SourceAndConverter< ? > source )
		{
			return DefaultViewerState.this.isActive( source );
		}

		@Override
		public boolean isCurrent( final SourceAndConverter< ? > source )
		{
			return DefaultViewerState.this.isCurrent( source );
		}

		@Override
		public boolean isVisible( final SourceAndConverter< ? > source )
		{
			return DefaultViewerState.this.isVisible( source );
		}

		@Override
		public Comparator< SourceAndConverter< ? > > order()
		{
			// TODO make more efficient
			return Comparator.comparingInt( sources::indexOf );
		}

		//
		// -- modification operations --
		//

		@Override
		public boolean setActive( final SourceAndConverter< ? > source, final boolean active )
		{
			return DefaultViewerState.this.setActive( source, active );
		}

		@Override
		public boolean makeCurrent( final SourceAndConverter< ? > source )
		{
			return DefaultViewerState.this.makeCurrent( source );
		}

		@Override
		public boolean add( final SourceAndConverter< ? > sourceAndConverter )
		{
			return DefaultViewerState.this.addSource( sourceAndConverter );
		}

		@Override
		public boolean remove( final Object o )
		{
			if ( o instanceof SourceAndConverter )
				return DefaultViewerState.this.removeSource( ( SourceAndConverter< ? > ) o );
			else
				return false;
		}
	}

	private class GroupData
	{
		private final SourceGroup group;

		String name = null;

		final Set< SourceAndConverter< ? > > sources = new HashSet<>();

		final GroupData.WrappedContainedSources wrappedSources;

		private class WrappedContainedSources extends WrappedSet< SourceAndConverter< ? > >
		{
			WrappedContainedSources( Set< SourceAndConverter< ? > > containedSources )
			{
				super( Collections.unmodifiableSet( containedSources ) );
			}

			@Override
			public boolean add( final SourceAndConverter< ? > source )
			{
				return DefaultViewerState.this.addSourceToGroup( source, group );
			}

			@Override
			public boolean remove( final Object o )
			{
				if ( o instanceof SourceAndConverter )
					return DefaultViewerState.this.removeSourceFromGroup( ( SourceAndConverter< ? > ) o, group );
				else
					return false;
			}
		}

		GroupData( SourceGroup group )
		{
			this.group = group;
			this.wrappedSources = new GroupData.WrappedContainedSources( sources );
		}
	}

	private class WrappedActiveGroups extends WrappedSet< SourceGroup >
	{
		WrappedActiveGroups()
		{
			super( Collections.unmodifiableSet( activeGroups ) );
		}

		@Override
		public boolean add( final SourceGroup group )
		{
			return DefaultViewerState.this.setActive( group, true );
		}

		@Override
		public boolean remove( final Object o )
		{
			if ( o instanceof SourceGroup )
				return DefaultViewerState.this.setActive( ( SourceGroup ) o, false );
			else
				return false;
		}
	}

	private class DefaultSourceGroups extends WrappedList< SourceGroup > implements SourceGroups
	{
		private final Set< SourceGroup > wrappedActiveGroups = new WrappedActiveGroups();

		DefaultSourceGroups()
		{
			super( Collections.unmodifiableList( groups ) );
		}

		@Override
		public SourceGroup getCurrent()
		{
			return currentGroup;
		}

		@Override
		public Set< SourceGroup > getActive()
		{
			return wrappedActiveGroups;
		}

		@Override
		public Set< SourceAndConverter< ? > > getSourcesIn( final SourceGroup group )
		{
			checkIsExistingGroup( group );

			return groupDatas.get( group ).wrappedSources;
		}

		@Override
		public Set< SourceGroup > getMatching( final Predicate< SourceGroup > condition )
		{
			Set< SourceGroup > result = new HashSet<>();
			for ( SourceGroup group : groups )
				if ( condition.test( group ) )
					result.add( group );
			return result;
		}

		@Override
		public String getName( final SourceGroup group )
		{
			return DefaultViewerState.this.getName( group );
		}

		@Override
		public boolean isActive( final SourceGroup group )
		{
			return DefaultViewerState.this.isActive( group );
		}

		@Override
		public boolean isCurrent( final SourceGroup group )
		{
			return DefaultViewerState.this.isCurrent( group );
		}

		@Override
		public Comparator< SourceGroup > order()
		{
			// TODO make more efficient
			return Comparator.comparingInt( groups::indexOf );
		}

		//
		// -- modification operations --
		//

		@Override
		public void setName( final SourceGroup group, final String name )
		{
			DefaultViewerState.this.setName( group, name );
		}

		@Override
		public boolean setActive( final SourceGroup group, final boolean active )
		{
			return DefaultViewerState.this.setActive( group, active );
		}

		@Override
		public boolean makeCurrent( final SourceGroup group )
		{
			return DefaultViewerState.this.makeCurrent( group );
		}

		@Override
		public boolean add( final SourceGroup group )
		{
			return DefaultViewerState.this.addGroup( group );
		}

		@Override
		public boolean remove( final Object o )
		{
			if ( o instanceof SourceGroup )
				return DefaultViewerState.this.removeGroup( ( SourceGroup ) o );
			else
				return false;
		}

		@Override
		public boolean addSourceToGroup( final SourceAndConverter< ? > source, final SourceGroup group )
		{
			return DefaultViewerState.this.addSourceToGroup( source, group );
		}

		@Override
		public boolean removeSourceFromGroup( final SourceAndConverter< ? > source, final SourceGroup group )
		{
			return DefaultViewerState.this.removeSourceFromGroup( source, group );
		}

	}

	/*
	 * All source/group modifications are routed through the methods below,
	 * which send notification to listeners.
	 */

	//
	// --- sources ---
	//

	private void checkIsExistingSource( final SourceAndConverter< ? > source )
	{
		if ( source == null | !sources.contains( source ) )
			throw new IllegalArgumentException();
	}

	private boolean isPresent( final SourceAndConverter< ? > source )
	{
		return source.getSpimSource().isPresent( currentTimepoint );
	}

	private boolean isActive( final SourceAndConverter< ? > source )
	{
		return activeSources.contains( source );
	}

	public boolean isCurrent( final SourceAndConverter< ? > source )
	{
		return Objects.equals( source, currentSource );
	}

	private boolean isVisible( final SourceAndConverter< ? > source )
	{
		checkIsExistingSource( source );

		switch ( displayMode )
		{
		case SINGLE:
		default:
			return Objects.equals( currentSource, source );
		case GROUP:
			return currentGroup != null && groupDatas.get( currentGroup ).sources.contains( source );
		case FUSED:
			return isActive( source );
		case FUSEDGROUP:
			for ( SourceGroup group : activeGroups )
				if ( groupDatas.get( group ).sources.contains( source ) )
					return true;
			return false;
		}
	}

	private Set< SourceAndConverter< ? > > getVisibleSources()
	{
		final Set< SourceAndConverter< ? > > visible = new HashSet<>();
		switch ( displayMode )
		{
		case SINGLE:
			if ( currentSource != null && isPresent( currentSource ))
				visible.add( currentSource );
			break;
		case GROUP:
			if ( currentGroup != null )
				for ( SourceAndConverter< ? > source : groupDatas.get( currentGroup ).sources )
					if ( isPresent( source ) )
						visible.add( source );
			break;
		case FUSED:
			for ( SourceAndConverter< ? > source : activeSources )
				if ( isPresent( source ) )
					visible.add( source );
			break;
		case FUSEDGROUP:
			for ( SourceGroup group : activeGroups )
				for ( SourceAndConverter< ? > source : groupDatas.get( group ).sources )
					if ( isPresent( source ) )
						visible.add( source );
			break;
		}
		return visible;
	}

	private boolean addSource( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			throw new IllegalArgumentException();

		if ( sources.contains( source ) )
			return false;

		sources.add( source );
		final boolean currentSourceChanged = ( currentSource == null );
		if ( currentSourceChanged )
			currentSource = source;

		notifyListeners( NUM_SOURCES_CHANGED );
		if ( currentSourceChanged )
			notifyListeners( CURRENT_SOURCE_CHANGED );
		checkVisibilityChanged();

		return true;
	}

	private boolean removeSource( final SourceAndConverter< ? > source )
	{
		if ( source == null )
			return false;

		final boolean removed = sources.remove( source );
		if ( removed )
		{
			activeSources.remove( source );
			final boolean currentSourceChanged = currentSource.equals( source );
			if ( currentSourceChanged )
				currentSource = sources.isEmpty() ? null : sources.get( 0 );

			boolean sourceToGroupAssignmentChanged = false;
			for ( GroupData groupData : groupDatas.values() )
				sourceToGroupAssignmentChanged |= groupData.sources.remove( source );

			notifyListeners( NUM_SOURCES_CHANGED );
			if ( currentSourceChanged )
				notifyListeners( CURRENT_SOURCE_CHANGED );
			if ( sourceToGroupAssignmentChanged )
				notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}
		return removed;
	}

	private boolean makeCurrent( SourceAndConverter< ? > source )
	{
		checkIsExistingSource( source );

		final boolean modified = !currentSource.equals( source );
		currentSource = source;
		if ( modified )
		{
			notifyListeners( CURRENT_SOURCE_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	private boolean setActive( SourceAndConverter< ? > source, boolean active )
	{
		checkIsExistingSource( source );

		final boolean modified = active ? activeSources.add( source ) : activeSources.remove( source );
		if ( modified )
		{
			notifyListeners( SOURCE_ACTVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	//
	// -- groups --
	//

	private void checkIsExistingGroup( final SourceGroup group )
	{
		if ( group == null || !groupDatas.containsKey( group ) )
			throw new IllegalArgumentException();
	}

	private String getName( final SourceGroup group )
	{
		checkIsExistingGroup( group );

		return groupDatas.get( group ).name;
	}

	private boolean isActive( final SourceGroup group )
	{
		return activeGroups.contains( group );
	}

	private boolean isCurrent( final SourceGroup group )
	{
		return Objects.equals( group, currentGroup );
	}

	private boolean addGroup( final SourceGroup group )
	{
		if ( group == null )
			throw new IllegalArgumentException();

		if ( groupDatas.containsKey( group ) )
			return false;

		groups.add( group );
		groupDatas.put( group, new GroupData( group ) );
		final boolean currentGroupChanged = ( currentGroup == null );
		if ( currentGroupChanged )
			currentGroup = group;

		notifyListeners( NUM_GROUPS_CHANGED );
		if ( currentGroupChanged )
			notifyListeners( CURRENT_GROUP_CHANGED );
		// new group is empty, so visibility will not change
		// checkVisibilityChanged();

		return true;
	}

	private boolean removeGroup( final SourceGroup group )
	{
		if ( group == null )
			return false;

		final boolean removed = groups.remove( group );
		if ( removed )
		{
			groupDatas.remove( group );
			activeGroups.remove( group );
			final boolean currentGroupChanged = currentGroup.equals( group );
			if ( currentGroupChanged )
				currentGroup = groups.isEmpty() ? null : groups.get( 0 );

			notifyListeners( NUM_GROUPS_CHANGED );
			if ( currentGroupChanged )
				notifyListeners( CURRENT_GROUP_CHANGED );
			checkVisibilityChanged();
		}
		return removed;
	}

	private void setName( final SourceGroup group, final String name )
	{
		checkIsExistingGroup( group );

		final GroupData data = groupDatas.get( group );
		if ( !Objects.equals( data.name, name ) )
		{
			data.name = name;
			notifyListeners( GROUP_NAME_CHANGED );
		}
	}

	private boolean makeCurrent( SourceGroup group )
	{
		checkIsExistingGroup( group );

		final boolean modified = !currentGroup.equals( group );
		currentGroup = group;
		if ( modified )
		{
			notifyListeners( CURRENT_GROUP_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	private boolean setActive( SourceGroup group, boolean active )
	{
		checkIsExistingGroup( group );

		final boolean modified = active ? activeGroups.add( group ) : activeGroups.remove( group );
		if ( modified )
		{
			notifyListeners( GROUP_ACTIVITY_CHANGED );
			checkVisibilityChanged();
		}
		return modified;
	}

	private boolean addSourceToGroup( SourceAndConverter< ? > source, SourceGroup group )
	{
		checkIsExistingSource( source );
		checkIsExistingGroup( group );

		final boolean modified = groupDatas.get( group ).sources.add( source );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}

		return modified;
	}

	private boolean removeSourceFromGroup( SourceAndConverter< ? > source, SourceGroup group )
	{
		checkIsExistingSource( source );
		checkIsExistingGroup( group );

		final boolean modified = groupDatas.get( group ).sources.remove( source );
		if ( modified )
		{
			notifyListeners( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
			checkVisibilityChanged();
		}

		return modified;
	}

	// -- Change Listeners --

	private void notifyListeners( ViewerStateChange change )
	{
		listeners.list.forEach( l -> l.viewerStateChanged( change ) );
	}

	private final Set< SourceAndConverter< ? > > previousVisibleSources = new HashSet<>();

	private void checkVisibilityChanged()
	{
		final Set< SourceAndConverter< ? > > visible = getVisibleSources();
		if ( !visible.equals( previousVisibleSources ) )
		{
			previousVisibleSources.clear();
			previousVisibleSources.addAll( visible );
			notifyListeners( VISIBILITY_CHANGED );
		}
	}
}
