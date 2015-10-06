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
package bdv.viewer.animate;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Draw one line of text in the center or bottom right of the display. Text is
 * fading in and out.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class TextOverlayAnimator extends AbstractAnimator implements OverlayAnimator
{
	protected final Font font;

	protected final String text;

	protected final double fadeInTime;

	protected final double fadeOutTime;

	public static enum TextPosition
	{
		CENTER,
		BOTTOM_RIGHT
	}

	protected final TextPosition position;

	public TextOverlayAnimator( final String text, final long duration )
	{
		this( text, duration, TextPosition.BOTTOM_RIGHT );
	}

	public TextOverlayAnimator( final String text, final long duration, final TextPosition position )
	{
		this( text, duration, position, 0.2, 0.5 );
	}

	public TextOverlayAnimator( final String text, final long duration, final TextPosition position, final double fadeInTime, final double fadeOutTime )
	{
		this( text, duration, position, fadeInTime, fadeOutTime, new Font( "SansSerif", Font.BOLD, 20 ) );
	}

	public TextOverlayAnimator( final String text, final long duration, final TextPosition position, final double fadeInTime, final double fadeOutTime, final Font font )
	{
		super( duration );
		this.text = text;
		this.font = font;
		this.fadeInTime = fadeInTime;
		this.fadeOutTime = fadeOutTime;
		this.position = position;
	}

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		setTime( time );

		final FontRenderContext frc = g.getFontRenderContext();
		final TextLayout layout = new TextLayout( text, font, frc );
		final Rectangle2D bounds = layout.getBounds();
		final float x, y;
		if ( position == TextPosition.BOTTOM_RIGHT )
		{
			x = ( float ) ( g.getClipBounds().getWidth() - bounds.getWidth() - 10 );
			y = ( float ) ( g.getClipBounds().getHeight() - 10 );
		}
		else // if ( position == TextPosition.CENTER )
		{
			x = ( float ) ( g.getClipBounds().getWidth() - bounds.getWidth() ) / 2;
			y = ( float ) ( g.getClipBounds().getHeight() - bounds.getHeight() ) / 2;
		}

		final double t = ratioComplete();
		final float alpha;
		if ( t <= fadeInTime )
			alpha = ( float ) Math.sin( ( Math.PI / 2 ) * t / fadeInTime );
		else if ( t >= 1.0 - fadeOutTime )
			alpha = ( float ) Math.sin( ( Math.PI / 2 ) * ( 1.0 - t ) / ( fadeOutTime ) );
		else
			alpha = 1;

		g.setColor( new Color( 1f, 1f, 1f, alpha ) );
		layout.draw( g, x, y );
	}

	@Override
	public boolean requiresRepaint()
	{
		return !isComplete();
	}
}
