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
package bdv.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Card panel example.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 */
public class CardPanelExample
{

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final JFrame frame = new JFrame( "CardPanel Example" );
		frame.setLayout( new MigLayout( "fillx", "[]", "" ) );
		final JButton add = new JButton( "Add Card" );
		final JButton remove = new JButton( "Remove Card" );
		final JButton toggle = new JButton( "Toggle Card" );
		frame.add( add, "growx, wrap" );
		frame.add( remove, "growx, wrap" );
		frame.add( toggle, "growx, wrap" );

		final CardPanel cardPanel = new CardPanel();

		frame.setPreferredSize( new Dimension( 200, 300 ) );
		frame.add( cardPanel.getComponent(), "growx, growy" );
		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.setVisible( true );

		final Random rand = new Random();
		final List< String > names = new ArrayList<>();

		add.addActionListener( e ->
		{
			final String name = "Card " + rand.nextInt();
			cardPanel.addCard( name, new JLabel( "Conent " + rand.nextFloat() ), rand.nextBoolean() );
			names.add( name );
		} );
		remove.addActionListener( e->
		{
			if ( names.size() > 0 )
			{
				final int idx = rand.nextInt( names.size() );
				cardPanel.removeCard( names.get( idx ) );
				names.remove( idx );
			}
		} );
		toggle.addActionListener( e ->
		{
			if ( names.size() > 0 )
			{
				final int idx = rand.nextInt( names.size() );
				cardPanel.setCardExpanded( names.get( idx ), !cardPanel.isCardExpanded( names.get( idx ) ) );
			}
		} );
	}
}
