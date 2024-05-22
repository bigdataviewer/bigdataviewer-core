/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.util;

import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.UIUtils;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.splitpanel.SplitPanel;
import bdv.viewer.ConverterSetups;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BigDataViewer;
import bdv.BigDataViewerActions;
import bdv.cache.CacheControl.CacheControls;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.DisplayMode;
import bdv.viewer.NavigationActions;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.TransformEventHandler;

import static bdv.BigDataViewerActions.COLLAPSE_CARDS;
import static bdv.BigDataViewerActions.COLLAPSE_CARDS_KEYS;
import static bdv.BigDataViewerActions.EXPAND_CARDS;
import static bdv.BigDataViewerActions.EXPAND_CARDS_KEYS;

public class BdvHandlePanel extends BdvHandle
{
	private final BrightnessDialog brightnessDialog;

	private final VisibilityAndGroupingDialog activeSourcesDialog;

	private final ManualTransformationEditor manualTransformationEditor;

	private final Bookmarks bookmarks;

	private final BookmarksEditor bookmarksEditor;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final KeymapManager keymapManager;

	private final AppearanceManager appearanceManager;

	public BdvHandlePanel( final Frame dialogOwner, final BdvOptions options )
	{
		super( options );
		UIUtils.installFlatLafInfos();

		final KeymapManager optionsKeymapManager = options.values.getKeymapManager();
		final AppearanceManager optionsAppearanceManager = options.values.getAppearanceManager();
		keymapManager = optionsKeymapManager != null ? optionsKeymapManager : new KeymapManager( BigDataViewer.configDir );
		appearanceManager = optionsAppearanceManager != null ? optionsAppearanceManager : new AppearanceManager( BigDataViewer.configDir );

		cacheControls = new CacheControls();

		viewer = new ViewerPanel( new ArrayList<>(), 1, cacheControls, options.values.getViewerOptions() );
		if ( !options.values.hasPreferredSize() )
			viewer.getDisplay().setPreferredSize( null );
		viewer.getDisplay().addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				tryInitTransform();
			}
		} );

		setupAssignments = new SetupAssignments( new ArrayList<>(), 0, 65535 );
		setups = new ConverterSetups( viewer.state() );
		setups.listeners().add( s -> viewer.requestRepaint() );

		cards = new CardPanel();
		BdvDefaultCards.setup( cards, viewer, setups );
		splitPanel = new SplitPanel( viewer, cards );

		keybindings = new InputActionBindings();
		SwingUtilities.replaceUIActionMap( viewer, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( viewer, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		triggerbindings = new TriggerBehaviourBindings();
		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

		final InputTriggerConfig inputTriggerConfig = viewer.getInputTriggerConfig();

		// TODO: should be a field?
		final Behaviours transformBehaviours = new Behaviours( inputTriggerConfig, "bdv" );
		transformBehaviours.install( triggerbindings, "transform" );

		final TransformEventHandler tfHandler = viewer.getTransformEventHandler();
		tfHandler.install( transformBehaviours );

		manualTransformationEditor = new ManualTransformationEditor( viewer, keybindings );

		bookmarks = new Bookmarks();
		bookmarksEditor = new BookmarksEditor( viewer, keybindings, bookmarks );

		brightnessDialog = new BrightnessDialog( dialogOwner, setupAssignments );
		activeSourcesDialog = new VisibilityAndGroupingDialog( dialogOwner, viewer.state() );

		appearanceManager.appearance().updateListeners().add( () -> SwingUtilities.getWindowAncestor( viewer ).repaint() );
		SwingUtilities.invokeLater(() -> appearanceManager.updateLookAndFeel());

		final Actions navigationActions = new Actions( inputTriggerConfig, "bdv", "navigation" );
		navigationActions.install( keybindings, "navigation" );
		NavigationActions.install( navigationActions, viewer, options.values.is2D() );

		final Actions bdvActions = new Actions( inputTriggerConfig, "bdv" );
		bdvActions.install( keybindings, "bdv" );
		BigDataViewerActions.dialog( bdvActions, brightnessDialog );
		BigDataViewerActions.dialog( bdvActions, activeSourcesDialog );
		BigDataViewerActions.bookmarks( bdvActions, bookmarksEditor );
		BigDataViewerActions.manualTransform( bdvActions, manualTransformationEditor );
		bdvActions.runnableAction( this::expandAndFocusCardPanel, EXPAND_CARDS, EXPAND_CARDS_KEYS );
		bdvActions.runnableAction( this::collapseCardPanel, COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );

		viewer.setDisplayMode( DisplayMode.FUSED );
	}

	@Override
	public ManualTransformationEditor getManualTransformEditor()
	{
		return manualTransformationEditor;
	}

	@Override
	public KeymapManager getKeymapManager()
	{
		return keymapManager;
	}

	@Override
	public AppearanceManager getAppearanceManager()
	{
		return appearanceManager;
	}

	@Override
	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	@Override
	public TriggerBehaviourBindings getTriggerbindings()
	{
		return triggerbindings;
	}

	@Override
	boolean createViewer(
			final List< ? extends ConverterSetup > converterSetups,
			final List< ? extends SourceAndConverter< ? > > sources,
			final int numTimepoints )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void close()
	{
		brightnessDialog.dispose();
		activeSourcesDialog.dispose();
		super.close();
	}

	public void expandAndFocusCardPanel()
	{
		splitPanel.setCollapsed( false );
		splitPanel.getRightComponent().requestFocusInWindow();
	}

	public void collapseCardPanel()
	{
		splitPanel.setCollapsed( true );
		viewer.requestFocusInWindow();
	}
}
