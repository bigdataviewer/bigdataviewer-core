package bdv.ui.splitpanel;

import bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType;
import bdv.viewer.ViewerPanel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_COLLAPSE;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_EXPAND;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_COLLAPSE;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_EXPAND;

class SplitPaneOneTouchExpandTrigger extends MouseAdapter
{
	private final SplitPaneOneTouchExpandAnimator animator;

	private final SplitPanel splitPanel;

	private final ViewerPanel viewer;

	public SplitPaneOneTouchExpandTrigger( final SplitPaneOneTouchExpandAnimator animator, final SplitPanel splitPanel, final ViewerPanel viewer )
	{
		this.animator = animator;
		this.splitPanel = splitPanel;
		this.viewer = viewer;
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		final int x = e.getX();
		final int y = e.getY();

		// check whether in border region
		if ( animator.isInBorderRegion( x, y ) )
			checkEnterBorderRegion();
		else
			checkExitBorderRegion();

		// check whether in trigger region
		if ( animator.isInTriggerRegion( x, y ) )
			checkEnterTriggerRegion();
		else
			checkExitTriggerRegion();
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		checkExitBorderRegion();
		checkExitTriggerRegion();
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		if ( animator.isInTriggerRegion( e.getX(), e.getY() ) )
		{
			splitPanel.setCollapsed( !splitPanel.isCollapsed() );
			viewer.requestRepaint();
			checkExitBorderRegion();
			checkExitTriggerRegion();
		}
	}

	private boolean inBorderRegion = false;
	private boolean inTriggerRegion = false;

	private void checkExitTriggerRegion()
	{
		if ( inTriggerRegion )
		{
			inTriggerRegion = false;
			if ( !splitPanel.isCollapsed() )
				startAnimation( HIDE_COLLAPSE );
		}
	}

	private void checkEnterTriggerRegion()
	{
		if ( !inTriggerRegion )
		{
			inTriggerRegion = true;
			if ( !splitPanel.isCollapsed() )
				startAnimation( SHOW_COLLAPSE );
		}
	}

	private void checkExitBorderRegion()
	{
		if ( inBorderRegion )
		{
			inBorderRegion = false;
			if ( splitPanel.isCollapsed() )
				startAnimation( HIDE_EXPAND );
		}
	}

	private void checkEnterBorderRegion()
	{
		if ( !inBorderRegion )
		{
			inBorderRegion = true;
			if ( splitPanel.isCollapsed() )
				startAnimation( SHOW_EXPAND );
		}
	}

	private void startAnimation( final AnimationType animationType )
	{
		animator.startAnimation( animationType );
		viewer.getDisplay().repaint();
	}
}
