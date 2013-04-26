package viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

public class HelpFrame
{
	private static JFrame frame = null;

	/**
	 * Instantiates and displays a JFrame that lists the help file for the SPIM viewer UI.   
	 */
	public HelpFrame()
	{
		this( HelpFrame.class.getResource( "Help.html" ) );
	}
	
	/**
	 * Instantiates and displays a JFrame that lists the content of a html file
	 * specified by its {@link URL}.   
	 */
	public HelpFrame(URL helpFile)
	{
		if ( frame != null )
			frame.toFront();
		else
		{
			try
			{
				frame = new JFrame( "Help" );
				final JEditorPane editorPane = new JEditorPane( helpFile );
				editorPane.setEditable( false );
				editorPane.setBorder( BorderFactory.createEmptyBorder( 10, 0, 10, 10 ) );

				final JScrollPane editorScrollPane = new JScrollPane( editorPane );
				editorScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
				editorScrollPane.setPreferredSize( new Dimension( 800, 800 ) );

				final Container content = frame.getContentPane();
				content.add( editorScrollPane, BorderLayout.CENTER );

				frame.addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosing( final WindowEvent e )
					{
						frame = null;
					}
				} );

				final ActionMap am = frame.getRootPane().getActionMap();
				final InputMap im = frame.getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
				final Object hideKey = new Object();
				final Action hideAction = new AbstractAction()
				{
					private static final long serialVersionUID = 6288745091648466880L;

					@Override
					public void actionPerformed( final ActionEvent e )
					{
						frame.dispose();
						frame = null;
					}
				};
				im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
				am.put( hideKey, hideAction );

				frame.pack();
				frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
				frame.setVisible( true );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}
}
