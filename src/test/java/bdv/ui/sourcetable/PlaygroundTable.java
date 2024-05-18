/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.ui.sourcetable;

import bdv.ui.CreateViewerState;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.convertersetupeditor.ConverterSetupEditPanel;
import bdv.viewer.BasicViewerState;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class PlaygroundTable
{
	public static void main( final String[] args )
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
		state.setSourceActive( sources.get( 1 ), true );
		state.setSourceActive( sources.get( 2 ), true );
		state.setSourceActive( sources.get( 7 ), true );

		final ConverterSetups converterSetups = new ConverterSetups( state );
		for ( SourceAndConverter< ? > source : sources )
			converterSetups.put( source, CreateViewerState.getConverterSetup( source ) );


		// TODO: create the table
		final SourceTable table = new SourceTable( state, converterSetups );
		table.setPreferredScrollableViewportSize( new Dimension( 400, 800 ) );
		table.setFillsViewportHeight( true );
		table.setDragEnabled( true );

		final ConverterSetupEditPanel editPanel = new ConverterSetupEditPanel( table, converterSetups );

		//Create and set up the window.
		final JFrame frame = new JFrame( "Sources Table" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getRootPane().setDoubleBuffered( true );
		final JScrollPane scrollPane = new JScrollPane( table );
		frame.add( scrollPane, BorderLayout.CENTER );
		frame.add( editPanel, BorderLayout.SOUTH );
		frame.pack();
		frame.setVisible( true );
	}
}

