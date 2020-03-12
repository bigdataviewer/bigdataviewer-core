package bdv.ui.iconcards.options;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * A ToggleGroupButton holds a group of states, through which the user can toggle by left-clicking the button.
 * If the user right-clicks on the button a popup menu will appear and the user can select one of the options
 * directly.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG, CSBD, Dresden, Germany
 * @author Deborah Schmidt, MPI-CBG, CSBD, Dresden, Germany
 */
public class ToggleGroupButton extends JButton
{
	private final List< Icon > toggleIcons;

	private final List< Runnable > toggleActions;

	private final JPopupMenu menu;

	private final List< String > toggleLabels;

	private int current = 0;

	/**
	 * Create a new ToggleGroupButton.
	 *
	 * @param toggleIcons displayed on the button and in the popup menu
	 * @param toggleLabels displayed in the popup menu and tooltip
	 * @param toggleActions executed if the button switches to this action
	 */
	public ToggleGroupButton( final List< Icon > toggleIcons, final List< String > toggleLabels, final List< Runnable > toggleActions )
	{
		this.toggleIcons = toggleIcons;
		this.toggleLabels = toggleLabels;
		this.toggleActions = toggleActions;
		this.menu = new JPopupMenu();
		populatePopupMenu();

		this.setContentAreaFilled( false );
		this.setFocusPainted( false );
		this.setBorderPainted( false );

		this.setIcon( toggleIcons.get( current ) );
		this.setToolTipText( toggleLabels.get( current ) );
		addActionListener( a -> {
			next();
		} );
	}

	private void populatePopupMenu()
	{
		menu.setInvoker( this );

		for ( int i = 0; i < toggleActions.size(); i++ )
		{
			createMenuItem( i );
		}

		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent mouseEvent )
			{
				if ( SwingUtilities.isRightMouseButton( mouseEvent ) )
					menu.show( menu.getInvoker(), mouseEvent.getX(), mouseEvent.getY() );
			}
		} );
	}

	private void createMenuItem( final int i )
	{
		final JMenuItem item = new JMenuItem( toggleLabels.get( i ), toggleIcons.get( i ) );
		final int index = i;
		item.addActionListener( a -> {
			current = index;
			trigger();
		} );
		menu.add( item );
	}

	/**
	 * Toggle to the next action in the list.
	 */
	public void next()
	{
		current = ( current + 1 ) % toggleActions.size();
		trigger();
	}

	/**
	 * Select a specific option.
	 *
	 * @param index modulo number-of-options is set.
	 */
	public void setCurrent( final int index )
	{
		this.current = index % toggleActions.size();
		trigger();
	}

	/**
	 * Select an option via name.
	 *
	 * @param label of the option
	 */
	public void setCurrent( final String label )
	{
		if ( toggleLabels.contains( label ) )
		{
			current = toggleLabels.indexOf( label );
			trigger();
		}
	}

	/**
	 * Add a new option.
	 *
	 * @param icon of the option
	 * @param label of the option
	 * @param action triggered by this option
	 */
	public void addOption( final Icon icon, final String label, final Runnable action )
	{
		toggleIcons.add( icon );
		toggleLabels.add( label );
		toggleActions.add( action );
		createMenuItem( toggleIcons.size() - 1 );
	}

	/**
	 * Removes the option.
	 *
	 * @param label of the option
	 */
	public void removeOption( final String label )
	{
		if ( toggleLabels.contains( label ) )
		{
			removeOption( toggleLabels.indexOf( label ) );
		}
	}

	/**
	 * Remove option via index.
	 * 
	 * @param index
	 */
	public void removeOption( final int index )
	{
		if ( index < toggleLabels.size() )
		{
			toggleIcons.remove( index );
			toggleLabels.remove( index );
			toggleActions.remove( index );
			menu.remove( index );
		}
	}

	private void trigger()
	{
		this.setIcon( toggleIcons.get( current ) );
		this.setToolTipText( toggleLabels.get( current ) );
		toggleActions.get( current ).run();
	}
}
