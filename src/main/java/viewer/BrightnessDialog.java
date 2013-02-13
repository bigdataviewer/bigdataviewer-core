package viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BrightnessDialog extends JDialog
{
	public interface MinMaxListener
	{
		public void setMinMax( final int min, final int max );
	}

	private MinMaxListener listener;

	public void setListener( final MinMaxListener listener )
	{
		this.listener = listener;
	}

	public BrightnessDialog( final Frame owner )
	{
		super( owner, "display range", false );
		listener = null;

		final Container content = getContentPane();
		final JPanel sliders = new JPanel();
		sliders.setLayout( new BoxLayout( sliders, BoxLayout.PAGE_AXIS ) );

		final SliderPanel minPanel = new SliderPanel( "min", 0, 65534, 0, 1 );
		minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
		sliders.add( minPanel );
		final SliderPanel maxPanel = new SliderPanel( "max", 1, 65535, 65535, 1 );
		maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
		sliders.add( maxPanel );

		minPanel.getSlider().addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int min = minPanel.getValue();
				int max = maxPanel.getValue();
				if ( max < min + 1 )
				{
					max = min + 1;
					maxPanel.setValue( max );
				}
				if ( listener != null )
					listener.setMinMax( min, max );
			}
		} );

		maxPanel.getSlider().addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int max = maxPanel.getValue();
				int min = minPanel.getValue();
				if ( min > max - 1 )
				{
					min = max - 1;
					minPanel.setValue( min );
				}
				if ( listener != null )
					listener.setMinMax( min, max );
			}
		} );

		content.add( sliders, BorderLayout.NORTH );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			private static final long serialVersionUID = -110094795301286228L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	static class SliderPanel extends JPanel
	{
		private final JSlider slider;

		private final JSpinner spinner;

		public SliderPanel( final String name, final int minimum, final int maximum, final int value, final int spinnerStepSize )
		{
			super();
			setLayout( new BorderLayout( 10, 10 ) );

			slider = new JSlider( JSlider.HORIZONTAL, minimum, maximum, value );
			spinner = new JSpinner();
			spinner.setModel( new SpinnerNumberModel( value, minimum, maximum, spinnerStepSize ) );

			slider.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					spinner.setValue( slider.getValue() );
				}
			} );

			spinner.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					slider.setValue( ( ( Integer ) spinner.getValue() ).intValue() );
				}
			} );

			final JLabel label = new JLabel( name, JLabel.CENTER );
			label.setAlignmentX( Component.CENTER_ALIGNMENT );

			add( label, BorderLayout.WEST );
			add( slider, BorderLayout.CENTER );
			add( spinner, BorderLayout.EAST );
		}

		public JSlider getSlider()
		{
			return slider;
		}

		public int getValue()
		{
			return slider.getValue();
		}

		public void setValue( final int value )
		{
			slider.setValue( value );
			spinner.setValue( value );
		}

		private static final long serialVersionUID = -833454714700639442L;
	}

	private static final long serialVersionUID = -4010513274385814205L;
}
