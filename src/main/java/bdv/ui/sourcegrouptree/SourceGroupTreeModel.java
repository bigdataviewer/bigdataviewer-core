package bdv.ui.sourcegrouptree;

import bdv.viewer.BasicViewerState;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import bdv.viewer.ViewerState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * @author Tobias Pietzsch
 */
public class SourceGroupTreeModel implements TreeModel
{
	private final EventListenerList listenerList = new EventListenerList();

	private final String root = "root";

	private final Object[] rootPath = new Object[] { root };

	private final ViewerState state;

	private final BasicViewerState previousState;

	public SourceGroupTreeModel( final ViewerState state )
	{
		this.state = state;
		previousState = new BasicViewerState( state );
		state.changeListeners().add( e ->
		{
			switch ( e )
			{
			case CURRENT_GROUP_CHANGED:
			case GROUP_ACTIVITY_CHANGED:
			case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
			case GROUP_NAME_CHANGED:
			case NUM_GROUPS_CHANGED:
				analyzeChanges();
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
			return state.getGroups().get( index );
		}
		else if ( parent instanceof SourceGroup )
		{
			return getOrderedSources( parent ).get( index );
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

	private List< SourceAndConverter< ? > > getOrderedSources( final Object parent )
	{
		if ( !( parent instanceof SourceGroup ) )
			throw new IllegalArgumentException();
		List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSourcesInGroup( ( SourceGroup ) parent ) );
		sources.sort( state.sourceOrder() );
		return sources;
	}

	@Override
	public int getChildCount( final Object parent )
	{
		if ( parent == root )
		{
			return state.getGroups().size();
		}
		else if ( parent instanceof SourceGroup )
		{
			return state.getSourcesInGroup( ( SourceGroup ) parent ).size();
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
			return state.getGroups().isEmpty();
		}
		else if ( node instanceof SourceGroup )
		{
			return state.getSourcesInGroup( ( SourceGroup ) node ).isEmpty();
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
		if ( o instanceof  SourceGroup )
			state.setGroupName( ( SourceGroup ) o, newValue.toString() );
	}

	@Override
	public int getIndexOfChild( final Object parent, final Object child )
	{
		if ( parent == root )
		{
			return state.getGroups().indexOf( child );
		}
		else if ( parent instanceof SourceGroup )
		{
			return getOrderedSources( parent ).indexOf( child );
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
	public void removeTreeModelListener( TreeModelListener l )
	{
		listenerList.remove( TreeModelListener.class, l );
	}

	private void fireTreeNodesChanged()
	{
		final Object[] path = new Object[] { root };
		final Object[] children = state.getGroups().toArray( new Object[] {} );
		final int[] childIndices = state.getGroups().stream().mapToInt( g -> getIndexOfChild( root, g ) ).toArray();
		fireTreeNodesChanged( new TreeModelEvent( this, path, childIndices, children ) );
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

	private void analyzeChanges()
	{
		// -- NUM_GROUPS_CHANGED --

		final HashSet< SourceGroup > removedGroups = new HashSet<>( previousState.getGroups() );
		removedGroups.removeAll( state.getGroups() );
//		removedGroups.forEach( g -> System.out.println( "    " + previousState.getGroupName( g ) ) );

		final HashSet< SourceGroup > addedGroups = new HashSet<>( state.getGroups() );
		addedGroups.removeAll( previousState.getGroups() );
//		addedGroups.forEach( g -> System.out.println( "    " + state.getGroupName( g ) ) );

		// -- GROUP_NAME_CHANGED --

		final HashSet< SourceGroup > changedGroups = new HashSet<>();
		for ( SourceGroup group : state.getGroups() )
		{
			if ( previousState.getGroups().contains( group ) )
			{
				final String groupName = state.getGroupName( group );
				final String previousGroupName = previousState.getGroupName( group );
				if ( !Objects.equals( groupName, previousGroupName ) )
				{
//					System.out.println( "    '" + previousGroupName + "' -> '" + groupName + "'" );
					changedGroups.add( group );
				}
			}
		}

		// -- CURRENT_GROUP_CHANGED --

		final SourceGroup prevCurrent = previousState.getCurrentGroup();
		final SourceGroup curCurrent = state.getCurrentGroup();
		if ( !Objects.equals( prevCurrent, curCurrent ) )
		{
			if ( prevCurrent != null && state.getGroups().contains( prevCurrent ) )
				changedGroups.add( prevCurrent );
			if ( curCurrent != null )
				changedGroups.add( curCurrent );
		}

		// -- GROUP_ACTIVITY_CHANGED --

		for ( SourceGroup group : state.getGroups() )
		{
			if ( previousState.getGroups().contains( group ) )
			{
				final boolean wasActive = previousState.isGroupActive( group );
				final boolean isActive = state.isGroupActive( group );
				if ( wasActive != isActive )
					changedGroups.add( group );
			}
		}

		// -- SOURCE_TO_GROUP_ASSIGNMENT_CHANGED --

		final HashSet< SourceGroup > structurallyChangedGroups = new HashSet<>();
		for ( SourceGroup group : state.getGroups() )
		{
			if ( previousState.getGroups().contains( group ) )
			{
				final Set< SourceAndConverter< ? > > content = state.getSourcesInGroup( group );
				final Set< SourceAndConverter< ? > > previousContent = previousState.getSourcesInGroup( group );
				if ( !content.equals( previousContent ) )
					structurallyChangedGroups.add( group );
			}
		}

		// -- create corresponding TreeModelEvents --

		// groups added or removed
		if ( !addedGroups.isEmpty() )
		{
			final ArrayList< SourceGroup > list = new ArrayList<>( addedGroups );
			list.sort( state.groupOrder() );
			final int[] childIndices = new int[ list.size() ];
			Arrays.setAll( childIndices, i -> state.getGroups().indexOf( list.get( i ) ) );
			final Object[] children = list.toArray( new Object[ 0 ] );
			SwingUtilities.invokeLater( () -> fireTreeNodesInserted( new TreeModelEvent( this, rootPath, childIndices, children ) ) );
		}
		else if ( !removedGroups.isEmpty() )
		{
			final ArrayList< SourceGroup > list = new ArrayList<>( removedGroups );
			list.sort( previousState.groupOrder() );
			final int[] childIndices = new int[ list.size() ];
			Arrays.setAll( childIndices, i -> previousState.getGroups().indexOf( list.get( i ) ) );
			final Object[] children = list.toArray( new Object[ 0 ] );
			SwingUtilities.invokeLater( () -> fireTreeNodesRemoved( new TreeModelEvent( this, rootPath, childIndices, children ) ) );
		}

		// groups that change currentness, activeness, or name
		if ( !changedGroups.isEmpty() )
		{
			final ArrayList< SourceGroup > list = new ArrayList<>( changedGroups );
			list.sort( state.groupOrder() );
			final int[] childIndices = new int[ list.size() ];
			Arrays.setAll( childIndices, i -> state.getGroups().indexOf( list.get( i ) ) );
			final Object[] children = list.toArray( new Object[ 0 ] );
			SwingUtilities.invokeLater( () -> fireTreeNodesChanged( new TreeModelEvent( this, rootPath, childIndices, children ) ) );
		}

		// groups that had children added or removed
		if ( !structurallyChangedGroups.isEmpty() )
		{
			for ( SourceGroup group : structurallyChangedGroups )
			{
				final Object[] path = new Object[] { root, group };
				SwingUtilities.invokeLater( () -> fireTreeStructureChanged( new TreeModelEvent( this, path, null, null ) ) );
			}
		}

		previousState.set( state );
	}

	public TreePath getPathTo( final SourceGroup group )
	{
		return new TreePath( new Object[] { root, group } );
	}
}
