/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.tools;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

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
			editorScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
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
			setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}
}
