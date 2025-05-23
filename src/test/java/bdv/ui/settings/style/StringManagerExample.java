/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.ui.settings.style;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SelectAndEditProfileSettingsPage;
import bdv.ui.settings.SelectAndEditProfileSettingsPage.ProfileEditPanel;
import bdv.ui.settings.SettingsPage;
import bdv.ui.settings.SingleSettingsPanel;
import org.scijava.listeners.Listeners;

public class StringManagerExample
{

	/*
	 * A "configurable profile" is a "named bundle of (configurable) settings".
	 * An example is configurable keymaps found in many programs: You can choose
	 * between one of several named keymaps, and you can also tweak the
	 * individual bindings in each keymap.
	 *
	 * To represent a configurable profile we need to implement the Style interface,
	 * which comprises methods to copy the style and get/set name.
	 * (AbstractStyle handles the name getting/setting.)
	 *
	 * In this example we want to make Greeting style which only comprises one
	 * setting: a greeting text. We only have to implement a copy() method,
	 * which makes a copy of these particular settings with a new name.
	 */
	static class Greeting extends AbstractStyle< Greeting >
	{
		public String text;

		public Greeting( final String name, final String text )
		{
			super( name );
			this.text = text;
		}

		@Override
		public Greeting copy( final String newName )
		{
			return new Greeting( newName, text );
		}
	}

	/*
	 * Next we need a StyleManager that manages a list of our Greeting styles.
	 *
	 * TODO: setting the active style
	 *
	 * TODO: handles builtin / user styles
	 *
	 * TODO: snapshots
	 */

	static class GreetingManager extends AbstractStyleManager< GreetingManager, Greeting >
	{
		@Override
		protected List< Greeting > loadBuiltinStyles()
		{
			return Arrays.asList(
					new Greeting( "hello_en", "Hello world!" ),
					new Greeting( "hello_de", "Hallo Welt!" ) );
		}

		@Override
		public void saveStyles()
		{
			System.out.println( "GreetingManager.saveStyles" );
		}
	}

	static class GreetingSettingsPage extends JPanel implements ProfileEditPanel< StyleProfile< Greeting > >
	{
		private final Listeners.List< ModificationListener > modificationListeners = new Listeners.SynchronizedList<>();

		private final JTextArea textArea;

		private boolean trackModifications = true;

		GreetingSettingsPage()
		{
			super( new BorderLayout() );
			textArea = new JTextArea();
			textArea.setPreferredSize( new Dimension( 400, 200 ) );
			add( new JLabel( "greeting text:" ), BorderLayout.NORTH );
			add( textArea, BorderLayout.CENTER );

			textArea.getDocument().addDocumentListener( new DocumentListener()
			{
				@Override
				public void removeUpdate( DocumentEvent e )
				{
					notifyModified();
				}

				@Override
				public void insertUpdate( DocumentEvent e )
				{
					notifyModified();
				}

				@Override
				public void changedUpdate( DocumentEvent arg0 )
				{
					notifyModified();
				}

				private void notifyModified()
				{
					if ( trackModifications )
						modificationListeners.list.forEach( ModificationListener::setModified );
				}
			} );
		}

		@Override
		public Listeners< ModificationListener > modificationListeners()
		{
			return modificationListeners;
		}

		@Override
		public void loadProfile( final StyleProfile< Greeting > profile )
		{
			trackModifications = false;
			textArea.setText( profile.getStyle().text );
			trackModifications = true;
		}

		@Override
		public void storeProfile( final StyleProfile< Greeting > profile )
		{
			profile.getStyle().text = textArea.getText();
		}

		@Override
		public JPanel getJPanel()
		{
			return this;
		}
	}

	public static void main( String[] args )
	{
		final GreetingManager greetingManager = new GreetingManager();
		final SettingsPage page = SelectAndEditProfileSettingsPage.forStyle(
				greetingManager,
				new GreetingManager(),
				new GreetingSettingsPage(),
				"greeting" );

		final JFrame frame = new JFrame( "Preferences" );
		frame.add( new SingleSettingsPanel( page ) );
		frame.pack();
		frame.setVisible( true );
	}

}
