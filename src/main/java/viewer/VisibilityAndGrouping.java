package viewer;

import static viewer.VisibilityAndGrouping.Event.CURRENT_GROUP_CHANGED;
import static viewer.VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.GROUP_ACTIVITY_CHANGED;
import static viewer.VisibilityAndGrouping.Event.GROUP_NAME_CHANGED;
import static viewer.VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED;
import static viewer.VisibilityAndGrouping.Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;
import static viewer.render.DisplayMode.FUSED;
import static viewer.render.DisplayMode.FUSEDGROUP;
import static viewer.render.DisplayMode.GROUP;
import static viewer.render.DisplayMode.SINGLE;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

import viewer.gui.XmlIoViewerState;
import viewer.render.DisplayMode;
import viewer.render.SourceGroup;
import viewer.render.SourceState;
import viewer.render.ViewerState;

public class VisibilityAndGrouping
{
	public static final class Event
	{
		public static final int CURRENT_SOURCE_CHANGED = 0;

		public static final int CURRENT_GROUP_CHANGED = 1;

		public static final int SOURCE_ACTVITY_CHANGED = 2;

		public static final int GROUP_ACTIVITY_CHANGED = 3;

		public static final int DISPLAY_MODE_CHANGED = 4;

		public static final int SOURCE_TO_GROUP_ASSIGNMENT_CHANGED = 5;

		public static final int GROUP_NAME_CHANGED = 6;

		public static final int VISIBILITY_CHANGED = 7;

		public static final int NUM_SOURCES_CHANGED = 8;

		public final int id;

		public final VisibilityAndGrouping visibilityAndGrouping;

		public Event( final int id, final VisibilityAndGrouping v )
		{
			this.id = id;
			this.visibilityAndGrouping = v;
		}
	}

	public interface UpdateListener
	{
		public void visibilityChanged( Event e );
	}

	protected final CopyOnWriteArrayList< UpdateListener > updateListeners;

	protected final ViewerState state;

	protected final XmlIoViewerState io;

	public VisibilityAndGrouping( final ViewerState viewerState )
	{
		updateListeners = new CopyOnWriteArrayList< UpdateListener >();
		state = viewerState;
		io = new XmlIoViewerState();
	}

	public int numSources()
	{
		return state.numSources();
	}

	public List< SourceState< ? > > getSources()
	{
		return state.getSources();
	}

	public int numGroups()
	{
		return state.numSourceGroups();
	}

	public List< SourceGroup > getSourceGroups()
	{
		return state.getSourceGroups();
	}

	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		state.setDisplayMode( displayMode );
		checkVisibilityChange();
		update( DISPLAY_MODE_CHANGED );
	}

	public synchronized int getCurrentSource()
	{
		return state.getCurrentSource();
	}

	/**
	 * TODO
	 *
	 * @param sourceIndex
	 */
	public synchronized void setCurrentSource( final int sourceIndex )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		state.setCurrentSource( sourceIndex );
		checkVisibilityChange();
		update( CURRENT_SOURCE_CHANGED );
	};

	public synchronized boolean isSourceActive( final int sourceIndex )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return false;

		return state.getSources().get( sourceIndex ).isActive();
	}

	/**
	 * Set the source active (visible in fused mode) or inactive.
	 *
	 * @param sourceIndex
	 * @param isActive
	 */
	public synchronized void setSourceActive( final int sourceIndex, final boolean isActive )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		state.getSources().get( sourceIndex ).setActive( isActive );
		update( SOURCE_ACTVITY_CHANGED );
		checkVisibilityChange();
	}

	public synchronized int getCurrentGroup()
	{
		return state.getCurrentGroup();
	}

	/**
	 * TODO
	 *
	 * @param groupIndex
	 */
	public synchronized void setCurrentGroup( final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.setCurrentGroup( groupIndex );
		checkVisibilityChange();
		update( CURRENT_GROUP_CHANGED );
		final SortedSet< Integer > ids = state.getSourceGroups().get( groupIndex ).getSourceIds();
		if ( !ids.isEmpty() )
		{
			state.setCurrentSource( ids.first() );
			update( CURRENT_SOURCE_CHANGED );
		}
	}

	public synchronized boolean isGroupActive( final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return false;

		return state.getSourceGroups().get( groupIndex ).isActive();
	}

	/**
	 * Set the group active (visible in fused mode) or inactive.
	 *
	 * @param groupIndex
	 * @param isActive
	 */
	public synchronized void setGroupActive( final int groupIndex, final boolean isActive )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.getSourceGroups().get( groupIndex ).setActive( isActive );
		update( GROUP_ACTIVITY_CHANGED );
		checkVisibilityChange();
	}

	public synchronized void setGroupName( final int groupIndex, final String name )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.getSourceGroups().get( groupIndex ).setName( name );
		update( GROUP_NAME_CHANGED );
	}

	public synchronized void addSourceToGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.getSourceGroups().get( groupIndex ).addSource( sourceIndex );
		update( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
		checkVisibilityChange();
	}

	public synchronized void removeSourceFromGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.getSourceGroups().get( groupIndex ).removeSource( sourceIndex );
		update( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
		checkVisibilityChange();
	}

	public synchronized boolean isGroupingEnabled()
	{
		final DisplayMode mode = state.getDisplayMode();
		return ( mode == GROUP ) || ( mode == FUSEDGROUP );
	}

	public synchronized boolean isFusedEnabled()
	{
		final DisplayMode mode = state.getDisplayMode();
		return ( mode == FUSED ) || ( mode == FUSEDGROUP );
	}

	public synchronized void setGroupingEnabled( final boolean enable )
	{
		setDisplayMode( isFusedEnabled() ? ( enable ? FUSEDGROUP : FUSED ) : ( enable ? GROUP : SINGLE ) );
	}

	public synchronized void setFusedEnabled( final boolean enable )
	{
		setDisplayMode( isGroupingEnabled() ? ( enable ? FUSEDGROUP : GROUP ) : ( enable ? FUSED : SINGLE ) );
	}

	public synchronized boolean isSourceVisible( final int sourceIndex )
	{
		return state.isSourceVisible( sourceIndex );
	}

	protected boolean[] previousVisibleSources = null;

	protected boolean[] currentVisibleSources = null;

	protected void checkVisibilityChange()
	{
		final boolean[] tmp = previousVisibleSources;
		previousVisibleSources = currentVisibleSources;
		currentVisibleSources = tmp;

		final int n = numSources();
		if ( currentVisibleSources == null || currentVisibleSources.length != n )
			currentVisibleSources = new boolean[ n ];
		Arrays.fill( currentVisibleSources, false );
		for ( final int i : state.getVisibleSourceIndices() )
			currentVisibleSources[ i ] = true;

		if ( previousVisibleSources == null || previousVisibleSources.length != n )
		{
			update( VISIBILITY_CHANGED );
			return;
		}

		for ( int i = 0; i < currentVisibleSources.length; ++i )
			if ( currentVisibleSources[ i ] != previousVisibleSources[ i ] )
			{
				update( VISIBILITY_CHANGED );
				return;
			}
	}

	protected void update( final int id )
	{
		final Event event = new Event( id, this );
		for ( final UpdateListener l : updateListeners )
			l.visibilityChanged( event );
	}

	public void addUpdateListener( final UpdateListener l )
	{
		updateListeners.add( l );
	}

	public void removeUpdateListener( final UpdateListener l )
	{
		updateListeners.remove( l );
	}
}
