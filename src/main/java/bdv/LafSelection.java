package bdv;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class LafSelection
{
	public static class LafDialog extends JDialog
	{
		private final List< Component > components = new CopyOnWriteArrayList<>();

		public LafDialog()
		{
			super( ( Frame ) null, "Look And Feel" );
			final JPanel panel = new JPanel();
			panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );

			for ( UIManager.LookAndFeelInfo feel : UIManager.getInstalledLookAndFeels() )
			{
				System.out.println( "feel = " + feel.getName() );
				final JButton button = new JButton( feel.getName() );
				panel.add( button );
				button.addActionListener( e -> setLookAndFeel( feel ) );
			}

			add( panel );
			pack();

			components.add( this );
		}

		public void addComponent( Component component )
		{
			components.add( component );
		}

		private void setLookAndFeel( final UIManager.LookAndFeelInfo feel )
		{
			try
			{
				UIManager.setLookAndFeel( feel.getClassName() );
				for ( Component component : components )
				{
					SwingUtilities.updateComponentTreeUI( component );
//					if ( component instanceof Window )
//						( ( Window ) component ).pack();
				}
			}
			catch ( Exception ex )
			{
				System.err.println( "Failed to initialize LaF" );
			}
		}
	}

	public static void main( String[] args )
	{
		FlatLightLaf.installLafInfo();
		FlatDarkLaf.installLafInfo();
		FlatDarculaLaf.installLafInfo();
		FlatIntelliJLaf.installLafInfo();

		final JDialog dialog = new LafDialog();
		dialog.setVisible( true );
	}
}
