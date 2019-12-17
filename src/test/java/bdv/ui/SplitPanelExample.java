package bdv.ui;

import bdv.cache.CacheControl;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.command.CommandService;

public class SplitPanelExample
{
	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "SplitPanel Example" );
		frame.setLayout( new MigLayout( "fillx, filly", "[grow]", "" ) );

		final Context context = new Context();
		final CardPanel cardPanel = new CardPanel(context.service( CommandService.class ));
		context.inject( cardPanel );
		cardPanel.addAll( DiscoverableCard.class, new HashMap<>() );

		final ViewerPanel viewerPanel = new ViewerPanel( new ArrayList<>(), 1, new CacheControl.Dummy(), ViewerOptions.options().width( 600 ) );

		final SplitPanel splitPanel = new SplitPanel( viewerPanel, cardPanel );

		frame.setPreferredSize( new Dimension( 800, 600 ) );
		frame.add( splitPanel, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}
