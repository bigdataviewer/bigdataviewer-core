package viewer.gui.brightness;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same
 * {@link BoundedValue value}.
 */
public class SliderPanel extends JPanel implements BoundedValue.UpdateListener
{
	private static final long serialVersionUID = 6444334522127424416L;

	private final JSlider slider;

	private final JSpinner spinner;

	private final BoundedValue model;

	/**
	 * Create a {@link SliderPanel} to modify a given {@link BoundedValue value}.
	 *
	 * @param name
	 *            label to show next to the slider.
	 * @param model
	 *            the value that is modified.
	 */
	public SliderPanel( final String name, final BoundedValue model, final int spinnerStepSize )
	{
		super();
		setLayout( new BorderLayout( 10, 10 ) );

		slider = new JSlider( JSlider.HORIZONTAL, model.getRangeMin(), model.getRangeMax(), model.getCurrentValue() );
		spinner = new JSpinner();
		spinner.setModel( new SpinnerNumberModel( model.getCurrentValue(), model.getRangeMin(), model.getRangeMax(), spinnerStepSize ) );

		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = slider.getValue();
				model.setCurrentValue( value );
			}
		} );

		spinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = ( ( Integer ) spinner.getValue() ).intValue();
				model.setCurrentValue( value );
			}
		} );

		final JLabel label = new JLabel( name, JLabel.CENTER );
		label.setAlignmentX( Component.CENTER_ALIGNMENT );

		add( label, BorderLayout.WEST );
		add( slider, BorderLayout.CENTER );
		add( spinner, BorderLayout.EAST );

		this.model = model;
		model.setUpdateListener( this );
	}

	@Override
	public void update()
	{
		final int value = model.getCurrentValue();
		final int min = model.getRangeMin();
		final int max = model.getRangeMax();
		if (slider.getMaximum() != max || slider.getMinimum() != min)
		{
			slider.setMinimum( min );
			slider.setMaximum( max );
			final SpinnerNumberModel spinnerModel = ( SpinnerNumberModel ) spinner.getModel();
			spinnerModel.setMinimum( min );
			spinnerModel.setMaximum( max );
		}
		slider.setValue( value );
		spinner.setValue( value );
	}
}
