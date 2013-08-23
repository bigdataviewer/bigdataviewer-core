package viewer.gui.visibility;

import static viewer.render.DisplayMode.FUSED;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;

import viewer.VisibilityAndGrouping;
import viewer.VisibilityAndGrouping.Event;

public class ActiveSourcesDialog extends JDialog
{
	private final VisibilityAndGrouping visibilityAndGrouping;

	public ActiveSourcesDialog( final Frame owner, final VisibilityAndGrouping visibilityAndGrouping )
	{
		super( owner, "visibility and grouping", false );

		this.visibilityAndGrouping = visibilityAndGrouping;

		final VisibilityPanel visibilityPanel = new VisibilityPanel( visibilityAndGrouping );
		visibilityAndGrouping.addUpdateListener( visibilityPanel );
		getContentPane().add( visibilityPanel, BorderLayout.NORTH );

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
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	public static class VisibilityPanel extends JPanel implements VisibilityAndGrouping.UpdateListener
	{
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

			final int numSources = visibilityAndGrouping.numSources();
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets( 0, 5, 0, 5 );

			// source names
			c.gridx = 0;
			c.gridy = 0;
			add( new JLabel( "source" ), c );
			c.anchor = GridBagConstraints.LINE_END;
			c.gridy = GridBagConstraints.RELATIVE;
			for ( int i = 0; i < numSources; ++i )
				add( new JLabel( visibilityAndGrouping.getSources().get( i ).getSpimSource().getName() ), c );

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
						visibility.setActive( sourceIndex, FUSED, b.isSelected() );
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

			update();
		}

		protected void update()
		{
			synchronized ( visibility )
			{
				final int n = visibility.numSources();
				currentButtons.get( visibility.getCurrentSource() ).setSelected( true );
				for ( int i = 0; i < n; ++i )
				{
					fusedBoxes.get( i ).setSelected( visibility.isActive( i, FUSED ) );
					visibleBoxes.get( i ).setSelected( visibility.isVisible( i ) );
				}
			}
		}

		@Override
		public void visibilityChanged( final Event e )
		{
			switch ( e.id )
			{
			case Event.ACTIVATE:
				if ( e.displayMode == FUSED )
					fusedBoxes.get( e.sourceIndex ).setSelected( true );
				if ( e.displayMode == visibility.getDisplayMode() )
					visibleBoxes.get( e.sourceIndex ).setSelected( true );
				break;
			case Event.DEACTIVATE:
				if ( e.displayMode == FUSED )
					fusedBoxes.get( e.sourceIndex ).setSelected( false );
				if ( e.displayMode == visibility.getDisplayMode() )
					visibleBoxes.get( e.sourceIndex ).setSelected( false );
				break;
			case Event.MAKE_CURRENT:
				currentButtons.get( e.sourceIndex ).setSelected( true );
				break;
			case Event.DISPLAY_MODE_CHANGED:
				update();
			default:
			}
		}
	}
}
