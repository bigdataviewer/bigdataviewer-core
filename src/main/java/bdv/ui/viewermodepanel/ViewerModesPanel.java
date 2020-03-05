package bdv.ui.viewermodepanel;

import bdv.viewer.Interpolation;
import bdv.viewer.ViewerState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
public class ViewerModesPanel extends JPanel implements ViewerModesModel.ViewerModeListener
{

	private static final String SINGLE_MODE_TOOL_TIP = "<html><b>Single</b>/Fused</html>";

	private static final String FUSED_MODE_TOOL_TIP = "<html>Single/<b>Fused</b></html>";

	private static final String GROUP_MODE_TOOL_TIP = "<html>Source/<b>Group</b></html>";

	private static final String SOURCE_MODE_TOOL_TIP = "<html><b>Source</b>/Group</html>";

	private static final String NEAREST_INTERPOLATION_TOOL_TIP = "<html><b>Nearest</b>/Linear</html>";

	private static final String LINEAR_INTERPOLATION_TOOL_TIP = "<html>Nearest/<b>Linear</b></html>";

	private static final String TRANSLATION_ACTIVE_TOOL_TIP = "<html><b>Translation</b></html>";

	private static final String TRANSLATION_INACTIVE_TOOL_TIP = "<html>Translation</html>";

	private static final String ROTATION_ACTIVE_TOOL_TIP = "<html><b>Rotation</b></html>";

	private static final String ROTATION_INACTIVE_TOOL_TIP = "<html>Rotation</html";

	private final JToggleButton fusion;

	private final JToggleButton grouping;

	private final JToggleButton interpolation;

	private final JToggleButton translation;

	private final JToggleButton rotation;

	private final ViewerModesModel viewerModesModel;

	public ViewerModesPanel( final ViewerState state, final TriggerBehaviourBindings triggerBindings )
	{
		viewerModesModel = new ViewerModesModel( state, triggerBindings );
		viewerModesModel.addViewerModeListener( this );

		this.setLayout( new MigLayout( "ins 4, fillx, filly", "[]10px[]", "top" ) );
		this.setBackground( Color.white );

		fusion = new JToggleButton();

		grouping = new JToggleButton();

		interpolation = new JToggleButton();

		translation = new JToggleButton();
		setupTranslationBlockButton();

		rotation = new JToggleButton();
		setupRotationBlockButton();

		final JPanel display_modes = new JPanel( new MigLayout( "ins 0, fillx, filly", "[][][]", "top" ) );
		display_modes.setBackground( Color.white );
		display_modes.add( new JLabel( "Display Modes" ), "span 3, growx, center, wrap" );
		display_modes.add( setupFusedModeButton() );
		display_modes.add( setupGroupedModeButton() );
		display_modes.add( setupInterpolationModeButton() );

		final JPanel navigation = new JPanel( new MigLayout( "ins 0, fillx, filly", "[][]", "top" ) );
		navigation.setBackground( Color.white );
		navigation.add( new JLabel( "Navigation" ), "span 2, growx, center, wrap" );
		navigation.add( translation );
		navigation.add( rotation );

		this.add( display_modes );
		this.add( navigation );
	}

	private void setFont(final JLabel label) {
		label.setFont( new Font(Font.MONOSPACED, Font.BOLD, 9) );
	}

	private JPanel setupFusedModeButton()
	{
		final Icon fusion_icon = new ImageIcon( this.getClass().getResource( "fusion_mode.png" ) );
		final Icon single_icon = new ImageIcon( this.getClass().getResource( "single_mode.png" ) );

		final JLabel fusionLabel = new JLabel( "Single" );
		setFont( fusionLabel );

		fusion.setIcon( single_icon );
		fusion.setToolTipText( SINGLE_MODE_TOOL_TIP );
		setLook( fusion );
		fusion.getModel().addChangeListener( e -> {
			if ( fusion.getModel().isSelected() )
			{
				fusion.setIcon( fusion_icon );
				fusion.setToolTipText( FUSED_MODE_TOOL_TIP );
				fusionLabel.setText( "Fused" );
			}
			else
			{
				fusion.setIcon( single_icon );
				fusion.setToolTipText( SINGLE_MODE_TOOL_TIP );
				fusionLabel.setText( "Single" );
			}
		} );
		fusion.addActionListener( e -> {
			viewerModesModel.setFused( fusion.getModel().isSelected() );
		} );

		return new LabeledToggleButton( fusion, fusionLabel );
	}

	private LabeledToggleButton setupGroupedModeButton()
	{
		final Icon grouping_icon = new ImageIcon( this.getClass().getResource( "grouping_mode.png" ) );
		final Icon source_icon = new ImageIcon( this.getClass().getResource( "source_mode.png" ) );

		final JLabel groupLabel = new JLabel( "Source" );
		setFont( groupLabel );

		grouping.setIcon( source_icon );
		grouping.setToolTipText( SOURCE_MODE_TOOL_TIP );
		setLook( grouping );
		grouping.getModel().addChangeListener( e -> {
			if ( grouping.getModel().isSelected() )
			{
				grouping.setIcon( grouping_icon );
				grouping.setToolTipText( GROUP_MODE_TOOL_TIP );
				groupLabel.setText( "Group" );
			}
			else
			{
				grouping.setIcon( source_icon );
				grouping.setToolTipText( SOURCE_MODE_TOOL_TIP );
				groupLabel.setText( "Source" );
			}
		} );
		grouping.addActionListener( e -> {
			viewerModesModel.setGrouping( grouping.getModel().isSelected() );
		} );

		return new LabeledToggleButton( grouping, groupLabel );
	}

	private LabeledToggleButton setupInterpolationModeButton()
	{
		final Icon nearest_icon = new ImageIcon( this.getClass().getResource( "nearest.png" ) );
		final Icon linear_icon = new ImageIcon( this.getClass().getResource( "linear.png" ) );

		final JLabel interpolationLabel = new JLabel( "Nearest" );
		setFont( interpolationLabel );

		interpolation.setIcon( nearest_icon );
		interpolation.setToolTipText( NEAREST_INTERPOLATION_TOOL_TIP );
		setLook( interpolation );
		interpolation.getModel().addChangeListener( e -> {
			if ( interpolation.getModel().isSelected() )
			{
				interpolation.setIcon( linear_icon );
				interpolation.setToolTipText( LINEAR_INTERPOLATION_TOOL_TIP );
				interpolationLabel.setText( "Linear" );
				viewerModesModel.setInterpolation( Interpolation.NLINEAR );
			}
			else
			{
				interpolation.setIcon( nearest_icon );
				interpolation.setToolTipText( NEAREST_INTERPOLATION_TOOL_TIP );
				interpolationLabel.setText( "Nearest" );
				viewerModesModel.setInterpolation( Interpolation.NEARESTNEIGHBOR );
			}
		} );

		return new LabeledToggleButton( interpolation, interpolationLabel );
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

	@Override
	public void singleMode()
	{
		fusion.getModel().setSelected( false );
	}

	@Override
	public void fusedMode()
	{
		fusion.getModel().setSelected( true );
	}

	@Override
	public void sourceMode()
	{
		grouping.getModel().setSelected( false );
	}

	@Override
	public void groupMode()
	{
		grouping.getModel().setSelected( true );
	}

	@Override
	public void interpolationMode( final Interpolation interpolation_mode )
	{
		interpolation.setSelected( interpolation_mode == Interpolation.NLINEAR );
	}

	private class LabeledToggleButton extends JPanel
	{
		public LabeledToggleButton( final JToggleButton button, final JLabel label )
		{
			this.setLayout( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );
			this.setBackground( Color.white );

			this.add( button, "growx, center, wrap" );
			this.add( label, "center" );
		}

	}
}
