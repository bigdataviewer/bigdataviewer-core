package bdv.ui;

import bdv.ui.iconcards.options.AnnotatedToggleGroupButton;
import bdv.ui.iconcards.options.ToggleGroupButton;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

public class AnnotatedToggleGroupButtonExample
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

		final List< String > annotations = new ArrayList<>(  );
		annotations.add( "Rot. On" );
		annotations.add( "Rot. Off" );

		final AnnotatedToggleGroupButton button = new AnnotatedToggleGroupButton( icons, labels, actions, annotations );
		button.setAnnotationFont( new Font( button.getFont().getName(), Font.TRUETYPE_FONT, 9 ) );

		button.addOption( new ImageIcon( translation_on ), "Translation On", () -> System.out.println( "Translation On" ), "Trans. On" );
		button.addOption( new ImageIcon( translation_off ), "Uups", () -> System.out.println( "uups" ), "Ups" );
		button.removeOption( "Uups" );
		button.addOption( new ImageIcon( translation_off ), "Translation Off", () -> System.out.println( "Translation Off" ), "Trans. Off" );

		button.setPreferredSize( new Dimension( 40, 40 ) );
		frame.add( button, "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );
	}
}

