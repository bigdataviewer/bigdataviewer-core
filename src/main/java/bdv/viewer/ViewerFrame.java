/*
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
package bdv.viewer;

import bdv.BigDataViewer;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.TransformEventHandler;
import bdv.cache.CacheControl;
import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.splitpanel.SplitPanel;
import bdv.util.AWTUtils;

/**
 * A {@link JFrame} containing a {@link ViewerPanel} and associated
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch
 */
public class ViewerFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	private final ViewerPanel viewer;

	private final CardPanel cards;

	private final SplitPanel splitPanel;

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final Behaviours transformBehaviours;

	private final ConverterSetups setups;

	private final KeymapManager keymapManager;

	private final AppearanceManager appearanceManager;

	public ViewerFrame(
			final List< SourceAndConverter< ? > > sources,
			final int numTimepoints,
			final CacheControl cache )
	{
		this( sources, numTimepoints, cache, new KeymapManager( BigDataViewer.configDir ), new AppearanceManager( BigDataViewer.configDir ), ViewerOptions.options() );
	}

	/**
	 *
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimepoints
	 *            number of available timepoints.
	 * @param cacheControl
	 *            handle to cache. This is used to control io timing.
	 * @param optional
	 *            optional parameters. See {@link ViewerOptions#options()}.
	 */
	public ViewerFrame(
			final List< SourceAndConverter< ? > > sources,
			final int numTimepoints,
			final CacheControl cacheControl,
			final KeymapManager keymapManager,
			final AppearanceManager appearanceManager,
			final ViewerOptions optional )
	{
//		super( "BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.ARGB_COLOR_MODEL ) );
		super( "BigDataViewer", AWTUtils.getSuitableGraphicsConfiguration( AWTUtils.RGB_COLOR_MODEL ) );
		this.keymapManager = keymapManager;
		this.appearanceManager = appearanceManager;
		viewer = new ViewerPanel( sources, numTimepoints, cacheControl, optional );
		setups = new ConverterSetups( viewer.state() );
		setups.listeners().add( s -> viewer.requestRepaint() );

		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();

		cards = new CardPanel();
		BdvDefaultCards.setup( cards, viewer, setups );
		splitPanel = new SplitPanel( viewer, cards );

		getRootPane().setDoubleBuffered( true );
//		add( viewer, BorderLayout.CENTER );
		add( splitPanel, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				viewer.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( viewer, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( viewer, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		mouseAndKeyHandler.setKeypressManager( optional.values.getKeyPressedManager(), viewer.getDisplayComponent() );
		viewer.getDisplay().addHandler( mouseAndKeyHandler );

		transformBehaviours = new Behaviours( optional.values.getInputTriggerConfig(), "bdv" );
		transformBehaviours.install( triggerbindings, "transform" );

		final TransformEventHandler tfHandler = viewer.getTransformEventHandler();
		tfHandler.install( transformBehaviours );
	}

	public ViewerPanel getViewerPanel()
	{
		return viewer;
	}

	public CardPanel getCardPanel()
	{
		return cards;
	}

	public SplitPanel getSplitPanel()
	{
		return splitPanel;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}

	public TriggerBehaviourBindings getTriggerbindings()
	{
		return triggerbindings;
	}

	/**
	 * Get {@code Behaviours} hook where TransformEventHandler behaviours are installed.
	 * This is installed in {@link #getTriggerbindings} with the id "transform".
	 * The hook can be used to update the keymap and install additional behaviours.
	 */
	public Behaviours getTransformBehaviours()
	{
		return transformBehaviours;
	}

	public ConverterSetups getConverterSetups()
	{
		return setups;
	}

	public KeymapManager getKeymapManager()
	{
		return keymapManager;
	}

	public AppearanceManager getAppearanceManager()
	{
		return appearanceManager;
	}
}
