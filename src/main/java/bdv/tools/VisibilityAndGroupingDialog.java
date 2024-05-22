/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.SourceGroup;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@Deprecated
public class VisibilityAndGroupingDialog extends DelayedPackDialog
{
	private static final long serialVersionUID = 1L;

	private final VisibilityPanel visibilityPanel;

	private final GroupingPanel groupingPanel;

	private final ModePanel modePanel;

	public VisibilityAndGroupingDialog( final Frame owner, final VisibilityAndGrouping visibilityAndGrouping )
	{
		this( owner, visibilityAndGrouping.getState() );
	}

	public VisibilityAndGroupingDialog( final Frame owner, final ViewerState state )
	{
		super( owner, "visibility and grouping", false );

		visibilityPanel = new VisibilityPanel( state, this::isVisible );
		visibilityPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"visibility" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		groupingPanel = new GroupingPanel( state, this::isVisible );
		groupingPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"grouping" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		modePanel = new ModePanel( state, this::isVisible );

		final JPanel content = new JPanel();
		content.setLayout( new BoxLayout( content, BoxLayout.PAGE_AXIS ) );
		content.add( visibilityPanel );
		content.add( groupingPanel );
		content.add( modePanel );
		add( content, BorderLayout.NORTH );

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

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				visibilityPanel.shown();
				groupingPanel.shown();
				modePanel.shown();
			}
		} );

		pack();
	}

	public void update()
	{
		visibilityPanel.update();
		groupingPanel.update();
		modePanel.update();
	}

	public static class VisibilityPanel extends JPanel implements ViewerStateChangeListener
	{
		private static final long serialVersionUID = 1L;

		private final ViewerState state;

		private final Map< SourceAndConverter< ? >, JRadioButton > currentButtonsMap = new HashMap<>();

		private final ArrayList< Consumer< Set< SourceAndConverter< ? > > > > updateActiveBoxes = new ArrayList<>();

		private final ArrayList< Consumer< Set< SourceAndConverter< ? > > > > updateVisibleBoxes = new ArrayList<>();

		private final BooleanSupplier isVisible;

		public VisibilityPanel( final ViewerState state, BooleanSupplier isVisible )
		{
			super( new GridBagLayout() );
			this.state = state;
			this.isVisible = isVisible;
			state.changeListeners().add( this );
		}

		private void shown()
		{
			if ( recreateContentPending.getAndSet( false ) )
				recreateContentNow();
			if ( updatePending.getAndSet( false ) )
				updateNow();
		}

		private final AtomicBoolean recreateContentPending = new AtomicBoolean( true );

		private void recreateContent()
		{
			if ( isVisible.getAsBoolean() )
			{
				recreateContentNow();
				recreateContentPending.set( false );
			}
			else
				recreateContentPending.set( true );
		}

		private void recreateContentNow()
		{
			synchronized ( state )
			{
				removeAll();
				currentButtonsMap.clear();
				updateActiveBoxes.clear();
				updateVisibleBoxes.clear();

				final List< SourceAndConverter< ? > > sources = state.getSources();

				final GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets( 0, 5, 0, 5 );

				// source names
				c.gridx = 0;
				c.gridy = 0;
				add( new JLabel( "source" ), c );
				c.anchor = GridBagConstraints.LINE_END;
				c.gridy = GridBagConstraints.RELATIVE;
				for ( final SourceAndConverter< ? > source : sources )
					add( new JLabel( source.getSpimSource().getName() ), c );

				// "current" radio-buttons
				c.anchor = GridBagConstraints.CENTER;
				c.gridx = 1;
				c.gridy = 0;
				add( new JLabel( "current" ), c );
				c.gridy = GridBagConstraints.RELATIVE;
				final ButtonGroup currentButtonGroup = new ButtonGroup();
				for ( final SourceAndConverter< ? > source : sources )
				{
					final JRadioButton b = new JRadioButton();
					b.addActionListener( e -> {
						if ( b.isSelected() )
							state.setCurrentSource( source );
					} );
					currentButtonsMap.put( source, b );
					currentButtonGroup.add( b );
					add( b, c );
				}

				// "active in fused" check-boxes
				c.gridx = 2;
				c.gridy = 0;
				add( new JLabel( "active in fused" ), c );
				c.gridy = GridBagConstraints.RELATIVE;
				for ( final SourceAndConverter< ? > source : sources )
				{
					final JCheckBox b = new JCheckBox();
					b.addActionListener( e -> state.setSourceActive( source, b.isSelected() ) );
					updateActiveBoxes.add( active -> b.setSelected( active.contains( source ) ) );
					add( b, c );
				}

				// "currently visible" check-boxes
				c.gridx = 3;
				c.gridy = 0;
				add( new JLabel( "visible" ), c );
				c.gridy = GridBagConstraints.RELATIVE;
				for ( final SourceAndConverter< ? > source : sources )
				{
					final JCheckBox b = new JCheckBox();
					updateVisibleBoxes.add( visible -> b.setSelected( visible.contains( source ) ) );
					b.setEnabled( false );
					add( b, c );
				}

				invalidate();
				final Window frame = SwingUtilities.getWindowAncestor( this );
				if ( frame != null )
					frame.pack();

				update();
			}
		}

		private final AtomicBoolean updatePending = new AtomicBoolean( true );

		private void update()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateNow();
				updatePending.set( false );
			}
			else
				updatePending.set( true );
		}

		private void updateNow()
		{
			final SourceAndConverter< ? > currentSource = state.getCurrentSource();
			if ( currentSource == null )
				currentButtonsMap.values().forEach( b -> b.setSelected( false ) );
			else
				currentButtonsMap.get( currentSource ).setSelected( true );

			final Set< SourceAndConverter< ? > > activeSources = state.getActiveSources();
			updateActiveBoxes.forEach( c -> c.accept( activeSources ) );

			final Set< SourceAndConverter< ? > > visibleSources = state.getVisibleSources();
			updateVisibleBoxes.forEach( c -> c.accept( visibleSources ) );
		}

		@Override
		public void viewerStateChanged( final ViewerStateChange change )
		{
			final AtomicBoolean pendingUpdate = new AtomicBoolean();
			final AtomicBoolean pendingRecreate = new AtomicBoolean();

			switch ( change )
			{
			case CURRENT_SOURCE_CHANGED:
			case SOURCE_ACTIVITY_CHANGED:
			case VISIBILITY_CHANGED:
				SwingUtilities.invokeLater( this::update );
				break;
			case NUM_SOURCES_CHANGED:
				SwingUtilities.invokeLater( this::recreateContent );
				break;
			}
		}
	}

	public static class ModePanel extends JPanel implements ViewerStateChangeListener
	{
		private static final long serialVersionUID = 1L;

		private final ViewerState state;

		private JCheckBox groupingBox;

		private JCheckBox fusedModeBox;

		private final BooleanSupplier isVisible;

		public ModePanel( final ViewerState state, BooleanSupplier isVisible )
		{
			super( new GridBagLayout() );
			this.state = state;
			this.isVisible = isVisible;
			state.changeListeners().add( this );
			synchronized ( state )
			{
				recreateContent();
				update();
			}
		}

		private void shown()
		{
			if ( updatePending.getAndSet( false ) )
				updateNow();
		}

		private void recreateContent()
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_START;
			groupingBox = new JCheckBox();
			groupingBox.addActionListener( e -> {
				synchronized ( state )
				{
					state.setDisplayMode( state.getDisplayMode().withGrouping( groupingBox.isSelected() ) );
				}
			} );
			c.gridx = 0;
			c.gridy = 0;
			add( groupingBox, c );
			c.gridx = 1;
			add( new JLabel("enable grouping"), c );

			fusedModeBox = new JCheckBox();
			fusedModeBox.addActionListener( e -> {
				synchronized ( state )
				{
					state.setDisplayMode( state.getDisplayMode().withFused( fusedModeBox.isSelected() ) );
				}
			} );
			c.gridx = 0;
			c.gridy = 1;
			add( fusedModeBox, c );
			c.gridx = 1;
			add( new JLabel("enable fused mode"), c );
		}

		private final AtomicBoolean updatePending = new AtomicBoolean( true );

		private void update()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateNow();
				updatePending.set( false );
			}
			else
				updatePending.set( true );
		}

		private void updateNow()
		{
			groupingBox.setSelected( state.getDisplayMode().hasGrouping() );
			fusedModeBox.setSelected( state.getDisplayMode().hasFused() );
		}

		@Override
		public void viewerStateChanged( final ViewerStateChange change )
		{
			if ( change == ViewerStateChange.DISPLAY_MODE_CHANGED )
				SwingUtilities.invokeLater( this::update );
		}
	}

	public static class GroupingPanel extends JPanel implements ViewerStateChangeListener
	{
		private static final long serialVersionUID = 1L;

		private final ArrayList< Runnable > updateNames = new ArrayList<>();

		private final Map< SourceGroup, JRadioButton > currentButtonsMap = new HashMap<>();

		private final ArrayList< Consumer< Set< SourceGroup > > > updateActiveBoxes = new ArrayList<>();

		private final ArrayList< Runnable > updateAssignBoxes = new ArrayList<>();

		private final ViewerState state;

		private final BooleanSupplier isVisible;

		public GroupingPanel( final ViewerState state, BooleanSupplier isVisible )
		{
			super( new GridBagLayout() );
			this.state = state;
			this.isVisible = isVisible;
			state.changeListeners().add( this );
		}

		private void shown()
		{
			if ( recreateContentPending.getAndSet( false ) )
				recreateContentNow();
			if ( updateCurrentGroupPending.getAndSet( false ) )
				updateCurrentGroupNow();
			if ( updateGroupNamesPending.getAndSet( false ) )
				updateGroupNamesNow();
			if ( updateGroupActivityPending.getAndSet( false ) )
				updateGroupActivityNow();
			if ( updateGroupAssignmentsPending.getAndSet( false ) )
				updateGroupAssignmentsNow();
		}

		private final AtomicBoolean recreateContentPending = new AtomicBoolean( true );

		private void recreateContent()
		{
			if ( isVisible.getAsBoolean() )
			{
				recreateContentNow();
				recreateContentPending.set( false );
			}
			else
				recreateContentPending.set( true );
		}

		private void recreateContentNow()
		{
			synchronized ( state )
			{
				removeAll();
				updateNames.clear();
				currentButtonsMap.clear();
				updateActiveBoxes.clear();
				updateAssignBoxes.clear();

				final GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets( 0, 5, 0, 5 );

				final List< SourceAndConverter< ? > > sources = state.getSources();
				final List< SourceGroup > groups = state.getGroups();

				// source shortcuts
				// TODO: shortcut "names" should not be hard-coded here!
				c.gridx = 0;
				c.gridy = 0;
				add( new JLabel( "shortcut" ), c );
				c.anchor = GridBagConstraints.LINE_END;
				c.gridy = GridBagConstraints.RELATIVE;
				final int nShortcuts = Math.min( groups.size(), 10 );
				for ( int i = 0; i < nShortcuts; ++i )
					add( new JLabel( Integer.toString( i == 10 ? 0 : i + 1 ) ), c );

				// source names
				c.gridx = 1;
				c.gridy = 0;
				c.anchor = GridBagConstraints.CENTER;
				add( new JLabel( "group name" ), c );
				c.anchor = GridBagConstraints.LINE_END;
				c.gridy = GridBagConstraints.RELATIVE;
				for ( final SourceGroup group : groups )
				{
					final JTextField tf = new JTextField( state.getGroupName( group ), 10 );
					tf.getDocument().addDocumentListener( new DocumentListener()
					{
						private void doit()
						{
							state.setGroupName( group, tf.getText() );
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
					updateNames.add( () -> {
						final String name = state.getGroupName( group );
						if ( !tf.getText().equals( name ) )
						{
							tf.setText( name );
						}
					} );
					add( tf, c );
				}

				// "current" radio-buttons
				c.anchor = GridBagConstraints.CENTER;
				c.gridx = 2;
				c.gridy = 0;
				add( new JLabel( "current" ), c );
				c.gridy = GridBagConstraints.RELATIVE;
				final ButtonGroup currentButtonGroup = new ButtonGroup();
				for ( final SourceGroup group : groups )
				{
					final JRadioButton b = new JRadioButton();
					b.addActionListener( e -> {
						if ( b.isSelected() )
							state.setCurrentGroup( group );
					} );
					currentButtonsMap.put( group, b );
					currentButtonGroup.add( b );
					add( b, c );
				}

				// "active in fused" check-boxes
				c.gridx = 3;
				c.gridy = 0;
				c.anchor = GridBagConstraints.CENTER;
				add( new JLabel( "active in fused" ), c );
				c.gridy = GridBagConstraints.RELATIVE;
				for ( final SourceGroup group : groups )
				{
					final JCheckBox b = new JCheckBox();
					b.addActionListener( e -> state.setGroupActive( group, b.isSelected() ) );
					updateActiveBoxes.add( active -> b.setSelected( active.contains( group ) ) );
					add( b, c );
				}

				// setup-to-group assignments
				c.gridx = 4;
				c.gridy = 0;
				c.gridwidth = sources.size();
				c.anchor = GridBagConstraints.CENTER;
				add( new JLabel( "assigned sources" ), c );
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				for ( final SourceAndConverter< ? > source : sources )
				{
					c.gridy = 1;
					for ( final SourceGroup group : groups )
					{
						final JCheckBox b = new JCheckBox();
						b.addActionListener( e -> {
							if ( b.isSelected() )
								state.addSourceToGroup( source, group );
							else
								state.removeSourceFromGroup( source, group );
						} );
						updateAssignBoxes.add( () -> {
							b.setSelected( state.getSourcesInGroup( group ).contains( source ) );
						} );
						add( b, c );
						c.gridy++;
					}
					c.gridx++;
				}

				invalidate();
				final Window frame = SwingUtilities.getWindowAncestor( this );
				if ( frame != null )
					frame.pack();

				update();
			}
		}

		private void update()
		{
			updateGroupNames();
			updateCurrentGroup();
			updateGroupActivity();
			updateGroupAssignments();
		}

		private final AtomicBoolean updateGroupNamesPending = new AtomicBoolean( true );

		private void updateGroupNames()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateGroupNamesNow();
				updateGroupNamesPending.set( false );
			}
			else
				updateGroupNamesPending.set( true );
		}

		private void updateGroupNamesNow()
		{
			updateNames.forEach( Runnable::run );
		}

		private final AtomicBoolean updateGroupAssignmentsPending = new AtomicBoolean( true );

		private void updateGroupAssignments()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateGroupAssignmentsNow();
				updateGroupAssignmentsPending.set( false );
			}
			else
				updateGroupAssignmentsPending.set( true );
		}

		private void updateGroupAssignmentsNow()
		{
			updateAssignBoxes.forEach( Runnable::run );
		}

		private final AtomicBoolean updateGroupActivityPending = new AtomicBoolean( true );

		private void updateGroupActivity()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateGroupActivityNow();
				updateGroupActivityPending.set( false );
			}
			else
				updateGroupActivityPending.set( true );
		}

		private void updateGroupActivityNow()
		{
			final Set< SourceGroup > activeGroups = state.getActiveGroups();
			updateActiveBoxes.forEach( c -> c.accept( activeGroups ) );
		}

		private final AtomicBoolean updateCurrentGroupPending = new AtomicBoolean( true );

		private void updateCurrentGroup()
		{
			if ( isVisible.getAsBoolean() )
			{
				updateCurrentGroupNow();
				updateCurrentGroupPending.set( false );
			}
			else
				updateCurrentGroupPending.set( true );
		}

		private void updateCurrentGroupNow()
		{
			final SourceGroup currentGroup = state.getCurrentGroup();
			if ( currentGroup == null )
				currentButtonsMap.values().forEach( b -> b.setSelected( false ) );
			else
				currentButtonsMap.get( currentGroup ).setSelected( true );
		}

		@Override
		public void viewerStateChanged( final ViewerStateChange change )
		{
			switch( change )
			{
			case CURRENT_GROUP_CHANGED:
				SwingUtilities.invokeLater( this::updateCurrentGroup );
				break;
			case GROUP_ACTIVITY_CHANGED:
				SwingUtilities.invokeLater( this::updateGroupActivity );
				break;
			case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
				SwingUtilities.invokeLater( this::updateGroupAssignments );
				break;
			case GROUP_NAME_CHANGED:
				SwingUtilities.invokeLater( this::updateGroupNames );
				break;
			case NUM_GROUPS_CHANGED:
			case NUM_SOURCES_CHANGED:
				SwingUtilities.invokeLater( this::recreateContent );
				break;
			}
		}
	}
}
