package bdv.ui.viewermodepanel;

import bdv.viewer.ViewerState;
import java.awt.Color;
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
		this.add( new DisplaySettingsPanel( state ) );
		this.add( new NavigationModesPanel( triggerBindings ) );
	}
}
