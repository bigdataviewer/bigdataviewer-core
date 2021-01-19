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
package bdv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.tree.TreeSelectionModel;

import bdv.ui.convertersetupeditor.ConverterSetupEditPanel;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.viewermodepanel.DisplaySettingsPanel;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.ConverterSetups;
import bdv.viewer.ViewerState;

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

	public static void setup( final CardPanel cards, final AbstractViewerPanel viewer, final ConverterSetups converterSetups )
	{
		final ViewerState state = viewer.state();

		// -- Sources table --
		final SourceTable table = new SourceTable( state, converterSetups, viewer.getInputTriggerConfig() );
		table.setPreferredScrollableViewportSize( new Dimension( 300, 200 ) );
		table.setFillsViewportHeight( true );
		table.setDragEnabled( true );
		final ConverterSetupEditPanel editPanelTable = new ConverterSetupEditPanel( table, converterSetups );
		final JPanel tablePanel = new JPanel( new BorderLayout() );
		final JScrollPane scrollPaneTable = new MyScrollPane( table, "Table.background" );
		scrollPaneTable.addMouseWheelListener( new MouseWheelScrollListener( scrollPaneTable ) );
		tablePanel.add( scrollPaneTable, BorderLayout.CENTER );
		tablePanel.add( editPanelTable, BorderLayout.SOUTH );
		tablePanel.setPreferredSize( new Dimension( 300, 245 ) );

		// -- Groups tree --
		final SourceGroupTree tree = new SourceGroupTree( state, viewer.getInputTriggerConfig() );
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
		final JScrollPane scrollPaneTree = new MyScrollPane( tree, "Tree.background" );
		scrollPaneTree.addMouseWheelListener( new MouseWheelScrollListener( scrollPaneTree ) );
		treePanel.add( scrollPaneTree, BorderLayout.CENTER );
		treePanel.add( editPanelTree, BorderLayout.SOUTH );
		treePanel.setPreferredSize( new Dimension( 300, 225 ) );

		cards.addCard( DEFAULT_VIEWERMODES_CARD, "Display Modes", new DisplaySettingsPanel( viewer.state() ), true, new Insets( 0, 4, 4, 0 ) );
		cards.addCard( DEFAULT_SOURCES_CARD, "Sources", tablePanel, true, new Insets( 0, 0, 0, 0 ) );
		cards.addCard( DEFAULT_SOURCEGROUPS_CARD, "Groups", treePanel, true, new Insets( 0, 0, 0, 0 ) );
	}

	static class MyScrollPane extends JScrollPane
	{
		private final String bgColorName;

		public MyScrollPane( final Component view, final String bgColorName )
		{
			super( view );
			this.bgColorName = bgColorName;
			updateBorder();
		}

		@Override
		public void updateUI()
		{
			super.updateUI();
			if ( bgColorName != null )
				updateBorder();
		}

		private void updateBorder()
		{
			setBorder( new MatteBorder( 0, 4, 0, 0, UIManager.getColor( bgColorName ) ) );
		}
	}
}
