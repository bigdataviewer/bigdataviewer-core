/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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

import bdv.ui.SourcesTransferable;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.GroupModel;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.SourceModel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import bdv.viewer.ViewerState;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

/**
 * A {@code JTree} that shows the source-groups of a {@link ViewerState} and
 * allows to modify activeness and currentness, to add/remove sources to/from
 * groups, and to add/remove/rename groups.
 *
 * @author Tobias Pietzsch
 */
public class SourceGroupTree extends JTree
{
	private final ViewerState state;

	private final SourceGroupTreeModel model;

	private final SourceGroupTreeCellRenderer renderer;

	private final SourceGroupEditor editor;

	public SourceGroupTree( final ViewerState state )
	{
		this( state, new InputTriggerConfig() );
	}

	public SourceGroupTree( final ViewerState state, final InputTriggerConfig inputTriggerConfig )
	{
		this.state = state;
		model = new SourceGroupTreeModel( state );
		setModel( model );

		renderer = new SourceGroupTreeCellRenderer();
		setCellRenderer( renderer );

		editor = new SourceGroupEditor( this, renderer );
		setCellEditor( editor );

		setOpaque( false );

		setTransferHandler( new SourceGroupTreeTransferHandler( this, state ) );

		this.installActions( inputTriggerConfig );
	}

	public void setSelectionBackground( final boolean hasFocus )
	{
		renderer.setBackgroundSelectionColor( hasFocus );
		this.repaint();
	}

	@Override
	public void updateUI()
	{
		if ( renderer != null )
			renderer.updateUI();
		super.updateUI();
	}

	/**
	 * Get list of sources in selected groups.
	 *
	 * TODO how should they be ordered?
	 */
	public List< SourceAndConverter< ? > > getSelectedSources()
	{
		final List< SourceAndConverter< ? > > sources = new ArrayList<>();
		for ( final SourceGroup group : getSelectedGroups() )
			for ( final SourceAndConverter< ? > source : state.getSourcesInGroup( group ) )
				if ( !sources.contains( source ) )
					sources.add( source );
		sources.sort( state.sourceOrder() );
		return sources;
	}

	private void installActions( final InputTriggerConfig inputTriggerConfig )
	{
		final InputActionBindings keybindings = InputActionBindings.installNewBindings( this, JComponent.WHEN_FOCUSED, false );
		final Actions actions = new Actions( inputTriggerConfig, "bdv" );
		actions.install( keybindings, "source groups tree" );
		actions.runnableAction( () -> toggleSelectedActive(), "toggle active", "A" );
		actions.runnableAction( () -> makeSelectedActive( true ), "set active", "not mapped" );
		actions.runnableAction( () -> makeSelectedActive( false ), "set inactive", "not mapped" );
		actions.runnableAction( () -> cycleSelectedCurrent(), "cycle current", "C" );
		actions.runnableAction( () -> removeSelected(), "remove sources or groups", "DELETE", "BACK_SPACE" );
		actions.runnableAction( () -> editGroupName(), "edit name", "ENTER" );
	}

	private void makeSelectedActive( final boolean active )
	{
		final List< SourceGroup > selectedGroups = getSelectedGroups();
		state.setGroupsActive( selectedGroups, active );
	}

	private void toggleSelectedActive()
	{
		final List< SourceGroup > selectedGroups = getSelectedGroups();
		if ( !selectedGroups.isEmpty() )
			state.setGroupsActive( selectedGroups, !state.isGroupActive( selectedGroups.get( 0 ) ) );
	}

	private void cycleSelectedCurrent()
	{
		final List< SourceGroup > selectedGroups = getSelectedGroups();
		if ( !selectedGroups.isEmpty() )
		{
			final SourceGroup current = state.getCurrentGroup();
			final int i = ( selectedGroups.indexOf( current ) + 1 ) % selectedGroups.size();
			state.setCurrentGroup( selectedGroups.get( i ) );
		}
	}

	private void removeSelected()
	{
		final Map< SourceGroup, List< SourceAndConverter< ? > > > groupToSelectedSources = getSelectedSourcesInGroups();
		final List< SourceGroup > selectedGroups = getSelectedGroups();

		for ( final SourceGroup group : selectedGroups )
			groupToSelectedSources.remove( group );
		groupToSelectedSources.forEach( ( group, sources ) -> state.removeSourcesFromGroup( sources, group ) );

		state.removeGroups( selectedGroups );
	}

	private void editGroupName()
	{
		final TreePath path = this.getLeadSelectionPath();
		if ( path != null )
		{
			final Object obj = path.getLastPathComponent();
			if ( obj instanceof GroupModel )
				startEditingAtPath( path );
		}
	}

	private List< SourceGroup > getSelectedGroups()
	{
		final List< SourceGroup > selectedGroups = new ArrayList<>();
		final TreePath[] selectionPaths = getSelectionPaths();
		if ( selectionPaths != null )
			for ( final TreePath path : selectionPaths )
			{
				final Object obj = path.getLastPathComponent();
				if ( obj instanceof GroupModel )
					selectedGroups.add( ( ( GroupModel ) obj ).getGroup() );
			}
		return selectedGroups;
	}

	/**
	 * Get all sources that are directly selected in the tree.
	 *
	 * @return map from group to list of directly selected sources under the group
	 */
	private Map< SourceGroup, List< SourceAndConverter< ? > > > getSelectedSourcesInGroups()
	{
		final Map< SourceGroup, List< SourceAndConverter< ? > > > selected = new HashMap<>();
		for ( final TreePath path : getSelectionPaths() )
		{
			final Object obj = path.getLastPathComponent();
			if ( obj instanceof SourceModel )
			{
				final SourceGroup group = ( ( GroupModel ) path.getParentPath().getLastPathComponent() ).getGroup();
				final SourceAndConverter< ? > source = ( ( SourceModel ) obj ).getSource();
				selected.computeIfAbsent( group, g -> new ArrayList<>() ).add( source );
			}
		}
		return selected;
	}

	@Override
	public String convertValueToText( final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus )
	{
		if ( value instanceof GroupModel )
		{
			final String groupName = ( ( GroupModel ) value ).getName();
			if ( groupName != null )
				return groupName;
		}
		return "";
	}

	// -- Process clicks on active and current checkboxes --
	// These clicks are consumed, because they should not cause selection changes, etc, in the tree.

	private Point pressedAt;
	private boolean consumeNext = false;
	private long releasedWhen = 0;

	@Override
	protected void processMouseEvent( final MouseEvent e )
	{
		MouseEvent modifiedMouseEvent = e;

		if ( e.getModifiers() == InputEvent.BUTTON1_MASK )
		{
			if ( e.getID() == MouseEvent.MOUSE_PRESSED )
			{
				pressedAt = e.getPoint();
				int x = e.getX();
				int y = e.getY();
				final TreePath path = getPathForLocation( x, y );

				if ( path != null )
				{
					final Rectangle bounds = getPathBounds( path );

					if ( e.getX() >= bounds.getX() + bounds.getWidth() )
					{
						modifiedMouseEvent = new MouseEvent(
								( Component ) e.getSource(), e.getID(), e.getWhen(), e.getModifiersEx(),
								( int ) ( bounds.getX() + bounds.getWidth() - 1 ), e.getY(),
								0, 0, e.getClickCount(), e.isPopupTrigger(), e.getButton() );
					}

					if ( path.getLastPathComponent() instanceof GroupModel )
					{
						x -= bounds.getX();
						y -= bounds.getY();
						final boolean currentHit = renderer.currentHit( x, y );
						final boolean activeHit = !currentHit && renderer.activeHit( x, y );
						if ( currentHit || activeHit )
						{
							if ( isPathSelected( path )  )
							{
								e.consume();
								consumeNext = true;
							}
						}
					}
				}
			}
			else if ( e.getID() == MouseEvent.MOUSE_RELEASED )
			{
				if ( consumeNext )
				{
					releasedWhen = e.getWhen();
					consumeNext = false;
					e.consume();
				}

				if ( pressedAt == null )
					return;

				final Point point = e.getPoint();
				if ( point.distanceSq( pressedAt ) > 2 )
					return;

				int x = e.getX();
				int y = e.getY();
				final TreePath path = getPathForLocation( x, y );
				if ( path != null && path.getLastPathComponent() instanceof GroupModel )
				{
					final Rectangle bounds = getPathBounds( path );
					x -= bounds.getX();
					y -= bounds.getY();
					final boolean currentHit = renderer.currentHit( x, y );
					final boolean activeHit = !currentHit && renderer.activeHit( x, y );
					if ( currentHit || activeHit )
					{
						final SourceGroup group = ( ( GroupModel ) path.getLastPathComponent() ).getGroup();
						if ( currentHit )
							state.setCurrentGroup( group );
						else
						{
							if ( isPathSelected( path ) )
								state.setGroupsActive( getSelectedGroups(), !state.isGroupActive( group ) );
							else
								state.setGroupActive( group, !state.isGroupActive( group ) );
						}
					}
				}
			}
			else if ( e.getID() == MouseEvent.MOUSE_CLICKED )
			{
				if ( e.getWhen() == releasedWhen )
					e.consume();
			}
		}

		super.processMouseEvent( modifiedMouseEvent );
	}

	@Override
	public TreePath getPathForLocation( final int x, final int y )
	{
		final TreePath closestPath = getClosestPathForLocation( x, y );

		if ( closestPath != null )
		{
			final Rectangle pathBounds = getPathBounds( closestPath );

			if ( pathBounds != null &&
					x >= pathBounds.x &&
					y >= pathBounds.y && y < ( pathBounds.y + pathBounds.height ) )
				return closestPath;
		}
		return null;
	}

	@Override
	public boolean isPathEditable( final TreePath path )
	{
		return path != null && path.getLastPathComponent() instanceof GroupModel;
	}

	@Override
	public void paintComponent( final Graphics g )
	{
		g.setColor( getBackground() );
		g.fillRect( 0, 0, getWidth(), getHeight() );
		final int[] rows = getSelectionRows();
		if ( rows != null )
		{
			g.setColor( renderer.getBackgroundSelectionColor() );
			for ( final int i : rows )
			{
				final Rectangle r = getRowBounds( i );
				g.fillRect( 0, r.y, getWidth(), r.height );
			}
		}
		super.paintComponent( g );
	}

	public TreePath getPathTo( final SourceGroup group )
	{
		synchronized ( state )
		{
			if ( state.containsGroup( group ) )
				return model.getPathTo( new GroupModel( group, state ) );
			else
				return null;
		}
	}

	/**
	 * {@code TransferHandler} for transferring sources into a group of a {@code SourceGroupTree} via cut/copy/paste and drag and drop.
	 */
	public static class SourceGroupTreeTransferHandler extends TransferHandler
	{
		private final SourceGroupTree tree;

		private final ViewerState state;

		public SourceGroupTreeTransferHandler( final SourceGroupTree tree, final ViewerState state )
		{
			this.tree = tree;
			this.state = state;
		}

		@Override
		public boolean canImport( final TransferSupport support )
		{
			final boolean canDrop = support.isDataFlavorSupported( SourcesTransferable.flavor );
			support.setShowDropLocation( canDrop );
			return canDrop;
		}

		@Override
		public boolean importData( final TransferSupport support )
		{
			if ( !canImport( support ) )
				return false;

			try
			{
				final Transferable t = support.getTransferable();
				final List< SourceAndConverter< ? > > sources = ( ( SourcesTransferable.SourceList ) t.getTransferData( SourcesTransferable.flavor ) ).getSources();

				final DropLocation dropLocation = support.getDropLocation();
				if ( dropLocation instanceof JTree.DropLocation )
				{
					final JTree.DropLocation drop = ( JTree.DropLocation ) dropLocation;

					final TreePath path = drop.getPath();
					if ( path == null )
					{
						final SourceGroup group = new SourceGroup();
						state.addGroup( group );
						state.setGroupName( group, "new group" );
						state.addSourcesToGroup( sources, group );
						SwingUtilities.invokeLater( () -> {
							final TreePath path1 = tree.getPathTo( group );
							tree.expandPath( path1 );
							tree.startEditingAtPath( path1 );
						} );
					}
					else
					{
						final GroupModel groupModel = ( GroupModel ) path.getPathComponent( 1 );
						state.addSourcesToGroup( sources, groupModel.getGroup() );
						SwingUtilities.invokeLater( () -> {
							tree.expandPath( tree.getPathTo( groupModel.getGroup() ) );
						} );
					}
					return true;
				}
			}
			catch ( final Exception e )
			{}
			return false;
		}
	}
}
