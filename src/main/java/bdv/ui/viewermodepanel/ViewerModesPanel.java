package bdv.ui.viewermodepanel;

import bdv.viewer.ViewerState;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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

	private static final String TRANSLATION_ACTIVE_TOOL_TIP = "<html><b>Translation</b></html>";

	private static final String TRANSLATION_INACTIVE_TOOL_TIP = "<html>Translation</html>";

	private static final String ROTATION_ACTIVE_TOOL_TIP = "<html><b>Rotation</b></html>";

	private static final String ROTATION_INACTIVE_TOOL_TIP = "<html>Rotation</html";

	private final JToggleButton fusion;

	private final JToggleButton grouping;

	private final JToggleButton translation;

	private final JToggleButton rotation;

	private final ViewerModesModel viewerModesModel;

	public ViewerModesPanel( final ViewerState state, final TriggerBehaviourBindings triggerBindings )
	{
		viewerModesModel = new ViewerModesModel( state, triggerBindings );
		viewerModesModel.addViewerModeListener( this );

		this.setLayout( new MigLayout( "ins 4, fillx, filly", "[center, grow][center, grow][center, grow][center, grow]", "" ) );
		this.setBackground( Color.white );

		fusion = new JToggleButton();
		setupFusedModeButton();

		grouping = new JToggleButton();
		setupGroupedModeButton();

		translation = new JToggleButton();
		setupTranslationBlockButton();

		rotation = new JToggleButton();
		setupRotationBlockButton();

		this.add( fusion, "growx" );
		this.add( grouping, "growx" );
		this.add( translation, "growx" );
		this.add( rotation, "growx" );
	}

	private void setupFusedModeButton()
	{
		final Icon fusion_icon = new ImageIcon( this.getClass().getResource( "fusion_mode.png" ) );
		final Icon single_icon = new ImageIcon( this.getClass().getResource( "single_mode.png" ) );

		fusion.setIcon( single_icon );
		fusion.setToolTipText( SINGLE_MODE_TOOL_TIP );
		setLook( fusion );
		fusion.getModel().addChangeListener( e -> {
			if ( fusion.getModel().isSelected() )
			{
				fusion.setIcon( fusion_icon );
				fusion.setToolTipText( FUSED_MODE_TOOL_TIP );
			}
			else
			{
				fusion.setIcon( single_icon );
				fusion.setToolTipText( SINGLE_MODE_TOOL_TIP );
			}
		} );
		fusion.addActionListener( e -> {
			viewerModesModel.setFused( fusion.getModel().isSelected() );
		} );
	}

	private void setupGroupedModeButton()
	{
		final Icon grouping_icon = new ImageIcon( this.getClass().getResource( "grouping_mode.png" ) );
		final Icon source_icon = new ImageIcon( this.getClass().getResource( "source_mode.png" ) );

		grouping.setIcon( source_icon );
		grouping.setToolTipText( SOURCE_MODE_TOOL_TIP );
		setLook( grouping );
		grouping.getModel().addChangeListener( e -> {
			if ( grouping.getModel().isSelected() )
			{
				grouping.setIcon( grouping_icon );
				grouping.setToolTipText( GROUP_MODE_TOOL_TIP );
			}
			else
			{
				grouping.setIcon( source_icon );
				grouping.setToolTipText( SOURCE_MODE_TOOL_TIP );
			}
		} );
		grouping.addActionListener( e -> {
			viewerModesModel.setGrouping( grouping.getModel().isSelected() );
		} );
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
		button.setMaximumSize( new Dimension( 40, 40 ) );
		button.setBackground( Color.white );
		button.setBorderPainted( false );
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
}
