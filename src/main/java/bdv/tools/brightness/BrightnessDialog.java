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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.type.numeric.ARGBType;


/**
 * Adjust brightness and colors for individual (or groups of) {@link BasicViewSetup setups}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BrightnessDialog extends JDialog
{
	public BrightnessDialog( final Frame owner, final SetupAssignments setupAssignments )
	{
		super( owner, "brightness and color", false );

		final Container content = getContentPane();

		final MinMaxPanels minMaxPanels = new MinMaxPanels( setupAssignments, this, true );
		final ColorsPanel colorsPanel = new ColorsPanel( setupAssignments );
		content.add( minMaxPanels, BorderLayout.NORTH );
		content.add( colorsPanel, BorderLayout.SOUTH );

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

		setupAssignments.setUpdateListener( new SetupAssignments.UpdateListener()
		{
			@Override
			public void update()
			{
				colorsPanel.recreateContent();
				minMaxPanels.recreateContent();
			}
		} );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	/**
	 * Adapted from http://stackoverflow.com/a/3072979/230513
	 */
	private static class ColorIcon implements Icon
	{
		private final int size = 16;

		private final Color color;

		public ColorIcon( final Color color )
		{
			this.color = color;
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setColor( color );
			g2d.fillOval( x, y, size, size );
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}

	public static class ColorsPanel extends JPanel
	{
		private final SetupAssignments setupAssignments;

		private final ArrayList< JButton > buttons;

		private final JColorChooser colorChooser;

		public ColorsPanel( final SetupAssignments assignments )
		{
			super();
			this.setupAssignments = assignments;
			buttons = new ArrayList< JButton >();
			colorChooser = new JColorChooser();

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );
			recreateContent();
		}

		public void recreateContent()
		{
			removeAll();
			buttons.clear();

			add( new JLabel( "set view colors:" ) );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
			{
				final JButton button = new JButton( new ColorIcon( getColor( setup ) ) );
				button.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						colorChooser.setColor( getColor( setup ) );
						final JDialog d = JColorChooser.createDialog( button, "Choose a color", true, colorChooser, new ActionListener()
						{
							@Override
							public void actionPerformed( final ActionEvent arg0 )
							{
								final Color c = colorChooser.getColor();
								if (c != null)
								{
									button.setIcon( new ColorIcon( c ) );
									setColor( setup, c );
								}
							}
						}, null );
						d.setVisible( true );
					}
				} );
				button.setEnabled( setup.supportsColor() );
				buttons.add( button );
				add( button );
			}

			invalidate();
			final Window frame = SwingUtilities.getWindowAncestor( this );
			if ( frame != null )
				frame.pack();
		}

		private static Color getColor( final ConverterSetup setup )
		{
			if ( setup.supportsColor() )
			{
				final int value = setup.getColor().get();
				return new Color( value );
			}
			else
				return new Color ( 0xFFBBBBBB );
		}

		private static void setColor( final ConverterSetup setup, final Color color )
		{
			setup.setColor( new ARGBType( color.getRGB() | 0xff000000 ) );
		}

		private static final long serialVersionUID = 6408468837346789676L;
	}

	public static class MinMaxPanels extends JPanel
	{
		private final SetupAssignments setupAssignments;

		private final ArrayList< MinMaxPanel > minMaxPanels;

		private final JDialog dialog;

		private final boolean rememberSizes;

		public MinMaxPanels( final SetupAssignments assignments, final JDialog dialog, final boolean rememberSizes )
		{
			super();
			this.setupAssignments = assignments;
			this.rememberSizes = rememberSizes;
			minMaxPanels = new ArrayList< MinMaxPanel >();

			setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
			setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );
			recreateContent();

			this.dialog = dialog;
		}

		public void recreateContent()
		{
			final Dimension sliderSize = ( rememberSizes && !minMaxPanels.isEmpty() ) ?
					minMaxPanels.get( 0 ).sliders.getPreferredSize() : null;

			removeAll();
			minMaxPanels.clear();

			for ( final MinMaxGroup group : setupAssignments.getMinMaxGroups() )
			{
				final MinMaxPanel panel = new MinMaxPanel( group, setupAssignments, this, rememberSizes );
				if ( sliderSize != null )
					panel.sliders.setPreferredSize( sliderSize );
				minMaxPanels.add( panel );
				add( panel );
			}

			for ( final MinMaxPanel panel : minMaxPanels )
			{
				panel.update();
				panel.showAdvanced( isShowingAdvanced );
			}

			invalidate();
			final Window frame = SwingUtilities.getWindowAncestor( this );
			if ( frame != null )
				frame.pack();
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
			if ( dialog != null )
				dialog.pack();
		}

		private static final long serialVersionUID = 6538962298579455010L;
	}

	/**
	 * A panel containing min/max {@link SliderPanel SliderPanels}, view setup check-boxes and advanced settings.
	 */
	public static class MinMaxPanel extends JPanel implements MinMaxGroup.UpdateListener
	{
		private final SetupAssignments setupAssignments;

		private final MinMaxGroup minMaxGroup;

		private final ArrayList< JCheckBox > boxes;

		private final JPanel sliders;

		private final Runnable showAdvanced;

		private final Runnable hideAdvanced;

		private boolean isShowingAdvanced;

		private final boolean rememberSizes;

		public MinMaxPanel( final MinMaxGroup group, final SetupAssignments assignments, final MinMaxPanels minMaxPanels, final boolean rememberSizes )
		{
			super();
			setupAssignments = assignments;
			minMaxGroup = group;
			this.rememberSizes = rememberSizes;
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
			if ( rememberSizes && ! minMaxPanels.minMaxPanels.isEmpty() )
			{
				final Dimension dim = minMaxPanels.minMaxPanels.get( 0 ).sliders.getSize();
				if ( dim.width > 0 )
					sliders.setPreferredSize( dim );
			}

			add( sliders, BorderLayout.CENTER );

			boxes = new ArrayList< JCheckBox >();
			final JPanel boxesPanel = new JPanel();
			boxesPanel.setLayout( new BoxLayout( boxesPanel, BoxLayout.LINE_AXIS ) );

			for ( final ConverterSetup setup : assignments.getConverterSetups() )
			{
				final JCheckBox box = new JCheckBox();
				box.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent arg0 )
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

			final JButton advancedButton = new JButton( ">>" );
			advancedButton.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
			isShowingAdvanced = false;
			advancedButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					minMaxPanels.showAdvanced( ! isShowingAdvanced );
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
					advancedButton.setText( "<<" );
					isShowingAdvanced = true;
				}
			};

			hideAdvanced = new Runnable()
			{
				@Override
				public void run()
				{
					advancedPanel.remove( spinnerRangeMin );
					advancedPanel.remove( spinnerRangeMax );
					advancedButton.setText( ">>" );
					isShowingAdvanced = false;
				}
			};

			addComponentListener( new ComponentAdapter()
			{
				@Override
				public void componentResized( final ComponentEvent e )
				{
					storeSliderSize();
				}
			} );

			final JPanel eastPanel = new JPanel();
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
			if ( rememberSizes )
			{
				final Dimension dim = sliders.getSize();
				if ( dim.width > 0 )
					sliders.setPreferredSize( dim );
			}
		}

		@Override
		public void update()
		{
			for ( int i = 0; i < boxes.size(); ++i )
			{
				final int setupId = setupAssignments.getConverterSetups().get( i ).getSetupId();
				boolean b = false;
				for ( final ConverterSetup s : minMaxGroup.setups )
					if ( s.getSetupId() == setupId )
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
