/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.tools.brightness;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.util.BoundedValue;
import bdv.util.BoundedValueDouble;

/**
 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same
 * {@link BoundedValue value}.
 */
public class SliderPanelDouble extends JPanel implements BoundedValueDouble.UpdateListener
{
	private static final long serialVersionUID = 6444334522127424416L;

	private static final int sliderLength = 10000;

	private final JSlider slider;

	private final JSpinner spinner;

	private final BoundedValueDouble model;

	private double dmin;

	private double dmax;

	/**
	 * Create a {@link SliderPanelDouble} to modify a given {@link BoundedValueDouble value}.
	 *
	 * @param name
	 *            label to show next to the slider.
	 * @param model
	 *            the value that is modified.
	 * @param spinnerStepSize
	 * @param decimalFormat
	 */
	public SliderPanelDouble(
			final String name,
			final BoundedValueDouble model,
			final double spinnerStepSize )
	{
		super();
		setLayout( new BorderLayout( 10, 10 ) );

		dmin = model.getRangeMin();
		dmax = model.getRangeMax();

		slider = new JSlider( SwingConstants.HORIZONTAL, 0, sliderLength, toSlider( model.getCurrentValue() ) );
		spinner = new JSpinner();
		spinner.setModel( new SpinnerNumberModel( model.getCurrentValue(), dmin, dmax, spinnerStepSize ) );

		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = slider.getValue();
				model.setCurrentValue( fromSlider( value ) );
			}
		} );

		spinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final double value = ( ( Double ) spinner.getValue() ).doubleValue();
				model.setCurrentValue( value );
			}
		} );

		if ( name != null )
		{
			final JLabel label = new JLabel( name, SwingConstants.CENTER );
			label.setAlignmentX( Component.CENTER_ALIGNMENT );
			add( label, BorderLayout.WEST );
		}

		add( slider, BorderLayout.CENTER );
		add( spinner, BorderLayout.EAST );

		this.model = model;
		model.setUpdateListener( this );
	}

	public void setDecimalFormat( final String pattern )
	{
		( ( JSpinner.NumberEditor ) spinner.getEditor() ).getFormat().applyPattern( pattern );
	}

	public void setNumColummns( final int cols )
	{
		( ( JSpinner.NumberEditor ) spinner.getEditor() ).getTextField().setColumns( cols );
	}

	@Override
	public void update()
	{
		final double value = model.getCurrentValue();
		final double min = model.getRangeMin();
		final double max = model.getRangeMax();
		if ( dmax != max || dmin != min)
		{
			dmin = min;
			dmax = max;
			final SpinnerNumberModel spinnerModel = ( SpinnerNumberModel ) spinner.getModel();
			spinnerModel.setMinimum( min );
			spinnerModel.setMaximum( max );
		}
		slider.setValue( toSlider( value ) );
		spinner.setValue( value );
	}

	private int toSlider( final double value )
	{
		return ( int ) Math.round( ( value - dmin ) * sliderLength / ( dmax - dmin ) );
	}

	private double fromSlider( final int value )
	{
		return ( value * ( dmax - dmin ) / sliderLength ) + dmin;
	}
}
