package bdv.ui.viewermodepanel;

import bdv.viewer.DisplayMode;
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
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;
import static bdv.viewer.Interpolation.NLINEAR;
import static bdv.viewer.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.ViewerStateChange.INTERPOLATION_CHANGED;

/**
 * This panel adds buttons to toggle fused, grouped, and
 * interpolation mode.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 * @author Tobias Pietzsch
 */
public class DisplaySettingsPanel extends JPanel
{
	private static final String SINGLE_MODE_TOOL_TIP = "<html><b>Single</b>/Fused</html>";
	private static final String FUSED_MODE_TOOL_TIP = "<html>Single/<b>Fused</b></html>";
	private static final String GROUP_MODE_TOOL_TIP = "<html>Source/<b>Group</b></html>";
	private static final String SOURCE_MODE_TOOL_TIP = "<html><b>Source</b>/Group</html>";
	private static final String NEAREST_INTERPOLATION_TOOL_TIP = "<html><b>Nearest</b>/Linear</html>";
	private static final String LINEAR_INTERPOLATION_TOOL_TIP = "<html>Nearest/<b>Linear</b></html>";

	private final LabeledToggleButton fusion;
	private final LabeledToggleButton grouping;
	private final LabeledToggleButton interpolation;

	public DisplaySettingsPanel( final ViewerState state )
	{
		this.setLayout( new MigLayout( "ins 0, fillx, filly", "[][][]", "top" ) );
		this.setBackground( Color.white );

		fusion = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "single_mode.png" ) ),
				new ImageIcon( this.getClass().getResource( "fusion_mode.png" ) ),
				"Single",
				"Fused",
				SINGLE_MODE_TOOL_TIP,
				FUSED_MODE_TOOL_TIP );
		grouping = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "source_mode.png" ) ),
				new ImageIcon( this.getClass().getResource( "grouping_mode.png" ) ),
				"Source",
				"Group",
				SOURCE_MODE_TOOL_TIP,
				GROUP_MODE_TOOL_TIP );
		interpolation = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "nearest.png" ) ),
				new ImageIcon( this.getClass().getResource( "linear.png" ) ),
				"Nearest",
				"Linear",
				NEAREST_INTERPOLATION_TOOL_TIP,
				LINEAR_INTERPOLATION_TOOL_TIP );

		fusion.setSelected( state.getDisplayMode().hasFused() );
		grouping.setSelected( state.getDisplayMode().hasGrouping() );
		interpolation.setSelected( state.getInterpolation() == NLINEAR );

		state.changeListeners().add( e -> {
			if ( e == DISPLAY_MODE_CHANGED )
			{
				final DisplayMode displayMode = state.getDisplayMode();
				SwingUtilities.invokeLater( () -> {
					fusion.setSelected( displayMode.hasFused() );
					grouping.setSelected( displayMode.hasGrouping() );
				} );
			}
			else if ( e == INTERPOLATION_CHANGED )
			{
				final Interpolation interpolationMode = state.getInterpolation();
				SwingUtilities.invokeLater( () -> interpolation.setSelected( interpolationMode == NLINEAR ) );
			}
		} );

		fusion.button.addActionListener( e -> {
			state.setDisplayMode( state.getDisplayMode().withFused( fusion.button.isSelected() ) );
		} );

		grouping.button.addActionListener( e -> {
			state.setDisplayMode( state.getDisplayMode().withGrouping( grouping.button.isSelected() ) );
		} );

		interpolation.button.addActionListener( e -> {
			state.setInterpolation( interpolation.button.isSelected() ? NLINEAR : NEARESTNEIGHBOR );
		} );

		this.add( new JLabel( "Display Modes" ), "span 3, growx, center, wrap" );
		this.add( fusion );
		this.add( grouping );
		this.add( interpolation );
	}

	private class LabeledToggleButton extends JPanel
	{
		private final String text;
		private final String selectedText;
		private final String tooltipText;
		private final String selectedTooltipText;

		private final JToggleButton button;
		private final JLabel label;

		public LabeledToggleButton(
				final Icon icon,
				final Icon selectedIcon,
				final String text,
				final String selectedText,
				final String tooltipText,
				final String selectedTooltipText )
		{
			this.text = text;
			this.selectedText = selectedText;
			this.tooltipText = tooltipText;
			this.selectedTooltipText = selectedTooltipText;
			this.setLayout( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );
			this.setBackground( Color.white );

			button = new JToggleButton( icon );
			button.setSelectedIcon( selectedIcon );
			setLook( button );

			label = new JLabel( text );
			setFont( label );

			this.add( button, "growx, center, wrap" );
			this.add( label, "center" );
		}

		public void setSelected( final boolean selected )
		{
			button.setSelected( selected );
			label.setText( selected ? selectedText : text );
			button.setToolTipText( selected ? selectedTooltipText : tooltipText );
		}

		private void setFont( final JLabel label )
		{
			label.setFont( new Font( Font.MONOSPACED, Font.BOLD, 9 ) );
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
}
