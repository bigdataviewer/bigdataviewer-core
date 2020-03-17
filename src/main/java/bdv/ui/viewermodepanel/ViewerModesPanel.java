package bdv.ui.viewermodepanel;

import bdv.viewer.ViewerState;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

/**
 * This panel adds buttons to toggle fused- and grouped-mode.
 * Additionally two button to block translation and rotation are
 * added.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 */
public class ViewerModesPanel extends JPanel
{
	private static final String TRANSLATION_ACTIVE_TOOL_TIP = "<html><b>Translation</b></html>";
	private static final String TRANSLATION_INACTIVE_TOOL_TIP = "<html>Translation</html>";
	private static final String ROTATION_ACTIVE_TOOL_TIP = "<html><b>Rotation</b></html>";
	private static final String ROTATION_INACTIVE_TOOL_TIP = "<html>Rotation</html";

	private final JToggleButton translation;

	private final JToggleButton rotation;

	private final ViewerModesModel viewerModesModel;

	public ViewerModesPanel( final ViewerState state, final TriggerBehaviourBindings triggerBindings )
	{
		viewerModesModel = new ViewerModesModel( triggerBindings );

		this.setLayout( new MigLayout( "ins 4, fillx, filly", "[]10px[]", "top" ) );
		this.setBackground( Color.white );

		translation = new JToggleButton();
		setupTranslationBlockButton();

		rotation = new JToggleButton();
		setupRotationBlockButton();

		final JPanel display_modes = new DisplaySettingsPanel( state );

		final JPanel navigation = new JPanel( new MigLayout( "ins 0, fillx, filly", "[][]", "top" ) );
		navigation.setBackground( Color.white );
		navigation.add( new JLabel( "Navigation" ), "span 2, growx, center, wrap" );
		navigation.add( translation );
		navigation.add( rotation );

		this.add( display_modes );
		this.add( navigation );
	}

	private void setupTranslationBlockButton()
	{
		final Icon translation_on = new ImageIcon( this.getClass().getResource( "translation_on.png" ) );
		final Icon translation_off = new ImageIcon( this.getClass().getResource( "translation_off.png" ) );

		translation.setIcon( translation_on );
		translation.setToolTipText( TRANSLATION_ACTIVE_TOOL_TIP );
		setLook( translation );
		translation.addActionListener( e -> {
			if ( translation.getModel().isSelected() )
			{
				translation.setIcon( translation_off );
				translation.setToolTipText( TRANSLATION_INACTIVE_TOOL_TIP );
				viewerModesModel.blockTranslation();
			}
			else
			{
				translation.setIcon( translation_on );
				translation.setToolTipText( TRANSLATION_ACTIVE_TOOL_TIP );
				viewerModesModel.unblockTranslation();
			}
		} );
	}

	private void setupRotationBlockButton()
	{
		final Icon rotation_on = new ImageIcon( this.getClass().getResource( "rotation_on.png" ) );
		final Icon rotation_off = new ImageIcon( this.getClass().getResource( "rotation_off.png" ) );

		rotation.setIcon( rotation_on );
		rotation.setToolTipText( ROTATION_ACTIVE_TOOL_TIP );
		setLook( rotation );
		rotation.addActionListener( e -> {
			if ( rotation.getModel().isSelected() )
			{
				rotation.setIcon( rotation_off );
				rotation.setToolTipText( ROTATION_INACTIVE_TOOL_TIP );
				viewerModesModel.blockRotation();
			}
			else
			{
				rotation.setIcon( rotation_on );
				rotation.setToolTipText( ROTATION_ACTIVE_TOOL_TIP );
				viewerModesModel.unblockRotation();
			}
		} );
	}

	private void setLook( final JToggleButton button )
	{
		button.setMaximumSize( new Dimension( button.getIcon().getIconWidth(), button.getIcon().getIconHeight() ) );
		button.setBackground( Color.white );
		button.setBorderPainted( false );
		button.setFocusPainted( false );
		button.setContentAreaFilled( false );
	}
}
