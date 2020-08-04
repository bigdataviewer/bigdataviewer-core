/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.ui;

import bdv.ui.convertersetupeditor.ConverterSetupEditPanel;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.viewermodepanel.DisplaySettingsPanel;
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

/**
 * Default cards added to the card panel.
 * <ul>
 * <li>Display Modes</li>
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

	public static void setup( final CardPanel cards, final ViewerPanel viewer, final ConverterSetups converterSetups )
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
		scrollPaneTable.addMouseWheelListener( new MouseWheelScrollListener( scrollPaneTable ) );
		scrollPaneTable.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		tablePanel.add( scrollPaneTable, BorderLayout.CENTER );
		tablePanel.add( editPanelTable, BorderLayout.SOUTH );
		tablePanel.setPreferredSize( new Dimension( 300, 245 ) );

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
		scrollPaneTree.addMouseWheelListener( new MouseWheelScrollListener( scrollPaneTree ) );
		scrollPaneTree.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		treePanel.add( scrollPaneTree, BorderLayout.CENTER );
		treePanel.add( editPanelTree, BorderLayout.SOUTH );
		treePanel.setPreferredSize( new Dimension( 300, 225 ) );

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

		cards.addCard( DEFAULT_VIEWERMODES_CARD, "Display Modes", new DisplaySettingsPanel( viewer.state() ), true, new Insets( 0, 4, 4, 0 ) );
		cards.addCard( DEFAULT_SOURCES_CARD, "Sources", tablePanel, true, new Insets( 0, 4, 0, 0 ) );
		cards.addCard( DEFAULT_SOURCEGROUPS_CARD, "Groups", treePanel, true, new Insets( 0, 4, 0, 0 ) );
	}
}
