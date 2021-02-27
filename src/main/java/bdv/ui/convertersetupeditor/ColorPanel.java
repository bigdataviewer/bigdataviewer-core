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
package bdv.ui.convertersetupeditor;

import bdv.ui.UIUtils;
import java.awt.Color;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import javax.swing.UIManager;
import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ColorIcon;

/**
 * A {@code JPanel} with a color button (for setting {@code ConverterSetup}
 * colors).
 *
 * @author Tobias Pietzsch
 */
class ColorPanel extends JPanel
{
	private final JButton colorButton;

	private final ARGBType color = new ARGBType();

	public interface ChangeListener
	{
		void colorChanged();
	}

	private final Listeners.List< ChangeListener > listeners = new Listeners.SynchronizedList<>();

	/**
	 * Whether the color reflects a set of sources all having the same color
	 */
	private boolean isConsistent = true;

	/**
	 * Panel background if color reflects a set of sources all having the same color
	 */
	private Color consistentBg = Color.WHITE;

	/**
	 * Panel background if color reflects a set of sources with different colors
	 */
	private Color inConsistentBg = Color.WHITE;

	public ColorPanel()
	{
		setLayout( new MigLayout( "ins 0, fillx, filly, hidemode 3", "[grow]", "" ) );
		updateColors();

		colorButton = new JButton();
		this.add( colorButton, "center" );

		colorButton.addActionListener( e -> chooseColor() );

		colorButton.setBorderPainted( false );
		colorButton.setFocusPainted( false );
		colorButton.setContentAreaFilled( false );
		colorButton.setMinimumSize( new Dimension( 46, 42 ) );
		colorButton.setPreferredSize( new Dimension( 46, 42 ) );
		setColor( null );
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		super.setEnabled( enabled );
		if ( colorButton != null )
			colorButton.setEnabled( enabled );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		updateColors();
		if ( !isConsistent )
			setBackground( inConsistentBg );
	}

	private void updateColors()
	{
		consistentBg = UIManager.getColor( "Panel.background" );
		inConsistentBg = UIUtils.mix( consistentBg, Color.red, 0.9 );
	}

	public void setConsistent( final boolean isConsistent )
	{
		this.isConsistent = isConsistent;
		setBackground( isConsistent ? consistentBg : inConsistentBg );
	}

	private void chooseColor()
	{
		final Color newColor = JColorChooser.showDialog( null, "Set Source Color", new Color( color.get() ) );
		if ( newColor == null )
			return;
		setColor( new ARGBType(  newColor.getRGB() | 0xff000000 ) );
		listeners.list.forEach( ChangeListener::colorChanged );
	};

	public Listeners< ChangeListener > changeListeners()
	{
		return listeners;
	}

	public synchronized void setColor( final ARGBType color )
	{
		if ( color == null )
			this.color.set( 0xffaaaaaa );
		else
			this.color.set( color );
		colorButton.setIcon( new ColorIcon( new Color( this.color.get() ), 30, 30, 10, 10, true ) );
	}

	public ARGBType getColor()
	{
		return color.copy();
	}
}
