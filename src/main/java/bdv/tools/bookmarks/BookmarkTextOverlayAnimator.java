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
 * @author Tobias Pietzsch
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
