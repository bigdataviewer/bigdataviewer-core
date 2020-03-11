package bdv.ui;

import bdv.ui.iconcards.options.ToggleGroupButton;
import java.awt.Dimension;
import java.awt.Image;
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

		final ToggleGroupButton button = new ToggleGroupButton( new Icon[] { new ImageIcon( rotation_on ), new ImageIcon( rotation_off ), new ImageIcon( translation_on ), new ImageIcon( translation_off ) },
				new String[] { "Rotation On", "Rotation Off", "Translation On", "Translation Off" },
				new Runnable[] { () -> System.out.println( "Rotation On" ), () -> System.out.println( "Rotation Off" ), () -> System.out.println( "Translation On" ), () -> System.out.println( "Translation Off" ) } );

		button.setPreferredSize( new Dimension( 40, 40 ) );
		frame.add( button, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}

