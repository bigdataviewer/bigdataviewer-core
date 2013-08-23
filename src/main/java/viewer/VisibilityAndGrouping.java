package viewer;

import static viewer.VisibilityAndGrouping.Event.ACTIVATE;
import static viewer.VisibilityAndGrouping.Event.DEACTIVATE;
import static viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.MAKE_CURRENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	final ArrayList< SourceGroup > groups;

	public VisibilityAndGrouping( final ViewerState viewerState )
	{
		updateListeners = new CopyOnWriteArrayList< UpdateListener >();
		state = viewerState;
		groups = new ArrayList< SourceGroup >();
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
		return Collections.unmodifiableList( groups );
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
		if ( sourceIndex < 0 || sourceIndex > numSources() )
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
		if ( sourceIndex < 0 || sourceIndex > numSources() )
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
		if ( sourceIndex < 0 || sourceIndex > numSources() )
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
		if ( sourceIndex < 0 || sourceIndex > numSources() )
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
