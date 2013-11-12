package viewer.gui.visibility;

import static viewer.VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;
import static viewer.VisibilityAndGrouping.Event.GROUP_NAME_CHANGED;
import static viewer.VisibilityAndGrouping.Event.SOURCE_ACTVITY_CHANGED;
import static viewer.VisibilityAndGrouping.Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED;
import static viewer.VisibilityAndGrouping.Event.VISIBILITY_CHANGED;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import viewer.VisibilityAndGrouping;
import viewer.VisibilityAndGrouping.Event;
import viewer.render.SourceGroup;

public class ActiveSourcesDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	private final VisibilityPanel visibilityPanel;

	private final GroupingPanel groupingPanel;

	public ActiveSourcesDialog( final Frame owner, final VisibilityAndGrouping visibilityAndGrouping )
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
		getContentPane().add( visibilityPanel, BorderLayout.NORTH );

		groupingPanel = new GroupingPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( groupingPanel );
		groupingPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"grouping" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		getContentPane().add( groupingPanel, BorderLayout.SOUTH );

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
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	public void update()
	{
		visibilityPanel.update();
		groupingPanel.update();
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
					update();
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

		private final ArrayList< JCheckBox > assignBoxes;

		private JCheckBox groupingBox;

		private int numSources;

		private int numGroups;

		public GroupingPanel( final VisibilityAndGrouping visibilityAndGrouping )
		{
			super( new GridBagLayout() );
			this.visibility = visibilityAndGrouping;
			nameFields = new ArrayList< JTextField >();
			assignBoxes = new ArrayList< JCheckBox >();
			numSources = visibilityAndGrouping.numSources();
			numGroups = visibilityAndGrouping.numGroups();
			recreateContent();
		}

		protected void recreateContent()
		{
			removeAll();
			nameFields.clear();
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

			// setup-to-group assignments
			c.gridx = 2;
			c.gridy = 0;
			c.gridwidth = numSources;
			c.anchor = GridBagConstraints.CENTER;
			add( new JLabel( "assigned sources" ), c );
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_END;
			for ( int s = 0; s < numSources; ++s )
			{
				final int sourceIndex = s;
				c.gridx = sourceIndex + 2;
				for ( int g = 0; g < numGroups; ++g )
				{
					final int groupIndex = g;
					c.gridy = g + 1;
					final JCheckBox b = new JCheckBox();
					b.setSelected( groups.get( g ).getSourceIds().contains( s ) );
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

			final JPanel panel = new JPanel();
			panel.setLayout(  new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
			groupingBox = new JCheckBox();
			groupingBox.setSelected( visibility.isGroupingEnabled() );
			groupingBox.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					visibility.setGroupingEnabled( groupingBox.isSelected() );
				}
			} );
			panel.add( groupingBox );
			panel.add( new JLabel("enable grouping") );

			c.gridx = 0;
			c.gridy = numGroups + 1;
			c.gridwidth = 2 + numSources;
			c.anchor = GridBagConstraints.CENTER;
			add( panel, c );

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

				groupingBox.setSelected( visibility.isGroupingEnabled() );
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
				case DISPLAY_MODE_CHANGED:
					groupingBox.setSelected( visibility.isGroupingEnabled() );
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
