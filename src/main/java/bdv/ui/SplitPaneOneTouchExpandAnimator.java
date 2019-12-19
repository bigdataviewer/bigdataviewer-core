package bdv.ui;

import bdv.viewer.animate.OverlayAnimator;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_COLLAPSE;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_EXPAND;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_COLLAPSE;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_EXPAND;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.NONE;

public class SplitPaneOneTouchExpandAnimator implements OverlayAnimator, MouseMotionListener, MouseListener
{
	private final BufferedImage leftarrow;
	private final BufferedImage rightarrow;
	private final int imgw;
	private final int imgh;
	private final double animationSpeed = 0.09;
	private final float backgroundAlpha = 0.65f;

	private float alpha = 1.0f;

	private int viewPortWidth;
	private int viewPortHeight;



	private final SplitPanel splitPanel;
	private boolean collapsed;


	private int mouseX;
	private int mouseY;


	private boolean isAnimating;
	private long last_time;

	public SplitPaneOneTouchExpandAnimator( final SplitPanel viewer ) throws IOException
	{
		rightarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "rightdoublearrow_tiny.png" ) ); // TODO: use ImageIcon instead?
		leftarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "leftdoublearrow_tiny.png" ) ); // TODO: use ImageIcon instead?
		this.imgw = leftarrow.getWidth();
		this.imgh = leftarrow.getHeight() + 2; // TODO: why +2?
		this.splitPanel = viewer;
	}

	enum AnimationType
	{
		SHOW_EXPAND,
		HIDE_EXPAND,
		SHOW_COLLAPSE,
		HIDE_COLLAPSE,
		NONE
	}

	private AnimationType animationType = NONE;

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		viewPortWidth = g.getClipBounds().width;
		viewPortHeight = g.getClipBounds().height;

		if (!isAnimating)
		{
			last_time = time;
			switch ( animationType )
			{
			case SHOW_EXPAND:
				animation = new ShowExpandButton( animation );
				isAnimating = true;
				animationType = NONE;
				break;
			case HIDE_EXPAND:
				animation = new HideExpandButton( animation );
				isAnimating = true;
				animationType = NONE;
				break;
			case SHOW_COLLAPSE:
				animation = new ShowCollapseButton();
				isAnimating = true;
				animationType = NONE;
				break;
			case HIDE_COLLAPSE:
				animation = new HideCollapseButton();
				isAnimating = true;
				animationType = NONE;
				break;
			}
		}

		final long delta_time = time - last_time;
		if ( animation != null )
		{
			paintState = animation.animate( delta_time );
			if ( animation.isComplete() )
			{
				animation = null;
				isAnimating = false;
			}
		}

		last_time = time;

		if ( paintState != null )
			paint( g, paintState );
	}

	@Override
	public boolean isComplete()
	{
		return false;
	}

	@Override
	public boolean requiresRepaint()
	{
		return isAnimating;
	}


	// == PAINTING ===========================================================

	private static class PaintState
	{
		final double imgRatio;
		final double bgRatio;
		final double bumpRatio;
		final float alpha;

		private PaintState( final double imgRatio, final double bgRatio, final double bumpRatio, final float alpha )
		{
			this.imgRatio = imgRatio;
			this.bgRatio = bgRatio;
			this.bumpRatio = bumpRatio;
			this.alpha = alpha;
		}
	}

	private PaintState paintState;

	private void paint( final Graphics2D g, final PaintState state )
	{
		final int imgX = viewPortWidth - ( int ) ( imgw * state.imgRatio + 10 * state.bumpRatio );
		final int bgX = viewPortWidth - ( int ) ( imgw * state.bgRatio + 10 * state.bumpRatio );
		final int y = ( viewPortHeight - imgh ) / 2;

		drawBackground( g, bgX, y, state.alpha );
		drawImg( g, collapsed ? leftarrow : rightarrow, imgX, y, state.alpha );
	}

	private void drawBackground( final Graphics2D g, final int x, final int y, final float alpha )
	{
		final int width = imgw + 60;
		final int height = imgh;

		g.setColor( new Color( 0, 0, 0, Math.min( alpha, backgroundAlpha ) ) );
		g.fillRoundRect( x, y, width, height, 25, 25 );
	}

	private void drawImg( final Graphics2D g, final BufferedImage img, final int x, final int y, final float alpha )
	{
		Composite oldComposite = g.getComposite();
		final AlphaComposite alcom = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha );
		g.setComposite( alcom );
		g.drawImage( img, x, y, null );
		g.setComposite(oldComposite);
	}


	// == ANIMATOR HELPERS ===================================================

	/**
	 * Cosine shape acceleration/ deceleration curve  of linear [0,1]
	 */
	private double cos( final double t )
	{
		return 0.5 - 0.5 * Math.cos( Math.PI * t );
	}

	private void updateAlpha( final boolean fadeIn, final long delta_time )
	{
		if ( fadeIn )
		{
			alpha += ( 1 / 250.0 ) * delta_time;
			alpha = Math.min( 1, alpha );
		}
		else
		{
			alpha -= ( 1 / 250.0 ) * delta_time;
			alpha = Math.max( 0.25f, alpha );
		}
	}


	// == ANIMATORS ==========================================================

	interface Animation
	{
		PaintState animate( final long delta_time );

		boolean isComplete();
	}

	private Animation animation;



	// =======================================================================

	private class HideCollapseButton implements Animation
	{
		private boolean complete = false;

		@Override
		public PaintState animate( final long delta_time )
		{
			// Fade-out
			updateAlpha( false, delta_time );

			complete = alpha <= 0.25;

			return new PaintState( 1, 1, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}



	// =======================================================================

	private class ShowCollapseButton implements Animation
	{
		// TODO (?)
		// Slide image 10px to the left
		private final double bumpAnimationSpeed = animationSpeed / 10;

		private int keyFrame = 0;

		// ratio to which the image is slid left (0 = all the way right, 1 = all the way left)
		private double bumpRatio = 0;

		private boolean complete = false;

		@Override
		public PaintState animate( final long delta_time )
		{
			if ( keyFrame == 0 )
			{
				// Slide image to the left
				bumpRatio += bumpAnimationSpeed * delta_time;
				bumpRatio = Math.min( 1, Math.max( 0, bumpRatio ) );
				if ( bumpRatio == 1 )
					keyFrame = 1;

			}
			else if ( keyFrame == 1 )
			{
				// Slide image back to the initial position
				bumpRatio -= bumpAnimationSpeed * delta_time;
				bumpRatio = Math.min( 1, Math.max( 0, bumpRatio ) );
				if ( bumpRatio == 0 )
					keyFrame = 2;
			}

			// Fade-in
			updateAlpha( true, delta_time );

			complete = keyFrame >= 2;

			return new PaintState( 1, 1, cos( bumpRatio ), alpha );
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}



	// =======================================================================

	private class HideExpandButton implements Animation
	{
		// TODO (?)
		private final double expandAnimationSpeed = animationSpeed / imgw;

		// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
		private double expandRatio;

		private boolean complete = false;

		public HideExpandButton( Animation currentAnimation )
		{
			if ( currentAnimation != null && !currentAnimation.isComplete() && currentAnimation instanceof ShowExpandButton )
				// if an incomplete ShowExpandButton animation is running, initialize expandRatio to match
				expandRatio = ( ( ShowExpandButton ) currentAnimation ).expandRatio;
			else
				// otherwise start from fully expanded image
				expandRatio = 1;
		}

		@Override
		public PaintState animate( final long delta_time )
		{

			// Speed up animation by factor 2
			expandRatio -= 2 * expandAnimationSpeed * delta_time;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );

			// Fade-out
			updateAlpha( false, delta_time );

			complete = expandRatio <= 0;

			final double imgRatio = cos( expandRatio );
			return new PaintState( imgRatio, imgRatio, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}

	private class ShowExpandButton implements Animation
	{
		// TODO (?)
		private final double expandAnimationSpeed = animationSpeed / imgw;

		private int keyFrame;

		// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
		private double expandRatio;

		private boolean complete = false;

		public ShowExpandButton( Animation currentAnimation )
		{
			if ( currentAnimation != null && !currentAnimation.isComplete() && currentAnimation instanceof HideExpandButton )
				// if an incomplete HideExpandButton animation is running, initialize expandRatio to match
				expandRatio = ( ( HideExpandButton ) currentAnimation ).expandRatio;
			else
				// otherwise start from fully hidden image
				expandRatio = 0;

			// initialize keyFrame based on expandRatio
//			if ( expandRatio > 0.5 )
//				keyFrame = 2;
//			else
			if ( expandRatio > 0 )
				keyFrame = 1;
			else
				keyFrame = 0;
		}

		@Override
		public PaintState animate( final long delta_time )
		{
			// Slide image in
			if ( keyFrame == 0 )
			{
				// Slide image in with doubled speed
				expandRatio += delta_time * 2 * expandAnimationSpeed;
				expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
				if ( expandRatio == 1 )
				{
					keyFrame = 1;
				}
			}
			else if ( keyFrame == 1 )
			{
				// Move it half back
				expandRatio -= delta_time * expandAnimationSpeed;
				expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
				if ( expandRatio <= 0.5 )
				{
					keyFrame = 2;
				}
			}
			else if ( keyFrame == 2 )
			{
				// And move it again full in
				expandRatio += delta_time * expandAnimationSpeed;
				expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
				if ( expandRatio == 1 )
				{
					keyFrame = 3;
				}
			}
			else
			{
				expandRatio = 1;
			}

			// Fade-in
			updateAlpha( true, delta_time );

			complete = keyFrame >= 3;

			final double imgRatio = cos( expandRatio );
			final double bgRatio = keyFrame > 0 ? 1 : imgRatio; // Background should only move out and stay
			return new PaintState( imgRatio, bgRatio, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}
















	// == MOUSE HANDLER ======================================================

	@Override
	public void mouseDragged( final MouseEvent e )
	{
	}

	private boolean nearBorderX = false;
	private boolean nearBorderY = false;

	public boolean isNearBorder()
	{
		return nearBorderX;
	}

	public boolean isInTriggerRegion()
	{
		return nearBorderX && nearBorderY;
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		mouseX = e.getX();
		mouseY = e.getY();

		// TODO: expand trigger region (>imgw) ?
		if ( viewPortWidth - mouseX < imgw )
		{
			if ( !nearBorderX )
			{
				nearBorderX = true;
				enterBorderRegionX();
			}
		}
		else
		{
			if ( nearBorderX )
			{
				nearBorderX = false;
				exitBorderRegionX();
			}

		}

		// TODO: expand trigger region (>imgh) ?
		if ( Math.abs( viewPortHeight - 2 * mouseY ) < imgh )
		{
			if ( !nearBorderY )
			{
				nearBorderY = true;
				enterBorderRegionY();
			}
		}
		else
		{
			if ( nearBorderY )
			{
				nearBorderY = false;
				exitBorderRegionY();
			}
		}
	}

	private void repaint()
	{
		if ( collapsed )
		{
			if ( isNearBorder() )
				repaint( SHOW_EXPAND );
			else
				repaint( HIDE_EXPAND );
		}
		else
		{
			if ( isInTriggerRegion() )
				repaint( SHOW_COLLAPSE );
			else
				repaint( HIDE_COLLAPSE );
		}
	}

	private void repaint( final AnimationType animationType )
	{
		this.animationType = animationType;
		isAnimating = false;
		splitPanel.getViewerPanel().getDisplay().repaint();
	}

	private void exitBorderRegionY()
	{
		repaint();
	}

	private void enterBorderRegionY()
	{
		repaint();
	}

	private void exitBorderRegionX()
	{
		repaint();
	}

	private void enterBorderRegionX()
	{
		repaint();
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		if ( mouseX > viewPortWidth - imgw && e.getY() < ( viewPortHeight / 2 ) + 50 && e.getY() > ( viewPortHeight / 2 ) - 50 )
		{
			collapsed = !collapsed;
			isAnimating = true;
			splitPanel.collapseUI();
			if ( collapsed )
			{
				isAnimating = false;
				repaint( HIDE_EXPAND );
			}
			splitPanel.getViewerPanel().requestRepaint();
		}
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{

	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{

	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		System.out.println( "mouseEntered" );
		repaint();
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		System.out.println( "mouseExited" );
		repaint();
	}
}
