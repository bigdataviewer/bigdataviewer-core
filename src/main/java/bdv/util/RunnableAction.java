package bdv.util;

import java.awt.event.ActionEvent;

public class RunnableAction extends AbstractNamedAction
{
	private final Runnable action;

	public RunnableAction( final String name, final Runnable action )
	{
		super( name );
		this.action = action;
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		action.run();
	}

	private static final long serialVersionUID = 1L;
}
