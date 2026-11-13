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
package bdv.ui.links;

import static bdv.ui.settings.StyleElements.booleanElement;
import static bdv.ui.settings.StyleElements.comboBoxElement;
import static bdv.ui.settings.StyleElements.linkedCheckBox;
import static bdv.ui.settings.StyleElements.linkedComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.scijava.listeners.Listeners;

import bdv.tools.links.PasteSettings.RecenterMethod;
import bdv.tools.links.PasteSettings.RescaleMethod;
import bdv.tools.links.PasteSettings.SourceMatchingMethod;
import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SettingsPage;
import bdv.ui.settings.StyleElements;
import bdv.ui.settings.StyleElements.BooleanElement;
import bdv.ui.settings.StyleElements.ComboBoxElement;
import net.miginfocom.swing.MigLayout;

/**
 * Preferences page for changing {@link LinkSettings}.
 */
public class LinkSettingsPage implements SettingsPage
{
	private final String treePath;

	private final LinkSettingsManager manager;

	private final Listeners.List< ModificationListener > modificationListeners;

	private final LinkSettings editedLinkSettings;

	private final JPanel panel;

	public LinkSettingsPage( final LinkSettingsManager manager )
	{
		this( "LinkSettings", manager );
	}

	public LinkSettingsPage( final String treePath, final LinkSettingsManager manager )
	{
		this.treePath = treePath;
		this.manager = manager;
		editedLinkSettings = new LinkSettings();
		editedLinkSettings.set( manager.linkSettings() );
		panel = new Panel( editedLinkSettings );
		modificationListeners = new Listeners.SynchronizedList<>();
		editedLinkSettings.updateListeners().add( () -> modificationListeners.list.forEach( l -> l.setModified() ) );
	}

	@Override
	public String getTreePath()
	{
		return treePath;
	}

	@Override
	public JPanel getJPanel()
	{
		return panel;
	}

	@Override
	public Listeners< ModificationListener > modificationListeners()
	{
		return modificationListeners;
	}

	@Override
	public void cancel()
	{
		editedLinkSettings.set( manager.linkSettings() );
	}

	@Override
	public void apply()
	{
		manager.linkSettings().set( editedLinkSettings );
		manager.save();
	}

	// --------------------------------------------------------------------

	static class Panel extends JPanel
	{
		private boolean bla;

		public Panel( final LinkSettings ls )
		{
			super( new MigLayout( "fillx", "[l]", "" ) );

			final BooleanElement be = booleanElement( "show link settings in side panel", ls::showLinkSettingsCard, ls::setShowLinkSettingsCard );
			ls.updateListeners().add( be::update );

			add( new LinkSettingsPanel( ls, false ), "l, wrap" );
			add( new JSeparator(), "growx, wrap" );
			add( linkedCheckBox( be, be.getLabel() ), "l, wrap" );
		}
	}

	// --------------------------------------------------------------------

	public static class LinkSettingsPanel extends JPanel
	{
		public LinkSettingsPanel( final LinkSettings ls )
		{
			this( ls, true );
		}

		private final List< StyleElements.StyleElement > styleElements = new ArrayList<>();

		private LinkSettingsPanel( final LinkSettings ls, boolean inCardPanel )
		{
			super( new MigLayout( "fillx, insets " + ( inCardPanel ? "0" : "0 0 40 0" ), "[l]", "" ) );

			final SourceMatchingMethod[] matchingMethods = {
					SourceMatchingMethod.BY_SPEC_LOAD_MISSING,
					SourceMatchingMethod.BY_SPEC,
					SourceMatchingMethod.BY_INDEX
			};
			final String[] matchingMethodLabels = {
					"by spec, load unmatched",
					"by spec",
					"by index"
			};

			final RescaleMethod[] rescaleMethods = {
					RescaleMethod.NONE,
					RescaleMethod.FIT_PANEL,
					RescaleMethod.FILL_PANEL
			};
			final String[] rescaleMethodLabels = {
					"--",
					"fit to panel",
					"fill panel"
			};

			final RecenterMethod[] recenterMethods = {
					RecenterMethod.NONE,
					RecenterMethod.PANEL_CENTER,
					RecenterMethod.MOUSE_POS
			};
			final String[] recenterMethodLabels = {
					"--",
					"pasted panel center",
					"pasted mouse pos"
			};

			add( new JLabel( "when pasting links ..." ), "growx, gapbottom 5, wrap" );
			addCheckBox( booleanElement( "set display mode", ls::pasteDisplayMode, ls::setPasteDisplayMode ) );
			addCheckBox( booleanElement( "set timepoint", ls::pasteCurrentTimepoint, ls::setPasteCurrentTimepoint ) );
			final JCheckBox setTransformCheckBox = addCheckBox( booleanElement( "set view transform", ls::pasteViewerTransform, ls::setPasteViewerTransform ) );
			final Consumer< Boolean > enableRescale = addComboBox( true, 20, comboBoxElement( "rescale", ls::rescaleMethod, ls::setRescaleMethod, rescaleMethods, rescaleMethodLabels ) );
			final Consumer< Boolean > enableRecenter = addComboBox( true, 20, comboBoxElement( "recenter", ls::recenterMethod, ls::setRecenterMethod, recenterMethods, recenterMethodLabels ) );
			add( Box.createVerticalStrut( 5 ), "growx,  wrap" );
			addCheckBox( booleanElement( "set source visibility", ls::pasteSourceVisibility, ls::setPasteSourceVisibility ) );
			addCheckBox( booleanElement( "set source min/max and color", ls::pasteSourceConverterConfigs, ls::setPasteSourceConverterConfigs ) );
			add( Box.createVerticalStrut( 5 ), "growx,  wrap" );
			addComboBox( !inCardPanel, 0, comboBoxElement( "match sources", ls::sourceMatchingMethod, ls::setSourceMatchingMethod, matchingMethods, matchingMethodLabels ) );

			ls.updateListeners().add( () -> {
				styleElements.forEach( StyleElements.StyleElement::update );
				repaint();
			} );

			setTransformCheckBox.addActionListener( e -> {
				enableRescale.accept( setTransformCheckBox.isSelected() );
				enableRecenter.accept( setTransformCheckBox.isSelected() );
			} );
			enableRescale.accept( setTransformCheckBox.isSelected() );
			enableRecenter.accept( setTransformCheckBox.isSelected() );
		}

		private JCheckBox addCheckBox( BooleanElement element )
		{
			final JCheckBox checkBox = linkedCheckBox( element, element.getLabel() );
			add( checkBox, "l,  wrap" );
			styleElements.add( element );
			return checkBox;
		}

		private Consumer< Boolean > addComboBox( boolean singleRow, int gapleft, ComboBoxElement< ? > element )
		{
			JPanel row = new JPanel( new MigLayout( "insets 0, nogrid", "", "" ) );
			final JLabel label = new JLabel( element.getLabel() );
			final JComboBox< ? > comboBox = linkedComboBox( element );
			if ( singleRow )
			{
				row.add( label, "l" );
				row.add( comboBox, "l, growx, wrap 0" );
			}
			else
			{
				row.add( label, "l, wrap" );
				row.add( comboBox, "l, wrap 0" );
			}
			add( row, "l, gapleft " + gapleft + ", growx, wrap" );
			styleElements.add( element );
			return b -> {
				label.setEnabled( b );
				comboBox.setEnabled( b );
			};
		}
	}
}
