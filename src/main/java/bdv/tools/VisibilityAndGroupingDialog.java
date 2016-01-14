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
package bdv.tools;

import static bdv.viewer.VisibilityAndGrouping.Event.CURRENT_GROUP_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.GROUP_ACTIVITY_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.GROUP_NAME_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.NUM_SOURCES_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static bdv.viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.state.SourceGroup;

public class VisibilityAndGroupingDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private final VisibilityPanel visibilityPanel;

	private final GroupingPanel groupingPanel;

	private final ModePanel modePanel;

	public VisibilityAndGroupingDialog( final Frame owner, final VisibilityAndGrouping visibilityAndGrouping )
	{
		super( owner, "visibility and grouping", false );

		visibilityPanel = new VisibilityPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( visibilityPanel );
		visibilityPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"visibility" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		groupingPanel = new GroupingPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( groupingPanel );
		groupingPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"grouping" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		modePanel = new ModePanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( modePanel );

		final JPanel content = new JPanel();
		content.setLayout( new BoxLayout( content, BoxLayout.PAGE_AXIS ) );
		content.add( visibilityPanel );
		content.add( groupingPanel );
		content.add( modePanel );
		getContentPane().add( content, BorderLayout.NORTH );

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	public void update()
	{
		visibilityPanel.update();
		groupingPanel.update();
		modePanel.update();
	}

	public static class VisibilityPanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
		private static final long serialVersionUID = 1L;

		private final VisibilityAndGrouping visibility;

		private final ArrayList< JRadioButton > currentButtons;

		private final ArrayList< JCheckBox > fusedBoxes;

		private final ArrayList< JCheckBox > visibleBoxes;

		public VisibilityPanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			currentButtons = new ArrayList< JRadioButton >();
			fusedBoxes = new ArrayList< JCheckBox >();
			visibleBoxes = new ArrayList< JCheckBox >();
			recreateContent();
			update();
		}

		protected void recreateContent()
		{
			removeAll();
			currentButtons.clear();
			fusedBoxes.clear();
			visibleBoxes.clear();

			final int numSources = visibility.numSources();
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			// source names
			c.gridx = 0;
			c.gridy = 0;
			add( new JLabel( "source" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
				add( new JLabel( visibility.getSources().get( i ).getSpimSource().getName() ), c );

			// "current" radio-buttons
			c.anchor = GridBagConstraints.CENTER;
			c.gridx = 1;
			c.gridy = 0;
			add( new JLabel( "current" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			final ButtonGroup currentButtonGroup = new ButtonGroup();
			for ( int i = 0; i < numSources; ++i )
			{
				final JRadioButton b = new JRadioButton();
				final int sourceIndex = i;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						if ( b.isSelected() )
							visibility.setCurrentSource( sourceIndex );
					}
				} );
				currentButtons.add( b );
				currentButtonGroup.add( b );
				add( b, c );
			}

			// "active in fused" check-boxes
			c.gridx = 2;
			c.gridy = 0;
			add( new JLabel( "active in fused" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
			{
				final JCheckBox b = new JCheckBox();
				final int sourceIndex = i;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						visibility.setSourceActive( sourceIndex, b.isSelected() );
					}
				} );
				fusedBoxes.add( b );
				add( b, c );
			}

			// "currently visible" check-boxes
			c.gridx = 3;
			c.gridy = 0;
			add( new JLabel( "visible" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
			{
				final JCheckBox b = new JCheckBox();
				visibleBoxes.add( b );
				b.setEnabled( false );
				add( b, c );
			}

			invalidate();
			final Window frame = SwingUtilities.getWindowAncestor( this );
			if ( frame != null )
				frame.pack();
		}

		protected void update()
		{
			synchronized ( visibility )
			{
				final int numSources = visibility.numSources();
				if ( currentButtons.size() != numSources )
					recreateContent();

				currentButtons.get( visibility.getCurrentSource() ).setSelected( true );
				for ( int i = 0; i < numSources; ++i )
				{
					fusedBoxes.get( i ).setSelected( visibility.isSourceActive( i ) );
					visibleBoxes.get( i ).setSelected( visibility.isSourceVisible( i ) );
				}
			}
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			synchronized ( visibility )
			{
				if ( currentButtons.size() != visibility.numSources() )
					recreateContent();

				switch ( e.id )
				{
				case CURRENT_SOURCE_CHANGED:
				case SOURCE_ACTVITY_CHANGED:
				case VISIBILITY_CHANGED:
				case NUM_SOURCES_CHANGED:
					update();
					break;
				}
			}
		}
	}

	public static class ModePanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
		private static final long serialVersionUID = 1L;

		private final VisibilityAndGrouping visibility;

		private JCheckBox groupingBox;

		private JCheckBox fusedModeBox;

		public ModePanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			recreateContent();
			update();
		}

		protected void recreateContent()
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_START;
			groupingBox = new JCheckBox();
			groupingBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					visibility.setGroupingEnabled( groupingBox.isSelected() );
				}
			} );
			c.gridx = 0;
			c.gridy = 0;
			add( groupingBox, c );
			c.gridx = 1;
			add( new JLabel("enable grouping"), c );

			fusedModeBox = new JCheckBox();
			fusedModeBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					visibility.setFusedEnabled( fusedModeBox.isSelected() );
				}
			} );
			c.gridx = 0;
			c.gridy = 1;
			add( fusedModeBox, c );
			c.gridx = 1;
			add( new JLabel("enable fused mode"), c );
		}

		protected void update()
		{
			synchronized ( visibility )
			{
				groupingBox.setSelected( visibility.isGroupingEnabled() );
				fusedModeBox.setSelected( visibility.isFusedEnabled() );
			}
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			synchronized ( visibility )
			{
				switch ( e.id )
				{
				case DISPLAY_MODE_CHANGED:
					groupingBox.setSelected( visibility.isGroupingEnabled() );
					fusedModeBox.setSelected( visibility.isFusedEnabled() );
					break;
				}
			}
		}
	}

	public static class GroupingPanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
		private static final long serialVersionUID = 1L;

		private final VisibilityAndGrouping visibility;

		private final ArrayList< JTextField > nameFields;

		private final ArrayList< JRadioButton > currentButtons;

		private final ArrayList< JCheckBox > fusedBoxes;

		private final ArrayList< JCheckBox > assignBoxes;

		private int numSources;

		private int numGroups;

		public GroupingPanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			nameFields = new ArrayList< JTextField >();
			currentButtons = new ArrayList< JRadioButton >();
			fusedBoxes = new ArrayList< JCheckBox >();
			assignBoxes = new ArrayList< JCheckBox >();
			numSources = visibilityAndGrouping.numSources();
			numGroups = visibilityAndGrouping.numGroups();
			recreateContent();
			update();
		}

		protected void recreateContent()
		{
			removeAll();
			nameFields.clear();
			currentButtons.clear();
			fusedBoxes.clear();
			assignBoxes.clear();

			numSources = visibility.numSources();
			numGroups = visibility.numGroups();

			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			final List< SourceGroup > groups = visibility.getSourceGroups();

			// source shortcuts
			// TODO: shortcut "names" should not be hard-coded here!
			c.gridx = 0;
			c.gridy = 0;
			add( new JLabel( "shortcut" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			final int nShortcuts = Math.min( numGroups, 10 );
			for ( int i = 0; i < nShortcuts; ++i )
				add( new JLabel( Integer.toString( i == 10 ? 0 : i + 1 ) ), c );

			// source names
			c.gridx = 1;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "group name" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int g = 0; g < numGroups; ++g )
			{
				final JTextField tf = new JTextField( groups.get( g ).getName(), 10 );
				final int groupIndex = g;
				tf.getDocument().addDocumentListener( new DocumentListener()
				{
					private void doit()
					{
						visibility.setGroupName( groupIndex, tf.getText() );
					}

					@Override
					public void removeUpdate( final DocumentEvent e )
					{
						doit();
					}

					@Override
					public void insertUpdate( final DocumentEvent e )
					{
						doit();
					}

					@Override
					public void changedUpdate( final DocumentEvent e )
					{
						doit();
					}
				} );
				nameFields.add( tf );
				add( tf, c );
			}

			// "current" radio-buttons
			c.anchor = GridBagConstraints.CENTER;
			c.gridx = 2;
			c.gridy = 0;
			add( new JLabel( "current" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			final ButtonGroup currentButtonGroup = new ButtonGroup();
			for ( int g = 0; g < numGroups; ++g )
			{
				final JRadioButton b = new JRadioButton();
				final int groupIndex = g;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						if ( b.isSelected() )
							visibility.setCurrentGroup( groupIndex );
					}
				} );
				currentButtons.add( b );
				currentButtonGroup.add( b );
				add( b, c );
			}

			// "active in fused" check-boxes
			c.gridx = 3;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "active in fused" ), c );
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int g = 0; g < numGroups; ++g )
			{
				final JCheckBox b = new JCheckBox();
				final int groupIndex = g;
				b.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						visibility.setGroupActive( groupIndex, b.isSelected() );
					}
				} );
				fusedBoxes.add( b );
				add( b, c );
			}

			// setup-to-group assignments
			c.gridx = 4;
			c.gridy = 0;
			c.gridwidth = numSources;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "assigned sources" ), c );
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_END;
			for ( int s = 0; s < numSources; ++s )
			{
				final int sourceIndex = s;
				c.gridx = sourceIndex + 4;
				for ( int g = 0; g < numGroups; ++g )
				{
					final int groupIndex = g;
					c.gridy = g + 1;
					final JCheckBox b = new JCheckBox();
					b.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							if ( b.isSelected() )
								visibility.addSourceToGroup( sourceIndex, groupIndex );
							else
								visibility.removeSourceFromGroup( sourceIndex, groupIndex );
						}
					} );
					assignBoxes.add( b );
					add( b, c );
				}
			}

			invalidate();
			final Window frame = SwingUtilities.getWindowAncestor( this );
			if ( frame != null )
				frame.pack();
		}

		protected void update()
		{
			synchronized ( visibility )
			{
				if ( visibility.numSources() != numSources && visibility.numGroups() != numGroups )
					recreateContent();

				currentButtons.get( visibility.getCurrentGroup() ).setSelected( true );
				for ( int g = 0; g < numGroups; ++g )
					fusedBoxes.get( g ).setSelected( visibility.isGroupActive( g ) );

				updateGroupNames();
				updateGroupAssignments();
			}
		}

		protected void updateGroupNames()
		{
			final List< SourceGroup > groups = visibility.getSourceGroups();
			for ( int i = 0; i < numGroups; ++i )
			{
				final JTextField tf = nameFields.get( i );
				final String name = groups.get( i ).getName();
				if ( ! tf.getText().equals( name ) )
					tf.setText( name );
			}
		}

		protected void updateGroupAssignments()
		{
			final List< SourceGroup > groups = visibility.getSourceGroups();
			for ( int s = 0; s < numSources; ++s )
				for ( int g = 0; g < numGroups; ++g )
					assignBoxes.get( s * numGroups + g ).setSelected( groups.get( g ).getSourceIds().contains( s ) );
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			synchronized ( visibility )
			{
				if ( visibility.numSources() != numSources || visibility.numGroups() != numGroups )
					recreateContent();

				switch ( e.id )
				{
				case CURRENT_GROUP_CHANGED:
					currentButtons.get( visibility.getCurrentGroup() ).setSelected( true );
					break;
				case GROUP_ACTIVITY_CHANGED:
					for ( int g = 0; g < numGroups; ++g )
						fusedBoxes.get( g ).setSelected( visibility.isGroupActive( g ) );
					break;
				case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
					updateGroupAssignments();
					break;
				case GROUP_NAME_CHANGED:
					updateGroupNames();
					break;
				}
			}
		}
	}

}
