package bdv.util;

import javax.swing.*;
import java.awt.*;

public class MemoryFixedDialog extends JDialog
{
	private boolean packIsPending = false;

	public MemoryFixedDialog()
	{
		super();
		super.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
	}

	public MemoryFixedDialog( Frame owner, String title, boolean modal )
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

	@Override
	public void setDefaultCloseOperation( int operation )
	{
		// do nothing
	}
}
