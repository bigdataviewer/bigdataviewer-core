package bdv.ui.iconcards.options;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class ToggleGroupButton extends JButton
{
	private final Icon[] toggleIcons;

	private final Runnable[] toggleActions;

	private final JPopupMenu menu;

	private final String[] labels;

	private int current = 0;

	public ToggleGroupButton( final Icon[] toggleIcons, final String[] labels, final Runnable[] toggleActions )
	{
		this.toggleIcons = toggleIcons;
		this.labels = labels;
		this.toggleActions = toggleActions;
		this.menu = new JPopupMenu();

		for ( int i = 0; i < this.toggleActions.length; i++ )
		{
			final JMenuItem item = new JMenuItem( this.labels[ i ], this.toggleIcons[ i ] );
			final int index = i;
			item.addActionListener( a -> {
				current = index;
				trigger();
			} );
			this.menu.add( item );
		}

		menu.setInvoker( this );
		this.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent mouseEvent )
			{
				if ( SwingUtilities.isRightMouseButton( mouseEvent ) )
					menu.show( menu.getInvoker(), mouseEvent.getX(), mouseEvent.getY() );
			}
		} );
		this.setContentAreaFilled( false );
		this.setFocusPainted( false );
		this.setBorderPainted( false );
		this.setIcon( toggleIcons[ current ] );
		this.setToolTipText( labels[ current ] );

		addActionListener( a -> {
			next();
		} );
	}

	public void next()
	{
		current = ( current + 1 ) % toggleActions.length;
		trigger();
	}

	private void trigger()
	{
		this.setIcon( toggleIcons[ current ] );
		this.setToolTipText( labels[ current ] );
		toggleActions[ current ].run();
	}
}
