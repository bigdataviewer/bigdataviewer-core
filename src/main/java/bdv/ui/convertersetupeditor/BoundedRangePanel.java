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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import net.miginfocom.swing.MigLayout;

import org.scijava.listeners.Listeners;

import bdv.ui.UIUtils;
import bdv.ui.rangeslider.RangeSlider;
import bdv.util.BoundedRange;

/**
 * A {@code JPanel} with a range slider, min/max spinners, and a range bounds
 * display (for setting {@code ConverterSetup} display range).
 *
 * @author Tobias Pietzsch
 */
class BoundedRangePanel extends JPanel
{
	private Supplier< JPopupMenu > popup;

	public interface ChangeListener
	{
		void boundedRangeChanged();
	}

	private BoundedRange range;

	/**
	 * The range slider.
	 */
	private final RangeSlider rangeSlider;

	/**
	 * Range slider number of steps.
	 */
	private static final int SLIDER_LENGTH = 10000;

	/**
	 * The minimum spinner.
	 */
	private final JSpinner minSpinner;

	/**
	 * The maximum spinner.
	 */
	private final JSpinner maxSpinner;

	private final JLabel upperBoundLabel;

	private final JLabel lowerBoundLabel;

	private final Listeners.List< ChangeListener > listeners = new Listeners.SynchronizedList<>();

	/**
	 * Whether the range reflects a set of sources all having the same range
	 */
	private boolean isConsistent = true;

	/**
	 * Panel background if range reflects a set of sources all having the same range
	 */
	private Color consistentBg = Color.WHITE;

	/**
	 * Panel background if range reflects a set of sources with different ranges
	 */
	private Color inConsistentBg = Color.WHITE;

	public BoundedRangePanel()
	{
		this( new BoundedRange( 0, 1, 0, 0.5 ) );
	}

	public BoundedRangePanel( final BoundedRange range )
	{
		setLayout( new MigLayout( "ins 5 5 5 10, fillx, filly, hidemode 3", "[][grow][][]", "[]0[]" ) );
		updateColors();

		minSpinner = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 1.0, 1.0 ) );
		maxSpinner = new JSpinner( new SpinnerNumberModel( 1.0, 0.0, 1.0, 1.0 ) );
		rangeSlider = new RangeSlider( 0, SLIDER_LENGTH );
		upperBoundLabel = new JLabel();
		lowerBoundLabel = new JLabel();

		setupMinSpinner();
		setupMaxSpinner();
		setupRangeSlider();
		setupBoundLabels();
		setupPopupMenu();

		this.add( minSpinner, "sy 2" );
		this.add( rangeSlider, "growx, sy 2" );
		this.add( maxSpinner, "sy 2" );
		this.add( upperBoundLabel, "right, wrap" );
		this.add( lowerBoundLabel, "right" );

		setRange( range );
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		super.setEnabled( enabled );
		if ( minSpinner != null )
			minSpinner.setEnabled( enabled );
		if ( rangeSlider != null )
			rangeSlider.setEnabled( enabled );
		if ( maxSpinner != null )
			maxSpinner.setEnabled( enabled );
		if ( upperBoundLabel != null )
			upperBoundLabel.setEnabled( enabled );
		if ( lowerBoundLabel != null )
			lowerBoundLabel.setEnabled( enabled );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		updateColors();
		if ( !isConsistent )
			setBackground( inConsistentBg );
		if ( popup != null )
		{
			final JPopupMenu menu = popup.get();
			if ( menu != null )
				SwingUtilities.updateComponentTreeUI( menu );
		}
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

	@Override
	public void setBackground( final Color bg )
	{
		super.setBackground( bg );
		if ( minSpinner != null )
			minSpinner.setBackground( bg );
		if ( rangeSlider != null )
			rangeSlider.setBackground( bg );
		if ( maxSpinner != null )
			maxSpinner.setBackground( bg );
		if ( upperBoundLabel != null )
			upperBoundLabel.setBackground( bg );
		if ( lowerBoundLabel != null )
			lowerBoundLabel.setBackground( bg );
	}

	private static class UnboundedNumberEditor extends JSpinner.NumberEditor
	{
		public UnboundedNumberEditor( final JSpinner spinner )
		{
			super( spinner );
			final JFormattedTextField ftf = getTextField();
			final DecimalFormat format = ( DecimalFormat ) ( ( NumberFormatter ) ftf.getFormatter() ).getFormat();
			final NumberFormatter formatter = new NumberFormatter( format );
			formatter.setValueClass( spinner.getValue().getClass() );
			final DefaultFormatterFactory factory = new DefaultFormatterFactory( formatter );
			ftf.setFormatterFactory( factory );
		}
	}

	private void setupMinSpinner()
	{
		UIUtils.setPreferredWidth( minSpinner, 70 );

		minSpinner.addChangeListener( e -> {
			final double value = ( Double ) minSpinner.getValue();
			if ( value != range.getMin() )
				updateRange( range.withMin( value ) );
		} );

		minSpinner.setEditor( new UnboundedNumberEditor( minSpinner ) );
	}

	private void setupMaxSpinner()
	{
		UIUtils.setPreferredWidth( maxSpinner, 70 );

		maxSpinner.addChangeListener( e -> {
			final double value = ( Double ) maxSpinner.getValue();
			if ( value != range.getMax() )
				updateRange( range.withMax( value ) );
		} );

		maxSpinner.setEditor( new UnboundedNumberEditor( maxSpinner ) );
	}

	private void setupRangeSlider()
	{
		UIUtils.setPreferredWidth( rangeSlider, 50 );
		rangeSlider.setRange( 0, SLIDER_LENGTH );
		rangeSlider.setFocusable( false );

		rangeSlider.addChangeListener( e -> {
			updateRange( range.withMin( posToValue( rangeSlider.getValue() ) ).withMax( posToValue( rangeSlider.getUpperValue() ) ) );
		} );

		rangeSlider.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				updateNumberFormat();
			}
		} );
	}

	private void setupBoundLabels()
	{
		final Font font = upperBoundLabel.getFont().deriveFont( 10f );
		upperBoundLabel.setFont( font );
		lowerBoundLabel.setFont( font );
		upperBoundLabel.setBorder( null );
		lowerBoundLabel.setBorder( null );
	}

	private void setupPopupMenu()
	{
		final MouseListener ml = new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				if ( e.isPopupTrigger() ||
						( e.getButton() == MouseEvent.BUTTON1 && e.getX() > upperBoundLabel.getX() ) )
					doPop( e );
			}

			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
					doPop( e );
			}

			private void doPop( final MouseEvent e )
			{
				if ( isEnabled() && popup != null )
				{
					final JPopupMenu menu = popup.get();
					if ( menu != null )
						menu.show( e.getComponent(), e.getX(), e.getY() );
				}
			}
		};
		this.addMouseListener( ml );
		rangeSlider.addMouseListener( ml );
	}

	/**
	 * Convert range-slider position to value.
	 *
	 * @param pos
	 *            of range-slider
	 */
	private double posToValue( final int pos )
	{
		final double dmin = range.getMinBound();
		final double dmax = range.getMaxBound();
		return ( pos * ( dmax - dmin ) / SLIDER_LENGTH ) + dmin;
	}

	/**
	 * Convert value to range-slider position.
	 */
	private int valueToPos( final double value )
	{
		final double dmin = range.getMinBound();
		final double dmax = range.getMaxBound();
		return ( int ) Math.round( ( value - dmin ) * SLIDER_LENGTH / ( dmax - dmin ) );
	}

	private synchronized void updateNumberFormat()
	{
//		if ( userDefinedNumberFormat )
//			return;

		final int sw = rangeSlider.getWidth();
		if ( sw > 0 )
		{
			final double vrange = range.getMaxBound() - range.getMinBound();
			final int digits = ( int ) Math.ceil( Math.log10( sw / vrange ) );

			blockUpdates = true;

			JSpinner.NumberEditor numberEditor = ( ( JSpinner.NumberEditor ) minSpinner.getEditor() );
			numberEditor.getFormat().setMaximumFractionDigits( digits );
			numberEditor.stateChanged( new ChangeEvent( minSpinner ) );

			numberEditor = ( ( JSpinner.NumberEditor ) maxSpinner.getEditor() );
			numberEditor.getFormat().setMaximumFractionDigits( digits );
			numberEditor.stateChanged( new ChangeEvent( maxSpinner ) );

			blockUpdates = false;
		}
	}

	private synchronized void updateRange( final BoundedRange newRange )
	{
		if ( !blockUpdates )
			setRange( newRange );
	}

	private boolean blockUpdates = false;

	public synchronized void setRange( final BoundedRange range )
	{
		if ( Objects.equals( this.range, range ) )
			return;

		this.range = range;

		blockUpdates = true;

		final double minBound = range.getMinBound();
		final double maxBound = range.getMaxBound();

		final SpinnerNumberModel minSpinnerModel = ( SpinnerNumberModel ) minSpinner.getModel();
		minSpinnerModel.setMinimum( minBound );
		minSpinnerModel.setMaximum( maxBound );
		minSpinnerModel.setValue( range.getMin() );

		final SpinnerNumberModel maxSpinnerModel = ( SpinnerNumberModel ) maxSpinner.getModel();
		maxSpinnerModel.setMinimum( minBound );
		maxSpinnerModel.setMaximum( maxBound );
		maxSpinnerModel.setValue( range.getMax() );

		rangeSlider.setRange( valueToPos( range.getMin() ), valueToPos( range.getMax() ) );

		final double frac = Math.max(
				Math.abs( Math.round( minBound ) - minBound ),
				Math.abs( Math.round( maxBound ) - maxBound ) );
		final String format = frac > 0.005 ? "%.2f" : "%.0f";
		upperBoundLabel.setText( String.format( format, maxBound ) );
		lowerBoundLabel.setText( String.format( format, minBound ) );
		this.invalidate();

		blockUpdates = false;

		listeners.list.forEach( ChangeListener::boundedRangeChanged );
	}

	public BoundedRange getRange()
	{
		return range;
	}

	public Listeners< ChangeListener > changeListeners()
	{
		return listeners;
	}

	public void setPopup( final Supplier< JPopupMenu > popup )
	{
		this.popup = popup;
	}

	public void shrinkBoundsToRange()
	{
		updateRange( range.withMinBound( range.getMin() ).withMaxBound( range.getMax() ) );
	}

	public void setBoundsDialog()
	{
		final JPanel panel = new JPanel( new MigLayout( "fillx", "[][grow]", "" ) );
		final JSpinner minSpinner = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 1.0, 1.0 ) );
		final JSpinner maxSpinner = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 1.0, 1.0 ) );
		minSpinner.setEditor( new UnboundedNumberEditor( minSpinner ) );
		maxSpinner.setEditor( new UnboundedNumberEditor( maxSpinner ) );
		minSpinner.setValue( range.getMinBound() );
		maxSpinner.setValue( range.getMaxBound() );
		minSpinner.addChangeListener( e -> {
			final double value = ( Double ) minSpinner.getValue();
			if ( value > ( Double ) maxSpinner.getValue() )
				maxSpinner.setValue( value );
		} );
		maxSpinner.addChangeListener( e -> {
			final double value = ( Double ) maxSpinner.getValue();
			if ( value < ( Double ) minSpinner.getValue() )
				minSpinner.setValue( value );
		} );
		panel.add( "right", new JLabel( "min" ) );
		panel.add( "growx, wrap", minSpinner );
		panel.add( "right", new JLabel( "max" ) );
		panel.add( "growx", maxSpinner );
		final int result = JOptionPane.showConfirmDialog( null, panel, "Set Bounds", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
		if ( result == JOptionPane.YES_OPTION )
		{
			final double min = ( Double ) minSpinner.getValue();
			final double max = ( Double ) maxSpinner.getValue();
			updateRange( range.withMinBound( min ).withMaxBound( max ) );
		}
	}
}
