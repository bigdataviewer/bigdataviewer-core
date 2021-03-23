package bdv.ui.appearance;

import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SettingsPage;
import bdv.util.Prefs;
import bdv.ui.appearance.StyleElements.ComboBoxEntry;
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

import static bdv.ui.appearance.StyleElements.booleanElement;
import static bdv.ui.appearance.StyleElements.cbentry;
import static bdv.ui.appearance.StyleElements.colorElement;
import static bdv.ui.appearance.StyleElements.comboBoxElement;
import static bdv.ui.appearance.StyleElements.linkedCheckBox;
import static bdv.ui.appearance.StyleElements.linkedColorButton;
import static bdv.ui.appearance.StyleElements.linkedComboBox;
import static bdv.ui.appearance.StyleElements.separator;

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
					comboBoxElement( "look-and-feel", appearance::lookAndFeel, appearance::setLookAndFeel, lafs ),
					separator(),
					booleanElement( "show scalebar", appearance::showScaleBar, appearance::setShowScaleBar ),
					booleanElement( "show scalebar in movies", appearance::showScaleBarInMovie, appearance::setShowScaleBarInMovie ),
					colorElement( "scalebar foreground", appearance::scaleBarColor, appearance::setScaleBarColor ),
					colorElement( "scalebar background", appearance::scaleBarBgColor, appearance::setScaleBarBgColor ),
					separator(),
					booleanElement( "show minimap", appearance::showMultibox, appearance::setShowMultibox ),
					booleanElement( "show source info", appearance::showTextOverlay, appearance::setShowTextOverlay ),
					comboBoxElement( "source name position", appearance::sourceNameOverlayPosition, appearance::setSourceNameOverlayPosition, Prefs.OverlayPosition.values() )
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
							add( new JLabel( element.getLabel() ), "r" );
							add( linkedColorButton( element, colorChooser ), "l, wrap" );
						}

						@Override
						public void visit( final StyleElements.BooleanElement element )
						{
							add( new JLabel( element.getLabel() ), "r" );
							add( linkedCheckBox( element ), "l, wrap" );
						}

						@Override
						public void visit( final StyleElements.ComboBoxElement< ? > element )
						{
							add( new JLabel( element.getLabel() ), "r" );
							add( linkedComboBox( element ), "l, wrap" );
						}
					} ) );

			appearance.updateListeners().add( () -> {
				styleElements.forEach( StyleElements.StyleElement::update );
				repaint();
			} );
		}
	}
}
