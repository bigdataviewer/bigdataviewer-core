package bdv.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Card panel example.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresdens
 */
public class CardPanelExample
{

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "CardPanel Example" );
		frame.setLayout( new MigLayout( "fillx", "[]", "" ) );
		final JButton add = new JButton( "Add Card" );
		final JButton remove = new JButton( "Remove Card" );
		final JButton toggle = new JButton( "Toggle Card" );
		frame.add( add, "growx, wrap" );
		frame.add( remove, "growx, wrap" );
		frame.add( toggle, "growx, wrap" );

		final CardPanel cardPanel = new CardPanel( null );

		frame.setPreferredSize( new Dimension( 250, 300 ) );
		frame.add( cardPanel, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );

		final Random rand = new Random();
		final List< String > names = new ArrayList<>();

		add.addActionListener( e -> {
			final String name = "Card " + rand.nextInt();
			cardPanel.addCard( name, new JLabel( "Conent " + rand.nextFloat() ), rand.nextBoolean() );
			names.add( name );
			cardPanel.revalidate();
		} );
		remove.addActionListener( e -> {
			if ( names.size() > 0 )
			{
				final int idx = rand.nextInt( names.size() );
				cardPanel.removeCard( names.get( idx ) );
				names.remove( idx );
				cardPanel.revalidate();
			}
		} );
		toggle.addActionListener( e -> {
			if ( names.size() > 0 )
			{
				final int idx = rand.nextInt( names.size() );
				cardPanel.setCardOpen( names.get( idx ), !cardPanel.isCardOpen( names.get( idx ) ) );
				cardPanel.revalidate();
			}
		} );
	}
}
