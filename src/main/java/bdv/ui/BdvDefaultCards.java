package bdv.ui;

import bdv.ui.convertersetupeditor.ConverterSetupEditPanel;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.viewermodepanel.ViewerModesPanel;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeSelectionModel;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * Default cards added to the card panel.
 * <ul>
 * <li>SourceTable with ConverterSetup editor</li>
 * <li>SourceGroupTree with ConverterSetup editor</li>
 * </ul>
 *
 * @author Tobias Pietzsch
 */
public class BdvDefaultCards
{
	public static final String DEFAULT_SOURCES_CARD = "default bdv sources card";

	public static final String DEFAULT_SOURCEGROUPS_CARD = "default bdv groups card";

	public static final String DEFAULT_VIEWERMODES_CARD = "default bdv viewer modes card";

	public static void setup( final CardPanel cards, final ViewerPanel viewer, final ConverterSetups converterSetups, final TriggerBehaviourBindings triggerbindings )
	{
		final SynchronizedViewerState state = viewer.state();

		// -- Sources table --
		final SourceTable table = new SourceTable( state, converterSetups, viewer.getOptionValues().getInputTriggerConfig() );
		table.setPreferredScrollableViewportSize( new Dimension( 300, 200 ) );
		table.setFillsViewportHeight( true );
		table.setDragEnabled( true );
		final ConverterSetupEditPanel editPanelTable = new ConverterSetupEditPanel( table, converterSetups );
		final JPanel tablePanel = new JPanel( new BorderLayout() );
		final JScrollPane scrollPaneTable = new JScrollPane( table );
		scrollPaneTable.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		tablePanel.add( scrollPaneTable, BorderLayout.CENTER );
		tablePanel.add( editPanelTable, BorderLayout.SOUTH );
		tablePanel.setPreferredSize( new Dimension( 300, 230 ) );

		// -- Groups tree --
		SourceGroupTree tree = new SourceGroupTree( state, viewer.getOptionValues().getInputTriggerConfig() );
//		tree.setPreferredSize( new Dimension( 300, 200 ) );
		tree.setVisibleRowCount( 10 );
		tree.setEditable( true );
		tree.setSelectionRow( 0 );
		tree.setRootVisible( false );
		tree.setShowsRootHandles( true );
		tree.setExpandsSelectedPaths( true );
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );
		final ConverterSetupEditPanel editPanelTree = new ConverterSetupEditPanel( tree, converterSetups );
		final JPanel treePanel = new JPanel( new BorderLayout() );
		final JScrollPane scrollPaneTree = new JScrollPane( tree );
		scrollPaneTree.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		treePanel.add( scrollPaneTree, BorderLayout.CENTER );
		treePanel.add( editPanelTree, BorderLayout.SOUTH );
		treePanel.setPreferredSize( new Dimension( 300, 215 ) );

		// -- handle focus --
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener( "focusOwner", new PropertyChangeListener()
		{
			static final int MAX_DEPTH = 8;
			boolean tableFocused;
			boolean treeFocused;

			void focusTable( boolean focus )
			{
				if ( focus != tableFocused )
				{
					tableFocused = focus;
					table.setSelectionBackground( focus );
				}
			}

			void focusTree( boolean focus )
			{
				if ( focus != treeFocused )
				{
					treeFocused = focus;
					tree.setSelectionBackground( focus );
				}
			}

			@Override
			public void propertyChange( final PropertyChangeEvent evt )
			{
				if ( evt.getNewValue() instanceof JComponent )
				{
					JComponent component = ( JComponent ) evt.getNewValue();
					for ( int i = 0; i < MAX_DEPTH; ++i )
					{
						Container parent = component.getParent();
						if ( ! ( parent instanceof JComponent ) )
							break;

						component = ( JComponent ) parent;
						if ( component == treePanel )
						{
							focusTable( false );
							focusTree( true );
							return;
						}
						else if ( component == tablePanel )
						{
							focusTable( true );
							focusTree( false );
							return;
						}
					}
					focusTable( false );
					focusTree( false );
				}
			}
		} );

		cards.addCard( DEFAULT_VIEWERMODES_CARD, "Viewer Modes", new ViewerModesPanel( viewer.state(), triggerbindings ), true, new Insets( 0, 4, 0, 0 ) );
		cards.addCard( DEFAULT_SOURCES_CARD, "Sources", tablePanel, true, new Insets( 0, 4, 0, 0 ) );
		cards.addCard( DEFAULT_SOURCEGROUPS_CARD, "Groups", treePanel, true, new Insets( 0, 4, 0, 0 ) );
	}
}
