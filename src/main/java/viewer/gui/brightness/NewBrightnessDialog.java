package viewer.gui.brightness;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
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
				final MinMaxPanel panel = new MinMaxPanel( group, setupAssignments, dialog, this );
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
			for ( final MinMaxPanel panel : minMaxPanels )
				panel.storeSliderSize();

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
				final MinMaxPanel panel = new MinMaxPanel( group, setupAssignments, dialog, this );
				minMaxPanels.add( panel );
				add( panel );
				panel.update();
				panel.showAdvanced( isShowingAdvanced );
			}

			dialog.pack();
		}

		private boolean isShowingAdvanced = false;

		public void showAdvanced( final boolean b )
		{
			isShowingAdvanced = b;
			for( final MinMaxPanel panel : minMaxPanels )
			{
				panel.storeSliderSize();
				panel.showAdvanced( isShowingAdvanced );
			}
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
				final MinMaxGroup group = new MinMaxGroup( typeMin, typeMax, typeMin, typeMax, setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
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

			final MinMaxGroup newGroup = new MinMaxGroup( group.getFullRangeMin(), group.getFullRangeMax(), group.getRangeMin(), group.getRangeMax(), setup.getDisplayRangeMin(), setup.getDisplayRangeMax() );
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

	/**
	 * A panel containing min/max {@link SliderPanel SliderPanels}, view setup check-boxes and advanced settings.
	 */
	public static class MinMaxPanel extends JPanel implements MinMaxGroup.UpdateListener
	{
		private final MinMaxGroup minMaxGroup;

		private final ArrayList< JCheckBox > boxes;

		private final JPanel sliders;

		private final Runnable showAdvanced;

		private final Runnable hideAdvanced;

		public MinMaxPanel( final MinMaxGroup group, final SetupAssignments assignments, final JDialog dialog, final MinMaxPanels minMaxPanels )
		{
			super();
			minMaxGroup = group;
//			setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createLineBorder( Color.black ) ) );
			setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ), BorderFactory.createEtchedBorder() ) );
			setLayout( new BorderLayout( 10, 10 ) );

			sliders = new JPanel();
			sliders.setLayout( new BoxLayout( sliders, BoxLayout.PAGE_AXIS ) );

			final SliderPanel minPanel = new SliderPanel( "min", group.getMinBoundedValue(), 1 );
			minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			sliders.add( minPanel );
			final SliderPanel maxPanel = new SliderPanel( "max", group.getMaxBoundedValue(), 1 );
			maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			sliders.add( maxPanel );
			if ( ! minMaxPanels.minMaxPanels.isEmpty() )
			{
				final Dimension dim = minMaxPanels.minMaxPanels.get( 0 ).sliders.getSize();
				if ( dim.width > 0 )
					sliders.setPreferredSize( dim );
			}

			add( sliders, BorderLayout.CENTER );

			boxes = new ArrayList< JCheckBox >();
			final JPanel boxesPanel = new JPanel();
			boxesPanel.setLayout( new BoxLayout( boxesPanel, BoxLayout.LINE_AXIS ) );

			for ( final ConverterSetup setup : assignments.setups )
			{
				final JCheckBox box = new JCheckBox();
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

			final JPanel advancedPanel = new JPanel();
			advancedPanel.setLayout( new BoxLayout( advancedPanel, BoxLayout.PAGE_AXIS ) );

			final JSpinner dummy = new JSpinner();
			dummy.setModel( new SpinnerNumberModel( minMaxGroup.getRangeMax(), minMaxGroup.getFullRangeMin(), minMaxGroup.getFullRangeMax(), 1 ) );
			dummy.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			final Dimension ps = dummy.getPreferredSize();

			final JSpinner spinnerRangeMin = new JSpinner();
			spinnerRangeMin.setModel( new SpinnerNumberModel( minMaxGroup.getRangeMin(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1 ) );
			spinnerRangeMin.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int value = ( ( Integer ) spinnerRangeMin.getValue() ).intValue();
					if ( value < minMaxGroup.getFullRangeMin() )
						spinnerRangeMin.setValue( minMaxGroup.getFullRangeMin() );
					else if ( value > minMaxGroup.getRangeMax() - 1 )
						spinnerRangeMin.setValue( minMaxGroup.getRangeMax() - 1);
					else
						minMaxGroup.setRange( value, minMaxGroup.getRangeMax() );
				}
			} );
			spinnerRangeMin.setPreferredSize( ps );
			spinnerRangeMin.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			final JSpinner spinnerRangeMax = new JSpinner();
			spinnerRangeMax.setModel( new SpinnerNumberModel( minMaxGroup.getRangeMax(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1 ) );
			spinnerRangeMax.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int value = ( ( Integer ) spinnerRangeMax.getValue() ).intValue();
					if ( value < minMaxGroup.getRangeMin() + 1 )
						spinnerRangeMax.setValue( minMaxGroup.getRangeMin() + 1 );
					else if ( value > minMaxGroup.getFullRangeMax() )
						spinnerRangeMax.setValue( minMaxGroup.getFullRangeMax() );
					else
						minMaxGroup.setRange( minMaxGroup.getRangeMin(), value );
				}
			} );
			spinnerRangeMax.setPreferredSize( ps );
			spinnerRangeMax.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );

			final JButton advancedButton = new JButton( ">" );
			advancedButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					minMaxPanels.showAdvanced( advancedButton.getText().equals( ">" ) );
					dialog.pack();
				}
			} );
			boxesPanel.add( advancedButton );

			showAdvanced = new Runnable()
			{
				@Override
				public void run()
				{
					advancedPanel.add( spinnerRangeMin );
					advancedPanel.add( spinnerRangeMax );
					advancedButton.setText( "<" );
				}
			};

			hideAdvanced = new Runnable()
			{
				@Override
				public void run()
				{
					advancedPanel.remove( spinnerRangeMin );
					advancedPanel.remove( spinnerRangeMax );
					advancedButton.setText( ">" );
				}
			};

			final JPanel eastPanel = new JPanel();
//			eastPanel.setLayout( new BorderLayout( 10, 10 ) );
			eastPanel.setLayout( new BoxLayout( eastPanel, BoxLayout.LINE_AXIS ) );
			eastPanel.add( boxesPanel, BorderLayout.CENTER );
			eastPanel.add( advancedPanel, BorderLayout.EAST );
			add( eastPanel, BorderLayout.EAST );
		}

		public void showAdvanced( final boolean b )
		{
			if ( b )
				showAdvanced.run();
			else
				hideAdvanced.run();
		}

		public void storeSliderSize()
		{
			final Dimension dim = sliders.getSize();
			if ( dim.width > 0 )
				sliders.setPreferredSize( dim );
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

	private static final long serialVersionUID = 7963632306732311403L;
}
