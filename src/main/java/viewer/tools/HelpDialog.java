package viewer.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

public class HelpDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates and displays a JFrame that lists the help file for the SPIM
	 * viewer UI.
	 */
	public HelpDialog( final Frame owner )
	{
		this( owner, HelpDialog.class.getResource( "/viewer/Help.html" ) );
	}

	public HelpDialog( final Frame owner, final URL helpFile )
	{
		super( owner, "Help", false );

		if ( helpFile == null )
		{
			System.err.println( "helpFile url is null." );
			return;
		}
		try
		{
			final JEditorPane editorPane = new JEditorPane( helpFile );
			editorPane.setEditable( false );
			editorPane.setBorder( BorderFactory.createEmptyBorder( 10, 0, 10, 10 ) );

			final JScrollPane editorScrollPane = new JScrollPane( editorPane );
			editorScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
			editorScrollPane.setPreferredSize( new Dimension( 800, 800 ) );

			getContentPane().add( editorScrollPane, BorderLayout.CENTER );

			final ActionMap am = getRootPane().getActionMap();
			final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
			final Object hideKey = new Object();
			final Action hideAction = new AbstractAction()
			{
				private static final long serialVersionUID = 6288745091648466880L;

				@Override
				public void actionPerformed( final ActionEvent e )
				{
					setVisible( false );
				}
			};
			im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
			am.put( hideKey, hideAction );

			pack();
			setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}
}
