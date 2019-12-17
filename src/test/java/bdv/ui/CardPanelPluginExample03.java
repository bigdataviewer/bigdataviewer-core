package bdv.ui;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.command.CommandService;

/**
 * Card panel example with plugin cards.
 * <p>
 * This example displays all cards which implement {@link HighPriorityCard}.
 * If an {@link bdv.ui.ExampleCardHighPriority.Age} parameter is needed, it
 * will be populated. This will add the {@link ExampleCardHighPriority}.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresden
 */
public class CardPanelPluginExample03
{

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "CardPanel Example" );
		frame.setLayout( new MigLayout( "fillx", "[]", "" ) );

		final Context context = new Context();
		final CommandService cs = context.service( CommandService.class );

		final CardPanel cardPanel = new CardPanel( cs );

		final Map< Class< ? >, Object > parameters = new HashMap<>();
		ExampleCardHighPriority.Age age = new ExampleCardHighPriority.Age( 42 );
		parameters.put( ExampleCardHighPriority.Age.class, age );

		cardPanel.addAll( HighPriorityCard.class, parameters );

		frame.setPreferredSize( new Dimension( 250, 300 ) );
		frame.add( cardPanel, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}
