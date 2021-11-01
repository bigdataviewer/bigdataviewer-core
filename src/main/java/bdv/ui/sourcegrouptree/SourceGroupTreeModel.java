/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.ui.sourcegrouptree;

import bdv.util.WrappedList;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import bdv.viewer.ViewerState;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import static gnu.trove.impl.Constants.DEFAULT_CAPACITY;
import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;

/**
 * @author Tobias Pietzsch
 */
public class SourceGroupTreeModel implements TreeModel
{
	private final EventListenerList listenerList = new EventListenerList();

	private final String root = "root";

	private final Object[] rootPath = new Object[] { root };

	private final ViewerState state;

	private StateModel model;

	public SourceGroupTreeModel( final ViewerState state )
	{
		model = new StateModel( state );
		this.state = state;
		state.changeListeners().add( e ->
		{
			switch ( e )
			{
			case CURRENT_GROUP_CHANGED:
			case GROUP_ACTIVITY_CHANGED:
			case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
			case GROUP_NAME_CHANGED:
			case NUM_GROUPS_CHANGED:
				final StateModel model = new StateModel( state );
				SwingUtilities.invokeLater( () -> analyzeChanges( model ) );
			}
		} );
	}

	@Override
	public Object getRoot()
	{
		return root;
	}

	@Override
	public Object getChild( final Object parent, final int index )
	{
		if ( parent == root )
		{
			return model.getGroups().get( index );
		}
		else if ( parent instanceof GroupModel )
		{
			return ( ( GroupModel ) parent ).getSources().get( index );
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

	@Override
	public int getChildCount( final Object parent )
	{
		if ( parent == root )
		{
			return model.getGroups().size();
		}
		else if ( parent instanceof GroupModel )
		{
			return ( ( GroupModel ) parent ).getSources().size();
		}
		else
		{
			return 0;
		}
	}

	@Override
	public boolean isLeaf( final Object node )
	{
		if ( node == root )
		{
			return model.getGroups().isEmpty();
		}
		else if ( node instanceof GroupModel )
		{
			return ( ( GroupModel ) node ).getSources().isEmpty();
		}
		else
		{
			return true;
		}
	}

	@Override
	public void valueForPathChanged( final TreePath path, final Object newValue )
	{
		final Object o = path.getLastPathComponent();
		if ( o instanceof GroupModel )
			state.setGroupName( ( ( GroupModel ) o ).group, newValue.toString() );
	}

	@Override
	public int getIndexOfChild( final Object parent, final Object child )
	{
		if ( parent == root )
		{
			return model.getGroups().indexOf( child );
		}
		else if ( parent instanceof GroupModel )
		{
			return ( ( GroupModel ) parent ).getSources().indexOf( child );
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

	//
	//  Events
	//

	/**
	 * Adds a listener for the TreeModelEvent posted after the tree changes.
	 *
	 * @param l
	 * 		the listener to add
	 *
	 * @see #removeTreeModelListener
	 */
	@Override
	public void addTreeModelListener( TreeModelListener l )
	{
		listenerList.add( TreeModelListener.class, l );
	}

	/**
	 * Removes a listener previously added with <B>addTreeModelListener()</B>.
	 *
	 * @param l
	 * 		the listener to remove
	 *
	 * @see #addTreeModelListener
	 */
	@Override
	public void removeTreeModelListener( TreeModelListener l )
	{
		listenerList.remove( TreeModelListener.class, l );
	}

	private void fireTreeNodesChanged(final TreeModelEvent e)
	{
		final Object[] listeners = listenerList.getListenerList();
		for ( int i = listeners.length - 2; i >= 0; i -= 2 )
			if ( listeners[ i ] == TreeModelListener.class )
				( ( TreeModelListener ) listeners[ i + 1 ] ).treeNodesChanged( e );
	}

	private void fireTreeNodesInserted(final TreeModelEvent e)
	{
		final Object[] listeners = listenerList.getListenerList();
		for ( int i = listeners.length - 2; i >= 0; i -= 2 )
			if ( listeners[ i ] == TreeModelListener.class )
				( ( TreeModelListener ) listeners[ i + 1 ] ).treeNodesInserted( e );
	}

	private void fireTreeNodesRemoved(final TreeModelEvent e)
	{
		final Object[] listeners = listenerList.getListenerList();
		for ( int i = listeners.length - 2; i >= 0; i -= 2 )
			if ( listeners[ i ] == TreeModelListener.class )
				( ( TreeModelListener ) listeners[ i + 1 ] ).treeNodesRemoved( e );
	}

	private void fireTreeStructureChanged(final TreeModelEvent e)
	{
		final Object[] listeners = listenerList.getListenerList();
		for ( int i = listeners.length - 2; i >= 0; i -= 2 )
			if ( listeners[ i ] == TreeModelListener.class )
				( ( TreeModelListener ) listeners[ i + 1 ] ).treeStructureChanged( e );
	}

	public TreePath getPathTo( final GroupModel group )
	{
		return new TreePath( new Object[] { root, group } );
	}

	private void analyzeChanges( final StateModel model )
	{
		final StateModel previousModel = this.model;
		this.model = model;

		// -- NUM_GROUPS_CHANGED --

		final List< GroupModel > removedGroups = new ArrayList<>();
		for ( GroupModel group : previousModel.getGroups() )
			if ( !model.getGroups().contains( group ) )
				removedGroups.add( group );

		final List< GroupModel > addedGroups = new ArrayList<>();
		for ( GroupModel group : model.getGroups() )
			if ( !previousModel.getGroups().contains( group ) )
				addedGroups.add( group );

		// -- GROUP_NAME_CHANGED, CURRENT_GROUP_CHANGED, GROUP_ACTIVITY_CHANGED --

		final List< GroupModel > changedGroups = new ArrayList<>();
		for ( GroupModel group : model.getGroups() )
		{
			final GroupModel previousGroup = previousModel.getGroups().get( group );
			if ( previousGroup != null )
			{
				if ( group.isCurrent() != previousGroup.isCurrent() ||
						group.isActive() != previousGroup.isActive() ||
						!Objects.equals( group.getName(), previousGroup.getName() ) )
				{
					changedGroups.add( group );
				}
			}
		}

		// -- SOURCE_TO_GROUP_ASSIGNMENT_CHANGED --

		final List< GroupModel > structurallyChangedGroups = new ArrayList<>();
		for ( GroupModel group : model.getGroups() )
		{
			final GroupModel previousGroup = previousModel.getGroups().get( group );
			if ( previousGroup != null )
			{
				final List< SourceModel > content = group.getSources();
				final List< SourceModel > previousContent = previousGroup.getSources();
				if ( !content.equals( previousContent ) )
					structurallyChangedGroups.add( group );
			}
		}

		// -- create corresponding TreeModelEvents --

		// groups added or removed
		if ( !addedGroups.isEmpty() )
		{
			final int[] childIndices = new int[ addedGroups.size() ];
			Arrays.setAll( childIndices, i -> model.getGroups().indexOf( addedGroups.get( i ) ) );
			final Object[] children = addedGroups.toArray( new Object[ 0 ] );
			fireTreeNodesInserted( new TreeModelEvent( this, rootPath, childIndices, children ) );
		}
		else if ( !removedGroups.isEmpty() )
		{
			final int[] childIndices = new int[ removedGroups.size() ];
			Arrays.setAll( childIndices, i -> previousModel.getGroups().indexOf( removedGroups.get( i ) ) );
			final Object[] children = removedGroups.toArray( new Object[ 0 ] );
			fireTreeNodesRemoved( new TreeModelEvent( this, rootPath, childIndices, children ) );
		}

		// groups that change currentness, activeness, or name
		if ( !changedGroups.isEmpty() )
		{
			final int[] childIndices = new int[ changedGroups.size() ];
			Arrays.setAll( childIndices, i -> model.getGroups().indexOf( changedGroups.get( i ) ) );
			final Object[] children = changedGroups.toArray( new Object[ 0 ] );
			fireTreeNodesChanged( new TreeModelEvent( this, rootPath, childIndices, children ) );
		}

		// groups that had children added or removed
		if ( !structurallyChangedGroups.isEmpty() )
		{
			for ( GroupModel group : structurallyChangedGroups )
			{
				final Object[] path = new Object[] { root, group };
				fireTreeStructureChanged( new TreeModelEvent( this, path, null, null ) );
			}
		}
	}

	//
	//  Internal state model
	//

	private static final int NO_ENTRY_VALUE = -1;

	static class StateModel
	{
		private final UnmodifiableGroups groups;

		public StateModel( final ViewerState state )
		{
			final List< GroupModel > glist = new ArrayList<>();
			final TObjectIntMap< GroupModel > gindices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
			final List< SourceGroup > sgroups = state.getGroups();
			for ( int i = 0; i < sgroups.size(); ++i )
			{
				final GroupModel groupModel = new GroupModel( sgroups.get( i ), state );
				glist.add( groupModel );
				gindices.put( groupModel, i );
			}
			groups = new UnmodifiableGroups( glist, gindices );
		}

		public UnmodifiableGroups getGroups()
		{
			return groups;
		}

		static class UnmodifiableGroups extends WrappedList< GroupModel >
		{
			private final TObjectIntMap< GroupModel > groupIndices;

			public UnmodifiableGroups(final List< GroupModel > groups, final TObjectIntMap< GroupModel > groupIndices)
			{
				super( Collections.unmodifiableList( groups ) );
				this.groupIndices = groupIndices;
			}

			public GroupModel get( GroupModel groupModel )
			{
				final int index = groupIndices.get( groupModel );
				return index == NO_ENTRY_VALUE ? null : get( index );
			}

			@Override
			public boolean contains( final Object o )
			{
				return groupIndices.containsKey( o );
			}

			@Override
			public boolean containsAll( final Collection< ? > c )
			{
				return groupIndices.keySet().containsAll( c );
			}

			@Override
			public int indexOf( final Object o )
			{
				return groupIndices.get( o );
			}

			@Override
			public int lastIndexOf( final Object o )
			{
				return groupIndices.get( o );
			}
		}
	}

	static class GroupModel
	{
		private final String name;
		private final boolean active;
		private final boolean current;

		private final List< SourceModel > sources;

		private final SourceGroup group;

		public GroupModel( final SourceGroup group, final ViewerState state )
		{
			name = state.getGroupName( group );
			active = state.isGroupActive( group );
			current = state.isCurrentGroup( group );

			final List< SourceAndConverter< ? > > orderedSources = new ArrayList<>( state.getSourcesInGroup( group ) );
			orderedSources.sort( state.sourceOrder() );
			final List< SourceModel > slist = new ArrayList<>();
			final TObjectIntMap< SourceModel > sindices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
			for ( int i = 0; i < orderedSources.size(); ++i )
			{
				final SourceModel sourceModel = new SourceModel( orderedSources.get( i ) );
				slist.add( sourceModel );
				sindices.put( sourceModel, i );
			}
			sources = new UnmodifiableSources( slist, sindices );

			this.group = group;
		}

		public String getName()
		{
			return name;
		}

		public boolean isActive()
		{
			return active;
		}

		public boolean isCurrent()
		{
			return current;
		}

		public List< SourceModel > getSources()
		{
			return sources;
		}

		public SourceGroup getGroup()
		{
			return group;
		}

		@Override
		public boolean equals( final Object o )
		{
			return ( o instanceof GroupModel ) && group.equals( ( ( GroupModel ) o ).group );
		}

		@Override
		public int hashCode()
		{
			return group.hashCode();
		}

		static class UnmodifiableSources extends WrappedList< SourceModel >
		{
			private final TObjectIntMap< SourceModel > sourceIndices;

			public UnmodifiableSources( final List< SourceModel > sources, final TObjectIntMap< SourceModel > sourceIndices )
			{
				super( Collections.unmodifiableList( sources ) );
				this.sourceIndices = sourceIndices;
			}

			@Override
			public boolean contains( final Object o )
			{
				return sourceIndices.containsKey( o );
			}

			@Override
			public boolean containsAll( final Collection< ? > c )
			{
				return sourceIndices.keySet().containsAll( c );
			}

			@Override
			public int indexOf( final Object o )
			{
				return sourceIndices.get( o );
			}

			@Override
			public int lastIndexOf( final Object o )
			{
				return sourceIndices.get( o );
			}
		}
	}

	static class SourceModel
	{
		private final String name;

		private final SourceAndConverter< ? > source;

		public SourceModel( final SourceAndConverter< ? > source )
		{
			name = source.getSpimSource().getName();
			this.source = source;
		}

		public String getName()
		{
			return name;
		}

		public SourceAndConverter<?> getSource()
		{
			return source;
		}

		@Override
		public boolean equals( final Object o )
		{
			return ( o instanceof SourceModel ) && source.equals( ( ( SourceModel ) o ).source );
		}

		@Override
		public int hashCode()
		{
			return source.hashCode();
		}
	}
}
