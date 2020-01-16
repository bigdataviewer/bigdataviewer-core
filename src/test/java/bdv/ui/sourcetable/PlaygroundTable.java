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

