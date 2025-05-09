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
package bdv.ui.keymap;

import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SelectAndEditProfileSettingsPage;
import bdv.ui.settings.style.StyleProfile;
import bdv.ui.settings.style.StyleProfileManager;
import java.awt.Dimension;
import javax.swing.JPanel;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.VisualEditorPanel;

public class KeymapSettingsPage extends SelectAndEditProfileSettingsPage< StyleProfile< Keymap > >
{
	/**
	 * Creates a new settings page for Keymaps.
	 *
	 * @param treePath
	 *            path of this page in the settings tree.
	 * @param styleManager
	 *            the keymap manager.
	 * @param commandDescriptions
	 *            the command descriptions.
	 */
	public KeymapSettingsPage( final String treePath, final KeymapManager styleManager, final CommandDescriptions commandDescriptions )
	{
		this( treePath, styleManager, new KeymapManager(), commandDescriptions );
	}

	/**
	 * Creates a new settings page for Keymaps.
	 *
	 * @param treePath
	 *            path of this page in the settings tree.
	 * @param styleManager
	 *            the keymap manager.
	 * @param editingStyleManager
	 *            another keymap manager which will be used internally by the preferences page to hold changes that have not been applied yet.
	 * @param commandDescriptions
	 *            the command descriptions.
	 */
	public < M extends AbstractKeymapManager< M > > KeymapSettingsPage( final String treePath, final M styleManager, final M editingStyleManager, final CommandDescriptions commandDescriptions )
	{
		super(
				treePath,
				new StyleProfileManager<>( styleManager, editingStyleManager ),
				new KeymapProfileEditPanel( styleManager.getSelectedStyle(), commandDescriptions ) );
	}

	static class KeymapProfileEditPanel implements ProfileEditPanel< StyleProfile< Keymap > >
	{
		private final Listeners.SynchronizedList< ModificationListener > modificationListeners;

		private final Keymap editedStyle;

		private final VisualEditorPanel styleEditorPanel;

		public KeymapProfileEditPanel( final Keymap initialStyle, final CommandDescriptions commandDescriptions )
		{
			editedStyle = initialStyle.copy( "Edited" );
			styleEditorPanel = new VisualEditorPanel( editedStyle.getConfig(), commandDescriptions.createCommandDescriptionsMap() );
			styleEditorPanel.setButtonPanelVisible( false );
			modificationListeners = new Listeners.SynchronizedList<>();
			styleEditorPanel.modelChangedListeners().add( () -> {
				styleEditorPanel.modelToConfig();
				if ( trackModifications )
					modificationListeners.list.forEach( ModificationListener::setModified );
			} );
			styleEditorPanel.setPreferredSize( new Dimension( 200, 200 ) );
		}

		private boolean trackModifications = true;

		@Override
		public void loadProfile( final StyleProfile< Keymap > profile )
		{
			trackModifications = false;
			editedStyle.set( profile.getStyle() );
			styleEditorPanel.configToModel();
			trackModifications = true;
		}

		@Override
		public void storeProfile( final StyleProfile< Keymap > profile )
		{
			trackModifications = false;
			styleEditorPanel.modelToConfig();
			editedStyle.setName( profile.getStyle().getName() );
			trackModifications = true;
			profile.getStyle().set( editedStyle );
		}

		@Override
		public Listeners< ModificationListener > modificationListeners()
		{
			return modificationListeners;
		}

		@Override
		public JPanel getJPanel()
		{
			return styleEditorPanel;
		}
	}
}
