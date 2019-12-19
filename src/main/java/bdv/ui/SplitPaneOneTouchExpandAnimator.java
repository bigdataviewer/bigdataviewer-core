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

import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.ACTIVATE_COLLAPS;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.ACTIVATE_EXPAND;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.DEACTIVATE_COLLAPS;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.DEACTIVATE_EXPAND;
import static bdv.ui.SplitPaneOneTouchExpandAnimator.AnimationType.NONE;

public class SplitPaneOneTouchExpandAnimator implements OverlayAnimator, MouseMotionListener, MouseListener
{

	private final BufferedImage leftarrow;

	private final BufferedImage rightarrow;

	private final SplitPanel splitPanel;

	private final int imgw;

	private final int imgh;

	private int viewPortWidth;

	private int viewPortHeight;

	private int mouseX;

	private int mouseY;

	private boolean isAnimating;

	private long last_time;

	private boolean collapsed;

	private float alpha = 1.0f;

	private int keyFrame;

	private final double animationSpeed = 0.009;

	private final float backgroundAlpha = 0.65f;

	private PaintState paintState;

	public SplitPaneOneTouchExpandAnimator( final SplitPanel viewer ) throws IOException
	{
		rightarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "rightdoublearrow_tiny.png" ) );
		leftarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "leftdoublearrow_tiny.png" ) );
		this.imgw = leftarrow.getWidth();
		this.imgh = leftarrow.getHeight() + 2;
		this.splitPanel = viewer;
	}

	enum AnimationType
	{
		ACTIVATE_EXPAND,
		DEACTIVATE_EXPAND,
		ACTIVATE_COLLAPS,
		DEACTIVATE_COLLAPS,
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
			case ACTIVATE_EXPAND:
				animation = new ActivateExpand();
				isAnimating = true;
				animationType = NONE;
				break;
			case DEACTIVATE_EXPAND:
				animation = new DeactivateExpand();
				isAnimating = true;
				animationType = NONE;
				break;
			case ACTIVATE_COLLAPS:
				animation = new ActivateCollapse();
				isAnimating = true;
				animationType = NONE;
				break;
			case DEACTIVATE_COLLAPS:
				animation = new DeactivateCollapse();
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

	interface Animation
	{
		PaintState animate( final long delta_time );

		boolean isComplete();
	}

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

	private void paint( final Graphics2D g, final PaintState state )
	{
		final int imgX = viewPortWidth - ( int ) ( imgw * state.imgRatio + 10 * state.bumpRatio );
		final int bgX = viewPortWidth - ( int ) ( imgw * state.bgRatio + 10 * state.bumpRatio );

		drawBackground( g, bgX, state.alpha );
		drawImg( g, collapsed ? leftarrow : rightarrow, imgX, state.alpha );
	}

	private Animation animation;

	private class DeactivateCollapse implements Animation
	{
		private boolean complete = false;

		@Override
		public PaintState animate( final long delta_time )
		{
			// Fade-out
			updateAlpha( false, delta_time );

			// No move animation required
			final int x = viewPortWidth - imgw;

			complete = alpha <= 0.25;

			return new PaintState( 1, 1, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}

	private class ActivateCollapse implements Animation
	{
		private int keyFrame = 0;

		// ratio to which the image is slid left (0 = all the way right, 1 = all the way left)
		private double expandRatio = 0;

		@Override
		public PaintState animate( final long delta_time )
		{
			// TODO : should become new animation speed
			// Slide image 10px to the left
			final double expandAnimationSpeed = animationSpeed / 10;

			if ( keyFrame == 0 )
			{
				// Slide image to the left
				expandRatio += expandAnimationSpeed * delta_time;
				expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
				if ( expandRatio == 1 )
					keyFrame = 1;

			}
			else if ( keyFrame == 1 )
			{
				// Slide image back to the initial position
				expandRatio -= expandAnimationSpeed * delta_time;
				expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
				if ( expandRatio == 0 )
					keyFrame = 2;
			}

			// Fade-in
			updateAlpha( true, delta_time );

			isAnimating = keyFrame < 2;

			return new PaintState( 1, 1, cos( expandRatio ), alpha );
		}

		@Override
		public boolean isComplete()
		{
			return !isAnimating;
		}
	}

	// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
	private double expandRatio = 0;

	private class DeactivateExpand implements Animation
	{

		@Override
		public PaintState animate( final long delta_time )
		{
			// TODO : should become new animation speed
			final double expandAnimationSpeed = animationSpeed / imgw;

			// Speed up animation by factor 2
			expandRatio -= 2 * expandAnimationSpeed * delta_time;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );

			// Fade-out
			updateAlpha( false, delta_time );

			// Set keyFrame-frames back based on expandRatio
			if ( expandRatio > 0.5 )
			{
				keyFrame = 2;
			}
			else if ( expandRatio > 0 )
			{
				keyFrame = 1;
			}
			else
			{
				keyFrame = 0;
			}

			isAnimating = expandRatio > 0;

			final double imgRatio = cos( expandRatio );
			return new PaintState( imgRatio, imgRatio, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return !isAnimating;
		}
	}

	private class ActivateExpand implements Animation
	{
		@Override
		public PaintState animate( final long delta_time )
		{
			// TODO : should become new animation speed
			final double expandAnimationSpeed = animationSpeed / imgw;

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

			isAnimating = keyFrame != 3;

			final double imgRatio = cos( expandRatio );
			final double bgRatio = keyFrame > 0 ? 1 : imgRatio; // Background should only move out and stay
			return new PaintState( imgRatio, bgRatio, 0, alpha );
		}

		@Override
		public boolean isComplete()
		{
			return !isAnimating;
		}
	}

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

	private void drawBackground( final Graphics2D g, final int x, final float alpha )
	{
		final int y = ( viewPortHeight - imgh ) / 2;
		final int width = imgw + 60;
		final int height = imgh;

		g.setColor( new Color( 0, 0, 0, Math.min( alpha, backgroundAlpha ) ) );
		g.fillRoundRect( x, y, width, height, 25, 25 );
	}

	private void drawImg( final Graphics2D g, final BufferedImage img, final int x, final float alpha )
	{
		final int y = ( viewPortHeight - imgh ) / 2;

		Composite oldComposite = g.getComposite();
		final AlphaComposite alcom = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha );
		g.setComposite( alcom );
		g.drawImage( img, x, y, null );
		g.setComposite(oldComposite);
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
				repaint( ACTIVATE_EXPAND );
			else
				repaint( DEACTIVATE_EXPAND );
		}
		else
		{
			if ( isInTriggerRegion() )
				repaint( ACTIVATE_COLLAPS );
			else
				repaint( DEACTIVATE_COLLAPS );
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
				expandRatio = 1;
				keyFrame = 3;
				isAnimating = false;
				repaint( DEACTIVATE_EXPAND );
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
