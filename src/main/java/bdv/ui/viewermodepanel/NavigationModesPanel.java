package bdv.ui.viewermodepanel;

import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * This panel adds buttons to toggle fused- and grouped-mode.
 * Additionally two button to block translation and rotation are
 * added.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 */
class NavigationModesPanel extends JPanel
{
	private static final String TRANSLATION_ACTIVE_TOOL_TIP = "<html><b>Translation</b></html>";
	private static final String TRANSLATION_INACTIVE_TOOL_TIP = "<html>Translation</html>";
	private static final String ROTATION_ACTIVE_TOOL_TIP = "<html><b>Rotation</b></html>";
	private static final String ROTATION_INACTIVE_TOOL_TIP = "<html>Rotation</html";

	private final ToggleButton translation;
	private final ToggleButton rotation;

	public NavigationModesPanel( final TriggerBehaviourBindings triggerBindings )
	{
		final NavigationModesModel model = new NavigationModesModel( triggerBindings );

		this.setLayout( new MigLayout( "ins 0, fillx, filly", "[][]", "top" ) );
		this.setBackground( Color.white );

		translation = new ToggleButton(
				new ImageIcon( this.getClass().getResource( "translation_on.png" ) ),
				new ImageIcon( this.getClass().getResource( "translation_off.png" ) ),
				TRANSLATION_ACTIVE_TOOL_TIP,
				TRANSLATION_INACTIVE_TOOL_TIP );

		rotation = new ToggleButton(
				new ImageIcon( this.getClass().getResource( "rotation_on.png" ) ),
				new ImageIcon( this.getClass().getResource( "rotation_off.png" ) ),
				ROTATION_ACTIVE_TOOL_TIP,
				ROTATION_INACTIVE_TOOL_TIP );

		model.changeListeners().add( () ->{
			final boolean rotationBlocked = model.isRotationBlocked();
			final boolean translationBlocked = model.isTranslationBlocked();
			SwingUtilities.invokeLater( () -> {
				translation.setSelected( translationBlocked );
				rotation.setSelected( rotationBlocked );
			} );
		} );

		translation.addActionListener( e -> {
			model.setTranslationBlocked( translation.isSelected() );
		} );

		rotation.addActionListener( e -> {
			model.setRotationBlocked( rotation.isSelected() );
		} );

		this.add( new JLabel( "Navigation" ), "span 2, growx, center, wrap" );
		this.add( translation );
		this.add( rotation );
	}

}
