/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.tools.boundingbox;

import static bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode.FULL;
import static bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode.SECTION;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.scijava.listeners.ChangeListener;
import org.scijava.listeners.ListenableVar;

import bdv.tools.boundingbox.TransformedBoxOverlay.BoxDisplayMode;

/**
 * Panel with radio-buttons to switch between {@link BoxDisplayMode}s.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class BoxDisplayModePanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	public BoxDisplayModePanel( final ListenableVar< BoxDisplayMode, ChangeListener > mode )
	{
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 80, 80 };
//		layout.columnWeights = new double[] { 0.5, 0.5 };
		setLayout( layout );
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.insets = new Insets( 5, 5, 5, 5 );

		final JLabel overlayLabel = new JLabel( "Overlay:", JLabel.LEFT );
		overlayLabel.setFont( getFont().deriveFont( Font.BOLD ) );
		add( overlayLabel, gbc );

		gbc.gridy++;
		gbc.gridwidth = 1;
		final JRadioButton full = new JRadioButton( "Full", mode.get() == FULL );
		full.addActionListener( e -> mode.set( FULL ) );
		add( full, gbc );

		gbc.gridx++;
		final JRadioButton section = new JRadioButton( "Section", mode.get() == SECTION );
		section.addActionListener( e -> mode.set( SECTION ) );
		add( section, gbc );

		final ButtonGroup group = new ButtonGroup();
		group.add( full );
		group.add( section );
	}

	@Override
	public void setEnabled( final boolean b )
	{
		super.setEnabled( b );
		for ( final Component c : getComponents() )
			c.setEnabled( b );
	}
}
