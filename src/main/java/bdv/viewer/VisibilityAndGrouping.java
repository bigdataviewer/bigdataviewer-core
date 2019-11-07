/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer;

import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.FUSEDGROUP;
import static bdv.viewer.DisplayMode.GROUP;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.VisibilityAndGrouping.Event.CURRENT_GROUP_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.GROUP_ACTIVITY_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.GROUP_NAME_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;

/**
 * Manage visibility and currentness of sources and groups, as well as grouping
 * of sources, and display mode.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
@Deprecated
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

		public static final int NUM_GROUPS_CHANGED = 9;

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

	private final bdv.viewer.state.ViewerState deprecatedViewerState;

	private final SynchronizedViewerState state;

	public SynchronizedViewerState getState()
	{
		return state;
	}

	public VisibilityAndGrouping( final bdv.viewer.state.ViewerState viewerState )
	{
		updateListeners = new CopyOnWriteArrayList<>();
		deprecatedViewerState = viewerState;
		state = viewerState.getState();
		viewerState.getState().changeListeners().add( e ->
		{
			if ( e == ViewerStateChange.VISIBILITY_CHANGED )
				update( VISIBILITY_CHANGED );
		} );
		viewerState.getState().changeListeners().add( e ->
		{
			// log
		} );
	}

	public int numSources()
	{
		return state.getSources().size();
	}

	public List< SourceState< ? > > getSources()
	{
		return deprecatedViewerState.getSources();
	}

	public int numGroups()
	{
		return state.getGroups().size();
	}

	public List< SourceGroup > getSourceGroups()
	{
		return deprecatedViewerState.getSourceGroups();
	}

	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		state.setDisplayMode( displayMode );
		update( DISPLAY_MODE_CHANGED );
	}

	public synchronized int getCurrentSource()
	{
		return state.getSources().indexOf( state.getCurrentSource() );
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

		state.setCurrentSource( state.getSources().get( sourceIndex ) );
		update( CURRENT_SOURCE_CHANGED );
	};

	public synchronized void setCurrentSource( final Source< ? > source )
	{
		state.setCurrentSource( soc( source ) );
		update( CURRENT_SOURCE_CHANGED );
	};

	public synchronized boolean isSourceActive( final int sourceIndex )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return false;

		return state.isSourceActive( state.getSources().get( sourceIndex ) );
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

		state.setSourceActive( state.getSources().get( sourceIndex ), isActive );
		update( SOURCE_ACTVITY_CHANGED );
	}

	/**
	 * Set the source active (visible in fused mode) or inactive.
	 *
	 * @param source
	 * @param isActive
	 */
	public synchronized void setSourceActive( final Source< ? > source, final boolean isActive )
	{
		state.setSourceActive( soc( source ), isActive );
		update( SOURCE_ACTVITY_CHANGED );
	}

	public synchronized int getCurrentGroup()
	{
		return state.getGroups().indexOf( state.getCurrentGroup() );
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

		final bdv.viewer.SourceGroup group = state.getGroups().get( groupIndex );
		state.setCurrentGroup( group );
		update( CURRENT_GROUP_CHANGED );
		final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSourcesInGroup( group ) );
		if ( ! sources.isEmpty() )
		{
			sources.sort( state.sourceOrder() );
			state.setCurrentSource( sources.get( 0 ) );
		}
	}

	public synchronized boolean isGroupActive( final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return false;

		return state.isGroupActive( state.getGroups().get( groupIndex ) );
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

		state.setGroupActive( state.getGroups().get( groupIndex ), isActive );
		update( GROUP_ACTIVITY_CHANGED );
	}

	public synchronized void setGroupName( final int groupIndex, final String name )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.setGroupName( state.getGroups().get( groupIndex ), name );
		update( GROUP_NAME_CHANGED );
	}

	public synchronized void addSourceToGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.addSourceToGroup( state.getSources().get( sourceIndex ), state.getGroups().get( groupIndex ) );
		update( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
	}

	public synchronized void removeSourceFromGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.removeSourceFromGroup( state.getSources().get( sourceIndex ), state.getGroups().get( groupIndex ) );
		update( SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
	}

	/**
	 * TODO
	 * @param index
	 */
	public synchronized void setCurrentGroupOrSource( final int index )
	{
		if ( isGroupingEnabled() )
			setCurrentGroup( index );
		else
			setCurrentSource( index );
	}

	/**
	 * TODO
	 * @param index
	 */
	public synchronized void toggleActiveGroupOrSource( final int index )
	{
		if ( isGroupingEnabled() )
			setGroupActive( index, !isGroupActive( index ) );
		else
			setSourceActive( index, !isSourceActive( index ) );
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
		return state.isSourceVisibleAndPresent( state.getSources().get( sourceIndex ) );
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

	private SourceAndConverter< ? > soc( Source< ? > source )
	{
		for ( SourceAndConverter< ? > soc : state.getSources() )
			if ( soc.getSpimSource() == source )
				return soc;
		return null;
	}
}
