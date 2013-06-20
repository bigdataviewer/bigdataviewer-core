package viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
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

public class NewBrightnessDialog extends JDialog
{
	public NewBrightnessDialog( final Frame owner, final ArrayList< ConverterSetup > converterSetups )
	{
		super( owner, "display range", false );

		final Container content = getContentPane();

		final SetupAssignments assignments = new SetupAssignments( converterSetups, 0, 65535 );
		final MinMaxPanels minMaxPanels = new MinMaxPanels( assignments, this );
		content.add( minMaxPanels, BorderLayout.NORTH );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}

			private static final long serialVersionUID = 3904286091931838921L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	public interface ConverterSetup
	{
		public int getSetupId();

		public void setDisplayRange( int min, int max );

		public int getDisplayRangeMin();

		public int getDisplayRangeMax();
	}

	public static class MinMaxPanels extends JPanel implements SetupAssignments.UpdateListener
	{
		private final SetupAssignments setupAssignments;

		private final ArrayList< MinMaxPanel > minMaxPanels;

		private final JDialog dialog;

		public MinMaxPanels( final SetupAssignments assignments, final JDialog dialog )
		{
			super();
			this.setupAssignments = assignments;

			setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
			setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );

			minMaxPanels = new ArrayList< MinMaxPanel >();
			for ( final MinMaxGroup group : setupAssignments.minMaxGroups )
			{
				final MinMaxPanel panel = new MinMaxPanel( group, setupAssignments );
				minMaxPanels.add( panel );
				add( panel );
			}

			for ( final MinMaxPanel panel : minMaxPanels )
				panel.update();

			this.dialog = dialog;
			assignments.setUpdateListener( this );
		}

		@Override
		public void update()
		{
			final ArrayList< MinMaxPanel > panelsToRemove = new ArrayList< MinMaxPanel >();
			for ( final MinMaxPanel panel : minMaxPanels )
				if( ! setupAssignments.minMaxGroups.contains( panel.minMaxGroup ) )
					panelsToRemove.add( panel );
			minMaxPanels.removeAll( panelsToRemove );
			for ( final MinMaxPanel panel : panelsToRemove )
				remove( panel );

A:			for ( final MinMaxGroup group : setupAssignments.minMaxGroups )
			{
				for ( final MinMaxPanel panel : minMaxPanels )
					if ( panel.minMaxGroup == group )
						continue A;
				final MinMaxPanel panel = new MinMaxPanel( group, setupAssignments );
				minMaxPanels.add( panel );
				add( panel );
				panel.update();
			}

//			revalidate();
			dialog.pack();
		}

		private static final long serialVersionUID = 6538962298579455010L;
	}

	public static class SetupAssignments
	{
		protected final int typeMin;

		protected final int typeMax;

		protected final ArrayList< ConverterSetup > setups;

		protected final ArrayList< MinMaxGroup > minMaxGroups;

		protected final Map< ConverterSetup, MinMaxGroup > setupToGroup;

		public interface UpdateListener
		{
			public void update();
		}

		private UpdateListener updateListener;

		public SetupAssignments( final ArrayList< ConverterSetup > converterSetups, final int fullRangeMin, final int fullRangeMax )
		{
			typeMin = fullRangeMin;
			typeMax = fullRangeMax;
			setups = new ArrayList< ConverterSetup >( converterSetups );
			minMaxGroups = new ArrayList< MinMaxGroup >();
			setupToGroup = new HashMap< ConverterSetup, MinMaxGroup >();
			for ( final ConverterSetup setup : setups )
			{
				final MinMaxGroup group = new MinMaxGroup( typeMin, typeMax, setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
				minMaxGroups.add( group );
				setupToGroup.put( setup, group );
				group.addSetup( setup );
			}
			updateListener = null;
		}

		public void moveSetupToGroup( final ConverterSetup setup, final MinMaxGroup group )
		{
			final MinMaxGroup oldGroup = setupToGroup.get( setup );
			if ( oldGroup == group )
				return;

			setupToGroup.put( setup, group );
			group.addSetup( setup );

			final boolean oldGroupIsEmpty = oldGroup.removeSetup( setup );
			if ( oldGroupIsEmpty )
				minMaxGroups.remove( oldGroup );

			if ( updateListener != null )
				updateListener.update();
		}

		public void removeSetupFromGroup( final ConverterSetup setup, final MinMaxGroup group )
		{
			if ( setupToGroup.get( setup ) != group )
				return;

			final MinMaxGroup newGroup = new MinMaxGroup( typeMin, typeMax, setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
			minMaxGroups.add( newGroup );
			setupToGroup.put( setup, newGroup );
			newGroup.addSetup( setup );

			final boolean groupIsEmpty = group.removeSetup( setup );
			if ( groupIsEmpty )
				minMaxGroups.remove( group );

			if ( updateListener != null )
				updateListener.update();
		}

		public void setUpdateListener( final UpdateListener l )
		{
			updateListener = l;
		}
	}

	public static class MinMaxGroup
	{
		private final BoundedValueImp minValue;

		private final BoundedValueImp maxValue;

		private final BoundedValue minBoundedValue;

		private final BoundedValue maxBoundedValue;

		public interface UpdateListener
		{
			public void update();
		}

		private UpdateListener updateListener;

		private final Set< ConverterSetup > setups;

		public MinMaxGroup( final int rangeMin, final int rangeMax, final int currentMin, final int currentMax )
		{
			minValue = new BoundedValueImp( rangeMin, rangeMax - 1, currentMin );
			maxValue = new BoundedValueImp( rangeMin + 1, rangeMax, currentMax );
			minBoundedValue = new BoundedValue()
			{
				@Override
				public int getRangeMin()
				{
					return minValue.getRangeMin();
				}

				@Override
				public int getRangeMax()
				{
					return minValue.getRangeMax();
				}

				@Override
				public int getCurrentValue()
				{
					return minValue.getCurrentValue();
				}

				@Override
				public void setRange( final int min, final int max )
				{
					throw new UnsupportedOperationException();
				}

				@Override
				public void setCurrentValue( final int value )
				{
					setCurrentMin( value );
				}

				@Override
				public void setUpdateListener( final UpdateListener l )
				{
					minValue.setUpdateListener( l );
				}
			};

			maxBoundedValue = new BoundedValue()
			{
				@Override
				public int getRangeMin()
				{
					return maxValue.getRangeMin();
				}

				@Override
				public int getRangeMax()
				{
					return maxValue.getRangeMax();
				}

				@Override
				public int getCurrentValue()
				{
					return maxValue.getCurrentValue();
				}

				@Override
				public void setRange( final int min, final int max )
				{
					throw new UnsupportedOperationException();
				}

				@Override
				public void setCurrentValue( final int value )
				{
					setCurrentMax( value );
				}

				@Override
				public void setUpdateListener( final UpdateListener l )
				{
					maxValue.setUpdateListener( l );
				}
			};

			setups = new LinkedHashSet< ConverterSetup >();

			updateListener = null;
		}

		public BoundedValue getMinBoundedValue()
		{
			return minBoundedValue;
		}

		public BoundedValue getMaxBoundedValue()
		{
			return maxBoundedValue;
		}

		public int getRangeMin()
		{
			return minValue.getRangeMin();
		}

		public int getRangeMax()
		{
			return maxValue.getRangeMax();
		}

		protected int getCurrentMin()
		{
			return minValue.getCurrentValue();
		}

		protected int getCurrentMax()
		{
			return maxValue.getCurrentValue();
		}

		public void setRange( final int min, final int max )
		{
			assert min < max;
			minValue.setRange( min, max - 1 );
			maxValue.setRange( min + 1, max );
			final int currentMin = getCurrentMin();
			final int currentMax = getCurrentMax();
			if ( currentMin >= currentMax )
			{
				if ( currentMax == max )
					minValue.setCurrentValue( currentMax - 1 );
				else
					maxValue.setCurrentValue( currentMin + 1 );
			}
		}

		public void setCurrentMin( final int value )
		{
			minValue.setCurrentValue( value );
			if ( value >= getCurrentMax() )
				maxValue.setCurrentValue( value + 1 );
			for ( final ConverterSetup setup : setups )
				setup.setDisplayRange( getCurrentMin(), getCurrentMax() );
		}

		public void setCurrentMax( final int value )
		{
			maxValue.setCurrentValue( value );
			if ( value <= getCurrentMin() )
				minValue.setCurrentValue( value - 1 );
			for ( final ConverterSetup setup : setups )
				setup.setDisplayRange( getCurrentMin(), getCurrentMax() );
		}

		public void addSetup( final ConverterSetup setup )
		{
			setups.add( setup );
			setup.setDisplayRange( getCurrentMin(), getCurrentMax() );

			if ( updateListener != null )
				updateListener.update();
		}

		public boolean removeSetup( final ConverterSetup setup )
		{
			setups.remove( setup );

			if ( updateListener != null )
				updateListener.update();

			return setups.isEmpty();
		}

		public void setUpdateListener( final UpdateListener l )
		{
			updateListener = l;
		}
	}

	/**
	 * A panel containing min/max {@link SliderPanel SliderPanels}, view setup check-boxes and advanced settings.
	 */
	public static class MinMaxPanel extends JPanel implements MinMaxGroup.UpdateListener
	{
		protected final MinMaxGroup minMaxGroup;

		protected final ArrayList< JCheckBox > boxes;

		public MinMaxPanel( final MinMaxGroup group, final SetupAssignments assignments )
		{
			super();
			minMaxGroup = group;
//			setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createLineBorder( Color.black ) ) );
			setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createEtchedBorder() ) );
			setLayout( new BorderLayout( 10, 10 ) );

			final JPanel sliders = new JPanel();
			sliders.setLayout( new BoxLayout( sliders, BoxLayout.PAGE_AXIS ) );

			final SliderPanel minPanel = new SliderPanel( "min", group.getMinBoundedValue(), 1 );
			minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			sliders.add( minPanel );
			final SliderPanel maxPanel = new SliderPanel( "max", group.getMaxBoundedValue(), 1 );
			maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			sliders.add( maxPanel );

			add( sliders, BorderLayout.CENTER );

			boxes = new ArrayList< JCheckBox >();
			final JPanel boxesPanel = new JPanel();
			boxesPanel.setLayout( new BoxLayout( boxesPanel, BoxLayout.LINE_AXIS ) );

			for ( final ConverterSetup setup : assignments.setups )
			{
				final JCheckBox box = new JCheckBox();
//				box.addActionListener( new ActionListener()
//				{
//
//					@Override
//					public void actionPerformed( final ActionEvent arg0 )
//					{
//						if ( box.isSelected() )
//							assignments.moveSetupToGroup( setup, minMaxGroup );
//						else
//							assignments.removeSetupFromGroup( setup, minMaxGroup );
//					}
//				} );
				box.addChangeListener( new ChangeListener()
				{
					@Override
					public void stateChanged( final ChangeEvent e )
					{
						if ( box.isSelected() )
							assignments.moveSetupToGroup( setup, minMaxGroup );
						else
							assignments.removeSetupFromGroup( setup, minMaxGroup );
					}
				} );
				boxesPanel.add( box );
				boxes.add( box );
			}

			minMaxGroup.setUpdateListener( this );
			add( boxesPanel, BorderLayout.EAST );
		}

		@Override
		public void update()
		{
			for ( int i = 0; i < boxes.size(); ++i )
			{
				boolean b = false;
				for ( final ConverterSetup s : minMaxGroup.setups )
					if ( s.getSetupId() == i )
					{
						b = true;
						break;
					}
				boxes.get( i ).setSelected( b );
			}
		}

		private static final long serialVersionUID = -5209143847804383789L;
	}

	public interface BoundedValue
	{
		public int getRangeMin();

		public int getRangeMax();

		public int getCurrentValue();

		public void setRange( final int min, final int max );

		public void setCurrentValue( final int value );

		public interface UpdateListener
		{
			public void update();
		}

		public void setUpdateListener( final UpdateListener l );
	}

	public static class BoundedValueImp implements BoundedValue
	{
		private int rangeMin;

		private int rangeMax;

		private int currentValue;

		private UpdateListener updateListener;

		public BoundedValueImp( final int rangeMin, final int rangeMax, final int currentValue )
		{
			this.rangeMin = rangeMin;
			this.rangeMax = rangeMax;
			this.currentValue = currentValue;
			updateListener = null;
		}

		@Override
		public int getRangeMin()
		{
			return rangeMin;
		}

		@Override
		public int getRangeMax()
		{
			return rangeMax;
		}

		@Override
		public int getCurrentValue()
		{
			return currentValue;
		}

		@Override
		public void setRange( final int min, final int max )
		{
			assert min <= max;
			rangeMin = min;
			rangeMax = max;
			currentValue = Math.min( Math.max( currentValue, min ), max );

			if ( updateListener != null )
				updateListener.update();
		}

		@Override
		public void setCurrentValue( final int value )
		{
			currentValue = value;

			if ( currentValue < rangeMin )
				currentValue = rangeMin;
			else if ( currentValue > rangeMax )
				currentValue = rangeMax;

			if ( updateListener != null )
				updateListener.update();
		}

		@Override
		public void setUpdateListener( final UpdateListener l )
		{
			updateListener = l;
		}
	}

	/**
	 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same value;
	 */
	public static class SliderPanel extends JPanel implements BoundedValue.UpdateListener
	{
		protected final JSlider slider;

		protected final JSpinner spinner;

		protected final BoundedValue model;

		public SliderPanel( final String name, final BoundedValue model, final int spinnerStepSize )
		{
			super();
			setLayout( new BorderLayout( 10, 10 ) );

			slider = new JSlider( JSlider.HORIZONTAL, model.getRangeMin(), model.getRangeMax(), model.getCurrentValue() );
			spinner = new JSpinner();
			spinner.setModel( new SpinnerNumberModel( model.getCurrentValue(),  model.getRangeMin(), model.getRangeMax(), spinnerStepSize ) );

			slider.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int value = slider.getValue();
//					spinner.setValue( value );
					model.setCurrentValue( value );
				}
			} );

			spinner.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int value = ( ( Integer ) spinner.getValue() ).intValue();
//					slider.setValue( value );
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

		public JSlider getSlider()
		{
			return slider;
		}

		private static final long serialVersionUID = 4525482867645787419L;

		@Override
		public void update()
		{
			final int value = model.getCurrentValue();
			final int min = model.getRangeMin();
			final int max = model.getRangeMax();
			if ( slider.getMaximum() != max || slider.getMinimum() != min )
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

	private static final long serialVersionUID = 7963632306732311403L;
}
