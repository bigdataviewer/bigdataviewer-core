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
package bdv.ui.appearance;

import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SettingsPage;
import bdv.ui.settings.StyleElements;
import bdv.util.Prefs;
import bdv.ui.settings.StyleElements.ComboBoxEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import net.miginfocom.swing.MigLayout;
import org.scijava.listeners.Listeners;

import static bdv.ui.settings.StyleElements.booleanElement;
import static bdv.ui.settings.StyleElements.cbentry;
import static bdv.ui.settings.StyleElements.colorElement;
import static bdv.ui.settings.StyleElements.comboBoxElement;
import static bdv.ui.settings.StyleElements.linkedCheckBox;
import static bdv.ui.settings.StyleElements.linkedColorButton;
import static bdv.ui.settings.StyleElements.linkedComboBox;
import static bdv.ui.settings.StyleElements.separator;

/**
 * Preferences page for changing {@link Appearance}.
 */
public class AppearanceSettingsPage implements SettingsPage
{
	private final String treePath;

	private final AppearanceManager manager;

	private final Listeners.List< ModificationListener > modificationListeners;

	private final Appearance editedAppearance;

	private final JPanel panel;

	public AppearanceSettingsPage( final AppearanceManager manager )
	{
		this( "Appearance", manager );
	}

	public AppearanceSettingsPage( final String treePath, final AppearanceManager manager )
	{
		this.treePath = treePath;
		this.manager = manager;
		editedAppearance = new Appearance();
		editedAppearance.set( manager.appearance() );
		panel = new AppearancePanel( editedAppearance );
		modificationListeners = new Listeners.SynchronizedList<>();
		editedAppearance.updateListeners().add( () -> modificationListeners.list.forEach( l -> l.setModified() ) );
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
		editedAppearance.set( manager.appearance() );
	}

	@Override
	public void apply()
	{
		manager.appearance().set( editedAppearance );
		manager.save();
		manager.toPrefs();
		manager.updateLookAndFeel();
	}

	// --------------------------------------------------------------------

	static class AppearancePanel extends JPanel
	{
		public AppearancePanel( final Appearance appearance )
		{
			super( new MigLayout( "fillx", "[r][l]", "" ) );

			final List< ComboBoxEntry< LookAndFeelInfo > > lafs = new ArrayList<>();
			lafs.add( cbentry( Appearance.DONT_MODIFY_LOOK_AND_FEEL, "don't set" ) );
			for ( LookAndFeelInfo feel : UIManager.getInstalledLookAndFeels() )
				lafs.add( cbentry( feel, feel.getName() ) );

			final List< StyleElements.StyleElement > styleElements = Arrays.asList(
					comboBoxElement( "look-and-feel:", appearance::lookAndFeel, appearance::setLookAndFeel, lafs ),
					separator(),
					booleanElement( "show scalebar", appearance::showScaleBar, appearance::setShowScaleBar ),
					booleanElement( "show scalebar in movies", appearance::showScaleBarInMovie, appearance::setShowScaleBarInMovie ),
					colorElement( "scalebar foreground", appearance::scaleBarColor, appearance::setScaleBarColor ),
					colorElement( "scalebar background", appearance::scaleBarBgColor, appearance::setScaleBarBgColor ),
					separator(),
					booleanElement( "show minimap", appearance::showMultibox, appearance::setShowMultibox ),
					booleanElement( "show source info", appearance::showTextOverlay, appearance::setShowTextOverlay ),
					separator(),
					comboBoxElement( "source name position:", appearance::sourceNameOverlayPosition, appearance::setSourceNameOverlayPosition, Prefs.OverlayPosition.values() )
			);

			final JColorChooser colorChooser = new JColorChooser();
			styleElements.forEach( element -> element.accept(
					new StyleElements.StyleElementVisitor()
					{
						@Override
						public void visit( final StyleElements.Separator separator )
						{
							add( Box.createVerticalStrut( 10 ), "growx, span 2, wrap" );
						}

						@Override
						public void visit( final StyleElements.ColorElement element )
						{
							JPanel row = new JPanel(new MigLayout( "insets 0, fillx", "[r][l]", "" ));
							row.add( linkedColorButton( element, colorChooser ), "l" );
							row.add( new JLabel( element.getLabel() ), "l, growx" );
							add( row, "l, span 2, wrap" );
						}

						@Override
						public void visit( final StyleElements.BooleanElement element )
						{
							add( linkedCheckBox( element, element.getLabel() ), "l, span 2, wrap" );
						}

						@Override
						public void visit( final StyleElements.ComboBoxElement< ? > element )
						{
							JPanel row = new JPanel(new MigLayout( "insets 0, fillx", "[r][l]", "" ));
							row.add( new JLabel( element.getLabel() ), "l" );
							row.add( linkedComboBox( element ), "l, growx" );
							add( row, "l, span 2, wrap" );
						}
					} ) );

			appearance.updateListeners().add( () -> {
				styleElements.forEach( StyleElements.StyleElement::update );
				repaint();
			} );
		}
	}
}
