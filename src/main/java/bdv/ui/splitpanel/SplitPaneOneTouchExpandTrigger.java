/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
