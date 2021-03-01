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

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

class ToggleButton extends JPanel
{
	private final String tooltipText;
	private final String selectedTooltipText;

	private final JToggleButton button;

	public ToggleButton(
			final Icon icon,
			final Icon selectedIcon,
			final String tooltipText,
			final String selectedTooltipText )
	{
		super( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );
		this.tooltipText = tooltipText;
		this.selectedTooltipText = selectedTooltipText;

		button = new JToggleButton( icon );
		button.setSelectedIcon( selectedIcon );
		setLook( button );

//		this.setBackground( Color.white );
		this.add( button, "growx, center, wrap" );
	}

	public void setIcons( final Icon defaultIcon, final Icon selectedIcon )
	{
		button.setIcon( defaultIcon );
		button.setSelectedIcon( selectedIcon );
		setLook( button );
	}

	public void setSelected( final boolean selected )
	{
		button.setSelected( selected );
		button.setToolTipText( selected ? selectedTooltipText : tooltipText );
	}

	public boolean isSelected()
	{
		return button.isSelected();
	}

	public void addActionListener( final ActionListener l )
	{
		button.addActionListener( l );
	}

	public void removeActionListener( final ActionListener l )
	{
		button.removeActionListener( l );
	}

	private void setLook( final JToggleButton button )
	{
		button.setMaximumSize( new Dimension( button.getIcon().getIconWidth(), button.getIcon().getIconHeight() ) );
//		button.setBackground( Color.white );
		button.setBorderPainted( false );
		button.setFocusPainted( false );
		button.setContentAreaFilled( false );
	}
}
