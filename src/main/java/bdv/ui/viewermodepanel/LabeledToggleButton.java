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

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JLabel;

import bdv.ui.UIUtils;

class LabeledToggleButton extends ToggleButton
{
	private final String text;
	private final String selectedText;

	private final JLabel label;

	public LabeledToggleButton(
			final Icon icon,
			final Icon selectedIcon,
			final String text,
			final String selectedText,
			final String tooltipText,
			final String selectedTooltipText )
	{
		super( icon, selectedIcon, tooltipText, selectedTooltipText );
		this.text = text;
		this.selectedText = selectedText;

		label = new JLabel( text );
		setFont( label );

		this.add( label, "center" );
	}

	@Override
	public void setSelected( final boolean selected )
	{
		super.setSelected( selected );
		label.setText( selected ? selectedText : text );
	}

	private void setFont( final JLabel label )
	{
		label.setFont( new Font( Font.MONOSPACED, Font.BOLD, ( int )Math.round(9 * UIUtils.getUIScaleFactor() ) ) );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();

		if ( label != null )
			setFont( label );
	}
}
