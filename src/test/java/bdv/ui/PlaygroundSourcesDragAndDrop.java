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

import bdv.ui.sourcetable.SourceTable;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.viewer.BasicViewerState;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeSelectionModel;

public class PlaygroundSourcesDragAndDrop
{
	public static void main( String[] args )
	{
		// create a ViewerState to show
		final BasicViewerState state = new BasicViewerState();
		final List< SourceAndConverter< ? > > sources = new ArrayList<>();
		for ( int i = 0; i < 10; ++i )
			sources.add( CreateViewerState.createSource() );
		state.addSources( sources );
		state.setSourceActive( sources.get( 1 ), true );
		state.setSourceActive( sources.get( 2 ), true );
		state.setSourceActive( sources.get( 7 ), true );

		final List< SourceGroup > groups = new ArrayList<>();
		for ( int i = 0; i < 10; ++i )
		{
			final SourceGroup g = new SourceGroup();
			groups.add( g );
			state.addGroup( g );
			state.setGroupName( g, "group(" + i + ")" );
		}
		state.setGroupActive( groups.get( 1 ), true );
		state.setGroupActive( groups.get( 2 ), true );
		state.setGroupActive( groups.get( 7 ), true );

		state.addSourceToGroup( sources.get( 0 ), groups.get( 0 ) );
		state.addSourceToGroup( sources.get( 1 ), groups.get( 0 ) );
		state.addSourceToGroup( sources.get( 2 ), groups.get( 0 ) );
		state.addSourceToGroup( sources.get( 3 ), groups.get( 3 ) );
		state.addSourceToGroup( sources.get( 8 ), groups.get( 3 ) );
		state.addSourceToGroup( sources.get( 4 ), groups.get( 3 ) );
		state.addSourceToGroup( sources.get( 2 ), groups.get( 3 ) );

		final ConverterSetups converterSetups = new ConverterSetups( state );
		for ( SourceAndConverter< ? > source : sources )
			converterSetups.put( source, CreateViewerState.getConverterSetup( source ) );

		// create the table
		SourceTable table = new SourceTable( state, converterSetups );
		table.setPreferredScrollableViewportSize( new Dimension( 400, 200 ) );
		table.setFillsViewportHeight( true );
		table.setDragEnabled( true );
		table.setDropTarget( null );

		// create the tree
		SourceGroupTree tree = new SourceGroupTree( state );
		tree.setEditable( true );
		tree.setSelectionRow( 0 );
		tree.setRootVisible( false );
		tree.setShowsRootHandles( true );
		tree.setExpandsSelectedPaths( true );
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );
//		tree.setDropMode( DropMode.ON );


		// handle focus
		table.addFocusListener( new FocusListener()
		{
			@Override
			public void focusGained( final FocusEvent e )
			{
				table.setSelectionBackground( true );
			}

			@Override
			public void focusLost( final FocusEvent e )
			{
				table.setSelectionBackground( false );
			}
		} );
		tree.addFocusListener( new FocusListener()
		{
			@Override
			public void focusGained( final FocusEvent e )
			{
				tree.setSelectionBackground( true );
			}

			@Override
			public void focusLost( final FocusEvent e )
			{
				tree.setSelectionBackground( false );
			}
		} );


		//Create and set up the window.
		JFrame frame = new JFrame( "Sources Table" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getRootPane().setDoubleBuffered( true );
		JScrollPane scrollPane = new JScrollPane( table );
		frame.add( scrollPane, BorderLayout.NORTH );
		frame.add( tree, BorderLayout.CENTER );
		frame.pack();
		frame.setVisible( true );
	}

}
