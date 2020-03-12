package bdv.ui;

import bdv.ui.iconcards.options.ToggleGroupButton;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

public class ToggleGroupButtonExample
{
	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "ToggleGroupButton Example" );
		frame.setLayout( new MigLayout( "ins 0, fillx, filly", "[grow]", "" ) );

		final Image rotation_on = new ImageIcon( frame.getClass().getResource( "/bdv/ui/viewermodepanel/rotation_on.png" ) ).getImage().getScaledInstance( 40, 40, Image.SCALE_SMOOTH );
		final Image rotation_off = new ImageIcon( frame.getClass().getResource( "/bdv/ui/viewermodepanel/rotation_off.png" ) ).getImage().getScaledInstance( 40, 40, Image.SCALE_SMOOTH );
		final Image translation_on = new ImageIcon( frame.getClass().getResource( "/bdv/ui/viewermodepanel/translation_on.png" ) ).getImage().getScaledInstance( 40, 40, Image.SCALE_SMOOTH );
		final Image translation_off = new ImageIcon( frame.getClass().getResource( "/bdv/ui/viewermodepanel/translation_off.png" ) ).getImage().getScaledInstance( 40, 40, Image.SCALE_SMOOTH );

		final List< Icon > icons = new ArrayList<>();
		icons.add( new ImageIcon( rotation_on ) );
		icons.add( new ImageIcon( rotation_off ) );
		final List< String > labels = new ArrayList<>();
		labels.add( "Rotation On" );
		labels.add( "Rotation Off" );
		final List< Runnable > actions = new ArrayList<>();
		actions.add( () -> System.out.println( "Rotation On" ) );
		actions.add( () -> System.out.println( "Rotation Off" ) );

		final ToggleGroupButton button = new ToggleGroupButton( icons, labels, actions );

		button.addOption( new ImageIcon( translation_on ), "Translation On", () -> System.out.println( "Translation On" ) );
		button.addOption( new ImageIcon( translation_off ), "Uups", () -> System.out.println( "uups" ) );
		button.removeOption( "Uups" );
		button.addOption( new ImageIcon( translation_off ), "Translation Off", () -> System.out.println( "Translation Off" ) );

		button.setPreferredSize( new Dimension( 40, 40 ) );
		frame.add( button, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}

