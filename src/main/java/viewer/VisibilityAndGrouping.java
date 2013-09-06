package viewer;

import static viewer.VisibilityAndGrouping.Event.ACTIVATE;
import static viewer.VisibilityAndGrouping.Event.DEACTIVATE;
import static viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.GROUPING_ENABLED_CHANGED;
import static viewer.VisibilityAndGrouping.Event.MAKE_CURRENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;

import viewer.render.DisplayMode;
import viewer.render.SourceGroup;
import viewer.render.SourceState;
import viewer.render.ViewerState;

public class VisibilityAndGrouping
{
	public static final class Event
	{
		public static final int ACTIVATE = 0;

		public static final int DEACTIVATE = 1;

		public static final int MAKE_CURRENT = 2;

		public static final int DISPLAY_MODE_CHANGED = 3;

		public static final int GROUPING_ENABLED_CHANGED = 4;

		public final int id;

		public final int sourceIndex;

		public final DisplayMode displayMode;

		public Event( final int id, final int sourceIndex, final DisplayMode displayMode )
		{
			this.id = id;
			this.sourceIndex = sourceIndex;
			this.displayMode = displayMode;
		}
	}

	public interface UpdateListener
	{
		public void visibilityChanged( Event e );
	}

	protected final CopyOnWriteArrayList< UpdateListener > updateListeners;

	protected final ViewerState state;

	protected final ArrayList< SourceGroup > groups;

	/**
	 * read-only view of {@link #groups}.
	 */
	final private List< SourceGroup > unmodifiableGroups;

	protected boolean groupingEnabled;

	public VisibilityAndGrouping( final ViewerState viewerState )
	{
		updateListeners = new CopyOnWriteArrayList< UpdateListener >();
		state = viewerState;
		groups = new ArrayList< SourceGroup >();
		unmodifiableGroups = Collections.unmodifiableList( groups );
		groupingEnabled = false;
		for ( int i = 0; i < numSources(); ++i )
		{
			final SourceGroup g = new SourceGroup();
			g.addSource( i );
			groups.add( g );
		}
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
		return groups.size();
	}

	public List< SourceGroup > getSourceGroups()
	{
		return unmodifiableGroups;
	}

	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		state.setDisplayMode( displayMode );
		update( new VisibilityAndGrouping.Event( DISPLAY_MODE_CHANGED, 0, displayMode ) );
	}

	/**
	 * Is the source is active (visible in the specified mode)?
	 *
	 * @return whether the source is active.
	 */
	public synchronized boolean isActive( final int sourceIndex, final DisplayMode displayMode )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return false;

		return state.getSources().get( sourceIndex ).isActive( displayMode );
	}

	public synchronized boolean isVisible( final int sourceIndex )
	{
		return isActive( sourceIndex, getDisplayMode() );
	}

	/**
	 * TODO Set the source active (visible in fused mode) or inactive
	 *
	 * @param sourceIndex
	 * @param displayMode
	 * @param isActive
	 */
	public synchronized void setActive( final int sourceIndex, final DisplayMode displayMode, final boolean isActive )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		if ( displayMode == DisplayMode.SINGLE )
		{
			if ( isActive )
				setCurrentSource( sourceIndex );
		}
		else
		{
			state.getSources().get( sourceIndex ).setActive( displayMode, isActive );
			update( new VisibilityAndGrouping.Event( isActive ? ACTIVATE : DEACTIVATE, sourceIndex, displayMode ) );
		}
	}

	/**
	 * TODO
	 *
	 * @param sourceIndex
	 * @param displayMode
	 */
	public synchronized void toggleActive( final int sourceIndex, final DisplayMode displayMode )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		final SourceState< ? > source = state.getSources().get( sourceIndex );
		if ( displayMode == DisplayMode.SINGLE )
		{
			if ( !source.isActive( displayMode ) )
				setCurrentSource( sourceIndex );
		}
		else
		{
			final boolean a = !source.isActive( displayMode );
			source.setActive( displayMode, a );
			update( new VisibilityAndGrouping.Event( a ? ACTIVATE : DEACTIVATE, sourceIndex, displayMode ) );
		}
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

		final int oldSourceIndex = state.getCurrentSource();
		state.setCurrentSource( sourceIndex );

		update( new VisibilityAndGrouping.Event( MAKE_CURRENT, sourceIndex, null ) );
		if ( oldSourceIndex != sourceIndex )
		{
			update( new VisibilityAndGrouping.Event( DEACTIVATE, oldSourceIndex, DisplayMode.SINGLE ) );
			update( new VisibilityAndGrouping.Event( ACTIVATE, sourceIndex, DisplayMode.SINGLE ) );
		}
	};

	public synchronized int getCurrentSource()
	{
		return state.getCurrentSource();
	}

	public void setGroupingEnabled( final boolean enable )
	{
		if ( groupingEnabled != enable )
		{
			groupingEnabled = enable;
			update( new VisibilityAndGrouping.Event( GROUPING_ENABLED_CHANGED, 0, null ) );
		}
	}

	public boolean isGroupingEnabled()
	{
		return groupingEnabled;
	}

	/**
	 * TODO Set the sources in the group active (visible in fused mode) or inactive
	 */
	public synchronized void setGroupActive( final int groupIndex, final DisplayMode displayMode, final boolean isActive )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		if ( displayMode == DisplayMode.SINGLE )
		{
			if ( isActive )
			{
				final SortedSet< Integer > groupSourceIds = groups.get( groupIndex ).getSourceIds();
				if ( !groupSourceIds.isEmpty() )
					setCurrentSource( groupSourceIds.first() );
			}
		}
		else if ( displayMode == DisplayMode.GROUP )
		{
			if ( isActive )
				setCurrentGroup( groupIndex );
		}
		else
		{
			final SortedSet< Integer > groupSourceIds = groups.get( groupIndex ).getSourceIds();
			for ( final int sourceIndex : groupSourceIds )
			{
				state.getSources().get( sourceIndex ).setActive( displayMode, isActive );
				update( new VisibilityAndGrouping.Event( isActive ? ACTIVATE : DEACTIVATE, sourceIndex, displayMode ) );
			}
		}
	}

	/**
	 * TODO
	 *
	 * @param groupIndex
	 */
	public synchronized void toggleGroupActive( final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		final SourceGroup group = groups.get( groupIndex );

		final boolean a = !group.isActive();
		group.setActive( a );
		final SortedSet< Integer > groupSourceIDs = group.getSourceIds();
		for ( final int i : groupSourceIDs )
			setActive( i, DisplayMode.FUSED, a );
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

		final SortedSet< Integer > groupSourceIds = groups.get( groupIndex ).getSourceIds();
		if ( groupSourceIds.isEmpty() )
			return;

		// activate / deactivate sources in GROUP mode
		for ( int i = 0; i < numSources(); ++i )
		{
			final SourceState< ? > source = getSources().get( i );
			final boolean activate = groupSourceIds.contains( i );
			if ( activate )
				setActive( i, DisplayMode.GROUP, true );
			else if ( source.isActive( DisplayMode.GROUP ) )
				setActive( i, DisplayMode.GROUP, false );
		}

		setCurrentSource( groupSourceIds.first() );
	}

	protected void update( final Event e )
	{
		for ( final UpdateListener l : updateListeners )
			l.visibilityChanged( e );
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
