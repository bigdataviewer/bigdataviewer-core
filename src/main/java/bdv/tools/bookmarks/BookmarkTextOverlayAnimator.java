/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.tools.bookmarks;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.AbstractAnimator;
import bdv.viewer.animate.OverlayAnimator;

/**
 * Draw one line of text in the center or bottom right of the display. Text is
 * fading in and out.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BookmarkTextOverlayAnimator implements OverlayAnimator
{
	private final Font font;

	private String text;

	private AbstractAnimator animator;

	private boolean fadeIn;

	private boolean complete = false;

	private final ViewerPanel viewer;

	public BookmarkTextOverlayAnimator( final ViewerPanel viewer )
	{
		this( viewer, new Font( "SansSerif", Font.BOLD, 20 ) );
	}

	public BookmarkTextOverlayAnimator( final ViewerPanel viewer, final Font font )
	{
		this.viewer = viewer;
		this.font = font;
	}

	public final void fadeIn( final String message, final long fadeInDuration )
	{
		text = message;
		animator = ( fadeInDuration > 0 ) ? new AbstractAnimator( fadeInDuration ) : null;
		fadeIn = true;
		viewer.getDisplay().repaint();
	}

	public final void fadeOut( final String message, final long fadeOutDuration )
	{
		text = message;
		animator = new AbstractAnimator( fadeOutDuration );
		fadeIn = false;
		viewer.getDisplay().repaint();
	}

	public final void clear()
	{
		text = null;
		animator = null;
		complete = true;
		viewer.getDisplay().repaint();
	}

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		if ( text == null )
			return;

		final FontRenderContext frc = g.getFontRenderContext();
		final TextLayout layout = new TextLayout( text, font, frc );
		final Rectangle2D bounds = layout.getBounds();
		final float x, y;
		x = ( float ) ( g.getClipBounds().getWidth() - bounds.getWidth() - 10 );
		y = ( float ) ( g.getClipBounds().getHeight() - 10 );

		final float alpha;
		if ( animator != null )
		{
			animator.setTime( time );
			final double t = animator.ratioComplete();
			if ( fadeIn )
				alpha = ( float ) Math.sin( ( Math.PI / 2 ) * t );
			else
				alpha = ( float ) Math.sin( ( Math.PI / 2 ) * ( 1.0 - t ) );

			if ( animator.isComplete() )
			{
				animator = null;
				if ( ! fadeIn )
					complete = true;
			}
		}
		else
			alpha = 1;

		g.setColor( new Color( 1f, 1f, 1f, alpha ) );
		layout.draw( g, x, y );
	}

	@Override
	public boolean isComplete()
	{
		return complete;
	}

	@Override
	public boolean requiresRepaint()
	{
		return ! ( animator == null || text == null );
	}
}
