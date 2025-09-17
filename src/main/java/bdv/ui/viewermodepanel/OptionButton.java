/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * A compact UI button that can be used to cycles through multiple exclusive
 * options (icon + tooltip) each time it's clicked.
 * <p>
 * The update of the current option has to be managed by outside logic that
 * calls {@link #setOption} in response to {@code ActionEvent}s.
 */
class OptionButton extends JPanel
{
	final JButton button;

	private final Icon[] icons;

	private int currentOption = 0;

	public OptionButton(
			final Icon[] icons,
			final String[] tooltipTexts )
	{
		super( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );

		if ( icons == null || tooltipTexts == null )
			throw new NullPointerException();

		if ( icons.length == 0 || icons.length != tooltipTexts.length )
			throw new IllegalArgumentException();

		this.icons = icons;

		button = new JButton( icons[ 0 ] );
		button.setToolTipText( tooltipTexts[ 0 ] );
		setLook( button );

		button.addPropertyChangeListener( e -> {
			if ( e.getPropertyName().equals( "option" ) )
			{
				button.setIcon( this.icons[ currentOption ] );
				button.setToolTipText( tooltipTexts[ currentOption ] );
			}
		} );

		this.add( button, "growx, center, wrap" );
	}

	public void setIcons( final Icon[] icons )
	{
		if ( icons.length != this.icons.length )
			throw new IllegalArgumentException();

		System.arraycopy( icons, 0, this.icons, 0, icons.length );

		button.setIcon( icons[ currentOption ] );
		setLook( button );
	}

	public int getOption()
	{
		return currentOption;
	}

	public void setOption( int option )
	{
		if ( option < 0 || option >= icons.length )
			throw new IllegalArgumentException( "Invalid option index: " + option );
		final int oldOption = currentOption;
		currentOption = option;
		button.firePropertyChange( "option", oldOption, currentOption );
	}

	public void addActionListener( final ActionListener l )
	{
		button.addActionListener( l );
	}

	public void removeActionListener( final ActionListener l )
	{
		button.removeActionListener( l );
	}

	private void setLook( final JButton button )
	{
		button.setMaximumSize( new Dimension( button.getIcon().getIconWidth(), button.getIcon().getIconHeight() ) );
		button.setBorderPainted( false );
		button.setFocusPainted( false );
		button.setContentAreaFilled( false );
	}
}
