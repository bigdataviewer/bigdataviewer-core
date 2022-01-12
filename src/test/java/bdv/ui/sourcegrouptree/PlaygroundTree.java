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

import bdv.ui.CreateViewerState;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.GroupModel;
import bdv.viewer.BasicViewerState;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeSelectionModel;

public class PlaygroundTree
{
	public static void main( String[] args )
	{
//		try
//		{
//			UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());
//		}
//		catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
//		{
//			e.printStackTrace();
//		}

		// create a ViewerState to show
		final BasicViewerState state = new BasicViewerState();
		final List< SourceAndConverter< ? > > sources = new ArrayList<>();
		for ( int i = 0; i < 10; ++i )
			sources.add( CreateViewerState.createSource() );
		state.addSources( sources );

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

		// create the tree
		SourceGroupTree tree = new SourceGroupTree( state );
		tree.setEditable( true );
		tree.setSelectionRow( 0 );
		tree.setRootVisible( false );
		tree.setShowsRootHandles( true );
		tree.setExpandsSelectedPaths( true );
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );
//		tree.setToggleClickCount( 0 );
//		tree.setSelectionModel(  );

		JPanel buttons = new JPanel();
		buttons.setLayout( new BoxLayout( buttons, BoxLayout.Y_AXIS ) );
		final JButton toggle5Active = new JButton("a5");
		toggle5Active.addActionListener( e -> state.setGroupActive( groups.get( 5 ), !state.isGroupActive( groups.get( 5 ) ) ) );
		final JButton add5to3 = new JButton("5->3");
		add5to3.addActionListener( e -> state.addSourceToGroup( sources.get( 5 ), groups.get( 3 ) ) );
		final JButton remove02from0 = new JButton("0,2x>0");
		remove02from0.addActionListener( e ->
		{
			state.removeSourceFromGroup( sources.get( 0 ), groups.get( 0 ) );
			state.removeSourceFromGroup( sources.get( 2 ), groups.get( 0 ) );
		} );
		final JButton name4 = new JButton("rename4");
		name4.addActionListener( e -> state.setGroupName( groups.get( 4 ), "renamed group 4" ) );
		final JButton remove0 = new JButton("x0");
		remove0.addActionListener( e -> state.removeGroup( groups.get( 0 ) ) );
		final JButton remove4 = new JButton("x4");
		remove4.addActionListener( e -> state.removeGroup( groups.get( 4 ) ) );
		final JButton edit4 = new JButton("e4");
		edit4.addActionListener( e -> tree.startEditingAtPath( tree.getPathTo( groups.get( 4 ) ) ) );
		buttons.add( toggle5Active );
		buttons.add( add5to3 );
		buttons.add( remove02from0 );
		buttons.add( name4 );
		buttons.add( remove0 );
		buttons.add( remove4 );
		buttons.add( edit4 );

		//Create and set up the window.
		JFrame frame = new JFrame( "Groups Tree" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getRootPane().setDoubleBuffered( true );
		JScrollPane scrollPane = new JScrollPane( tree );
		frame.add( scrollPane, BorderLayout.CENTER );
		frame.add( buttons, BorderLayout.EAST );
		frame.pack();
		frame.setVisible( true );
	}
}
