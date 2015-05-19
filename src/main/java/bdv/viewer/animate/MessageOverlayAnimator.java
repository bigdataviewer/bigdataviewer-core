package bdv.viewer.animate;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Draw lines of text in the lower right corner of the display. Messages are
 * fading in a specified time. If several messages are drawn at the same time,
 * old messages scroll up.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class MessageOverlayAnimator implements OverlayAnimator
{
	protected static class TextAnimator extends AbstractAnimator
	{
		protected final String text;

		public TextAnimator( final String text, final long duration )
		{
			super( duration );
			this.text = text;
		}
	}

	protected final Font font;

	protected final long duration;

	protected final double fadeInTime;

	protected final double fadeOutTime;

	protected final List< TextAnimator > texts;

	public MessageOverlayAnimator( final long duration )
	{
		this( duration, 0.2, 0.5 );
	}

	public MessageOverlayAnimator( final long duration, final double fadeInTime, final double fadeOutTime )
	{
		this( duration, fadeInTime, fadeOutTime, new Font( "SansSerif", Font.BOLD, 20 ) );
	}

	public MessageOverlayAnimator( final long duration, final double fadeInTime, final double fadeOutTime, final Font font )
	{
		this.duration = duration;
		this.fadeInTime = fadeInTime;
		this.fadeOutTime = fadeOutTime;
		this.font = font;
		texts = Collections.synchronizedList( new LinkedList< TextAnimator >() );
	}

	public void add( final String text )
	{
		texts.add( 0, new TextAnimator( text, duration ) );
	}

	@Override
	public boolean isComplete()
	{
		// this animator should not be removed, ever.
		return false;
	}


	@Override
	public boolean requiresRepaint()
	{
		return !texts.isEmpty();
	}

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		if ( !requiresRepaint() )
			return;

		final double ox = g.getClipBounds().getWidth() - 10;
		double oy = g.getClipBounds().getHeight() - 10;
		final FontRenderContext frc = g.getFontRenderContext();
		for ( int i = 0; i < texts.size(); ++i )
		{
			final TextAnimator text = texts.get( i );
			text.setTime( time );

			final TextLayout layout = new TextLayout( text.text, font, frc );
			final Rectangle2D bounds = layout.getBounds();

			final double t = text.ratioComplete();
			final float alpha;
			if ( t <= fadeInTime )
				alpha = ( float ) Math.sin( ( Math.PI / 2 ) * t / fadeInTime );
			else if ( t >= 1.0 - fadeOutTime )
				alpha = ( float ) Math.sin( ( Math.PI / 2 ) * ( 1.0 - t ) / ( fadeOutTime ) );
			else
				alpha = 1;

			g.setColor( new Color( 1f, 1f, 1f, alpha ) );
			final float x = ( float ) ( ox - bounds.getMaxX() );
			final float y = ( float ) oy;
			layout.draw( g, x, y );

			oy += bounds.getY() - 5;

			if ( text.isComplete() )
				while ( texts.size() > i )
					texts.remove( i );
		}
	}
}
