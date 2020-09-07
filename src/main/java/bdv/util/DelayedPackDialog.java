package bdv.util;

import javax.swing.*;
import java.awt.*;

/**
 * A {@code JDialog} that delays {@code pack()} calls until the dialog is made visible.
 */
public class DelayedPackDialog extends JDialog
{
	private volatile boolean packIsPending = false;

	public DelayedPackDialog( Frame owner, String title, boolean modal )
	{
		super( owner, title, modal );
	}

	@Override
	public void pack()
	{
		if ( isVisible() )
		{
			packIsPending = false;
			super.pack();
		}
		else
			packIsPending = true;
	}

	@Override
	public void setVisible( boolean visible )
	{
		if ( visible && packIsPending )
		{
			packIsPending = false;
			super.pack();
		}
		super.setVisible( visible );
	}
}
