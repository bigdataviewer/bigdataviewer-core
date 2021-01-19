/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.ui.viewermodepanel;

import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;
import static bdv.viewer.Interpolation.NLINEAR;
import static bdv.viewer.ViewerStateChange.DISPLAY_MODE_CHANGED;
import static bdv.viewer.ViewerStateChange.INTERPOLATION_CHANGED;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import bdv.ui.UIUtils;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerState;
import net.miginfocom.swing.MigLayout;

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
		super( new MigLayout( "ins 2 0 0 0, fillx, filly", "[][][]", "top" ) );

		final String isDark = UIUtils.isDark( "Panel.background" ) ? "_dark" : "";
		final String isLarge = UIUtils.getUIScaleFactor() > 1.5 ? "_200" : "";

		fusion = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "single_mode" + isDark + isLarge + ".png" ) ),
				new ImageIcon( this.getClass().getResource( "fusion_mode" + isDark + isLarge + ".png" ) ),
				" Single",
				" Fused ",
				SINGLE_MODE_TOOL_TIP,
				FUSED_MODE_TOOL_TIP );
		grouping = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "source_mode" + isDark + isLarge + ".png" ) ),
				new ImageIcon( this.getClass().getResource( "grouping_mode" + isDark + isLarge + ".png" ) ),
				" Source",
				" Group ",
				SOURCE_MODE_TOOL_TIP,
				GROUP_MODE_TOOL_TIP );
		interpolation = new LabeledToggleButton(
				new ImageIcon( this.getClass().getResource( "nearest" + isDark + isLarge + ".png" ) ),
				new ImageIcon( this.getClass().getResource( "linear" + isDark + isLarge + ".png" ) ),
				" Nearest",
				" Linear ",
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

		fusion.addActionListener( e -> {
			state.setDisplayMode( state.getDisplayMode().withFused( fusion.isSelected() ) );
		} );

		grouping.addActionListener( e -> {
			state.setDisplayMode( state.getDisplayMode().withGrouping( grouping.isSelected() ) );
		} );

		interpolation.addActionListener( e -> {
			state.setInterpolation( interpolation.isSelected() ? NLINEAR : NEARESTNEIGHBOR );
		} );

		this.add( fusion );
		this.add( grouping );
		this.add( interpolation );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		this.setBackground( UIManager.getColor( "Panel.background" ) );
		if ( fusion != null )
		{
			final String isDark = UIUtils.isDark( "Panel.background" ) ? "_dark" : "";
			final String isLarge = UIUtils.getUIScaleFactor() > 1.5 ? "_200" : "";

			fusion.setIcons(
					new ImageIcon( this.getClass().getResource( "single_mode" + isDark + isLarge + ".png" ) ),
					new ImageIcon( this.getClass().getResource( "fusion_mode" + isDark + isLarge + ".png" ) )
			);
			grouping.setIcons(
					new ImageIcon( this.getClass().getResource( "source_mode" + isDark + isLarge + ".png" ) ),
					new ImageIcon( this.getClass().getResource( "grouping_mode" + isDark + isLarge + ".png" ) )
			);
			interpolation.setIcons(
					new ImageIcon( this.getClass().getResource( "nearest" + isDark + isLarge + ".png" ) ),
					new ImageIcon( this.getClass().getResource( "linear" + isDark + isLarge + ".png" ) )
			);
		}
	}
}
