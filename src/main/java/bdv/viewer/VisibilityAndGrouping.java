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

/**
 * @deprecated This is not necessary anymore, because {@link ViewerState} can be modified directly.
 * (See {@link ViewerPanel#state()}.)
 *
 * Manage visibility and currentness of sources and groups, as well as grouping
 * of sources, and display mode.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
@Deprecated
public class VisibilityAndGrouping
{
	@Deprecated
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

	@Deprecated
	public interface UpdateListener
	{
		void visibilityChanged( Event e );
	}

	protected final CopyOnWriteArrayList< UpdateListener > updateListeners;

	private final bdv.viewer.state.ViewerState deprecatedViewerState;

	private final ViewerState state;

	@Deprecated
	public ViewerState getState()
	{
		return state;
	}

	@Deprecated
	public VisibilityAndGrouping( final bdv.viewer.state.ViewerState viewerState )
	{
		updateListeners = new CopyOnWriteArrayList<>();
		deprecatedViewerState = viewerState;
		state = viewerState.getState();
		viewerState.getState().changeListeners().add( e ->
		{
			switch ( e )
			{
			case CURRENT_SOURCE_CHANGED:
				update( Event.CURRENT_SOURCE_CHANGED );
				break;
			case CURRENT_GROUP_CHANGED:
				update( Event.CURRENT_GROUP_CHANGED );
				break;
			case SOURCE_ACTIVITY_CHANGED:
				update( Event.SOURCE_ACTVITY_CHANGED );
				break;
			case GROUP_ACTIVITY_CHANGED:
				update( Event.GROUP_ACTIVITY_CHANGED );
				break;
			case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
				update( Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED );
				break;
			case GROUP_NAME_CHANGED:
				update( Event.GROUP_NAME_CHANGED );
				break;
			case NUM_SOURCES_CHANGED:
				update( Event.NUM_SOURCES_CHANGED );
				break;
			case NUM_GROUPS_CHANGED:
				update( Event.NUM_GROUPS_CHANGED );
				break;
			case VISIBILITY_CHANGED:
				update( Event.VISIBILITY_CHANGED );
				break;
			case DISPLAY_MODE_CHANGED:
				update( Event.DISPLAY_MODE_CHANGED );
				break;
			}
		} );
	}

	@Deprecated
	public int numSources()
	{
		return state.getSources().size();
	}

	@Deprecated
	public List< SourceState< ? > > getSources()
	{
		return deprecatedViewerState.getSources();
	}

	@Deprecated
	public int numGroups()
	{
		return state.getGroups().size();
	}

	@Deprecated
	public List< SourceGroup > getSourceGroups()
	{
		return deprecatedViewerState.getSourceGroups();
	}

	@Deprecated
	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	@Deprecated
	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		state.setDisplayMode( displayMode );
	}

	@Deprecated
	public synchronized int getCurrentSource()
	{
		return state.getSources().indexOf( state.getCurrentSource() );
	}

	/**
	 * TODO
	 *
	 * @param sourceIndex
	 */
	@Deprecated
	public synchronized void setCurrentSource( final int sourceIndex )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		state.setCurrentSource( state.getSources().get( sourceIndex ) );
	};

	@Deprecated
	public synchronized void setCurrentSource( final Source< ? > source )
	{
		state.setCurrentSource( soc( source ) );
	};

	@Deprecated
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
	@Deprecated
	public synchronized void setSourceActive( final int sourceIndex, final boolean isActive )
	{
		if ( sourceIndex < 0 || sourceIndex >= numSources() )
			return;

		state.setSourceActive( state.getSources().get( sourceIndex ), isActive );
	}

	/**
	 * Set the source active (visible in fused mode) or inactive.
	 *
	 * @param source
	 * @param isActive
	 */
	@Deprecated
	public synchronized void setSourceActive( final Source< ? > source, final boolean isActive )
	{
		state.setSourceActive( soc( source ), isActive );
	}

	@Deprecated
	public synchronized int getCurrentGroup()
	{
		return state.getGroups().indexOf( state.getCurrentGroup() );
	}

	/**
	 * TODO
	 *
	 * @param groupIndex
	 */
	@Deprecated
	public synchronized void setCurrentGroup( final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		final bdv.viewer.SourceGroup group = state.getGroups().get( groupIndex );
		state.setCurrentGroup( group );
		final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSourcesInGroup( group ) );
		if ( ! sources.isEmpty() )
		{
			sources.sort( state.sourceOrder() );
			state.setCurrentSource( sources.get( 0 ) );
		}
	}

	@Deprecated
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
	@Deprecated
	public synchronized void setGroupActive( final int groupIndex, final boolean isActive )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.setGroupActive( state.getGroups().get( groupIndex ), isActive );
	}

	@Deprecated
	public synchronized void setGroupName( final int groupIndex, final String name )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.setGroupName( state.getGroups().get( groupIndex ), name );
	}

	@Deprecated
	public synchronized void addSourceToGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.addSourceToGroup( state.getSources().get( sourceIndex ), state.getGroups().get( groupIndex ) );
	}

	@Deprecated
	public synchronized void removeSourceFromGroup( final int sourceIndex, final int groupIndex )
	{
		if ( groupIndex < 0 || groupIndex >= numGroups() )
			return;

		state.removeSourceFromGroup( state.getSources().get( sourceIndex ), state.getGroups().get( groupIndex ) );
	}

	/**
	 * TODO
	 * @param index
	 */
	@Deprecated
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
	@Deprecated
	public synchronized void toggleActiveGroupOrSource( final int index )
	{
		if ( isGroupingEnabled() )
			setGroupActive( index, !isGroupActive( index ) );
		else
			setSourceActive( index, !isSourceActive( index ) );
	}

	@Deprecated
	public synchronized boolean isGroupingEnabled()
	{
		final DisplayMode mode = state.getDisplayMode();
		return ( mode == GROUP ) || ( mode == FUSEDGROUP );
	}

	@Deprecated
	public synchronized boolean isFusedEnabled()
	{
		final DisplayMode mode = state.getDisplayMode();
		return ( mode == FUSED ) || ( mode == FUSEDGROUP );
	}

	@Deprecated
	public synchronized void setGroupingEnabled( final boolean enable )
	{
		setDisplayMode( isFusedEnabled() ? ( enable ? FUSEDGROUP : FUSED ) : ( enable ? GROUP : SINGLE ) );
	}

	@Deprecated
	public synchronized void setFusedEnabled( final boolean enable )
	{
		setDisplayMode( isGroupingEnabled() ? ( enable ? FUSEDGROUP : GROUP ) : ( enable ? FUSED : SINGLE ) );
	}

	@Deprecated
	public synchronized boolean isSourceVisible( final int sourceIndex )
	{
		return state.isSourceVisibleAndPresent( state.getSources().get( sourceIndex ) );
	}

	@Deprecated
	protected void update( final int id )
	{
		final Event event = new Event( id, this );
		for ( final UpdateListener l : updateListeners )
			l.visibilityChanged( event );
	}

	@Deprecated
	public void addUpdateListener( final UpdateListener l )
	{
		updateListeners.add( l );
	}

	@Deprecated
	public void removeUpdateListener( final UpdateListener l )
	{
		updateListeners.remove( l );
	}

	@Deprecated
	private SourceAndConverter< ? > soc( Source< ? > source )
	{
		for ( SourceAndConverter< ? > soc : state.getSources() )
			if ( soc.getSpimSource() == source )
				return soc;
		return null;
	}
}
