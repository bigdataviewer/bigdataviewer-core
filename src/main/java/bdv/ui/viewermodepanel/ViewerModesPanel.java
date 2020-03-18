package bdv.ui.viewermodepanel;

import bdv.viewer.ViewerState;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	public ViewerModesPanel( final ViewerState state, final TriggerBehaviourBindings triggerBindings )
	{
		super( new MigLayout( "ins 4, fillx, filly", "[]10px[]", "top" ) );
		this.setBackground( Color.white );

		JPanel displaySettings = new JPanel( new MigLayout( "ins 0, fillx, filly", "[]", "top" ) );
		displaySettings.setBackground( Color.WHITE );
		displaySettings.add( new JLabel( "Display Modes" ), "span 3, growx, center, wrap" );
		displaySettings.add( new DisplaySettingsPanel( state ) );

		JPanel navigationSettings = new JPanel( new MigLayout( "ins 0, fillx, filly", "[]", "top" ) );
		navigationSettings.setBackground( Color.white );
		navigationSettings.add( new JLabel( "Navigation" ), "span 2, growx, center, wrap" );
		navigationSettings.add( new NavigationModesPanel( triggerBindings ) );

		this.add( displaySettings );
		this.add( navigationSettings );
	}
}
