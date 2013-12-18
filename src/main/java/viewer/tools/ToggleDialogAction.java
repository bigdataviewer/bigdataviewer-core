package viewer.tools;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import viewer.util.AbstractNamedAction;

public class ToggleDialogAction extends AbstractNamedAction
{
	protected final Dialog dialog;

	public ToggleDialogAction( final String name, final Dialog dialog )
	{
		super( name );
		this.dialog = dialog;
	}

	@Override
	public void actionPerformed( final ActionEvent arg0 )
	{
		dialog.setVisible( ! dialog.isVisible() );
	}

	private static final long serialVersionUID = 1L;
}
