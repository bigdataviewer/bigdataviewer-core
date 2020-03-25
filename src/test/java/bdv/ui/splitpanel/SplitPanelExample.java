package bdv.ui.splitpanel;

import bdv.cache.CacheControl;
import bdv.ui.CardPanel;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

public class SplitPanelExample
{
	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "SplitPanel Example" );
		frame.setLayout( new MigLayout( "ins 0, fillx, filly", "[grow]", "" ) );

		final CardPanel cardPanel = new CardPanel();
		final ViewerPanel viewerPanel = new ViewerPanel( new ArrayList<>(), 1, new CacheControl.Dummy(), ViewerOptions.options().width( 600 ) );

		final SplitPanel splitPanel = new SplitPanel( viewerPanel, cardPanel );
		splitPanel.setBorder( null );

		frame.setPreferredSize( new Dimension( 800, 600 ) );
		frame.add( splitPanel, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}
