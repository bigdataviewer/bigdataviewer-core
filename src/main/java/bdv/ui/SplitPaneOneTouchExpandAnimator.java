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

	private boolean repaint;

	private long last_time;

	private long time_since_last_update;

	private boolean collapsed;

	private float alpha = 1.0f;

	private int keyFrame;

	private double animationSpeed = 0.09;

	private float backgroundAlpha = 0.65f;

	public SplitPaneOneTouchExpandAnimator( final SplitPanel viewer ) throws IOException
	{
		rightarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "rightdoublearrow_tiny.png" ) );
		leftarrow = ImageIO.read( SplitPaneOneTouchExpandAnimator.class.getResource( "leftdoublearrow_tiny.png" ) );
		this.imgw = leftarrow.getWidth();
		this.imgh = leftarrow.getHeight() + 2;
		this.splitPanel = viewer;
	}

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		viewPortWidth = g.getClipBounds().width;
		viewPortHeight = g.getClipBounds().height;

		time_since_last_update += time - last_time;
		if ( time_since_last_update > 1 )
		{
//			System.out.println( "time_since_last_update = " + time_since_last_update );

			if ( collapsed )
			{
				if ( isNearBorder() )
					activateExpand( g, time_since_last_update );
				else
					deactivateExpand( g, time_since_last_update );
			}
			else
			{
				if ( isInTriggerRegion() )
					activateCollapse( g, time_since_last_update );
				else
					deactivateCollapse( g, time_since_last_update );
			}

			time_since_last_update = 0;
		}
		last_time = time;
	}

	private void deactivateCollapse( final Graphics2D g, final long time_since_last_update )
	{
		// TODO : should become new animation speed
		final double expandAnimationSpeed = animationSpeed / imgw;

		// Fade-out
		getAlpha( false );

		// No move animation required
		final int x = viewPortWidth - imgw;

		drawBackground( g, x, Math.min( alpha, backgroundAlpha ) );
		drawImg( g, rightarrow, x, alpha );

		// Reset animation keyFrame-frame
		keyFrame = 0;
		repaint = alpha > 0.25;
	}

	private void activateCollapse( final Graphics2D g, final long time_since_last_update )
	{
		// TODO : should become new animation speed
		// Slide image 10px to the left
		final double expandAnimationSpeed = animationSpeed / 10;

		if ( keyFrame == 0 )
		{
			// Slide image to the left
			expandRatio += expandAnimationSpeed * time_since_last_update;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
			if ( expandRatio == 1 )
				keyFrame = 1;

		}
		else if ( keyFrame == 1 )
		{
			// Slide image back to the initial position
			expandRatio -= expandAnimationSpeed * time_since_last_update;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
			if ( expandRatio == 0 )
				keyFrame = 2;
		}
		else
		{
			// Animation finished
			expandRatio = 0; // TODO: remove?
		}

		// Fade-in
		getAlpha( true );

		final int x = viewPortWidth - ( int ) ( imgw + 10 * cos( expandRatio ) );

		drawBackground( g, x, Math.min( alpha, backgroundAlpha ) );
		drawImg( g, rightarrow, x, alpha );

		repaint = keyFrame != 2 || alpha != 1.0;
	}




	// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
	private double expandRatio = 0;

	private void deactivateExpand( final Graphics2D g, final long time_since_last_update )
	{
		// TODO : should become new animation speed
		final double expandAnimationSpeed = animationSpeed / imgw;

		// Speed up animation by factor 2
		expandRatio -= 2 * expandAnimationSpeed * time_since_last_update;
		expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );

		// Fade-out
		getAlpha( false );

		final int x = viewPortWidth - ( int ) ( imgw * cos( expandRatio ) );

		drawBackground( g, x, alpha );
		drawImg( g, leftarrow, x, alpha );

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

		repaint = expandRatio > 0;
	}

	private void activateExpand( final Graphics2D g, final long time_since_last_update )
	{
		// TODO : should become new animation speed
		final double expandAnimationSpeed = animationSpeed / imgw;

		// Slide image in
		if ( keyFrame == 0 )
		{
			// Slide image in with doubled speed
			expandRatio += time_since_last_update * 2 * expandAnimationSpeed;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
			if ( expandRatio == 1 )
			{
				keyFrame = 1;
			}
		}
		else if ( keyFrame == 1 )
		{
			// Move it half back
			expandRatio -= time_since_last_update * expandAnimationSpeed;
			expandRatio = Math.min( 1, Math.max( 0, expandRatio ) );
			if ( expandRatio <= 0.5 )
			{
				keyFrame = 2;
			}
		}
		else if ( keyFrame == 2 )
		{
			// And move it again full in
			expandRatio += time_since_last_update * expandAnimationSpeed;
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
		getAlpha( true );

		final int x = viewPortWidth - ( int ) ( imgw * cos( expandRatio ) );

		// Background should only move out and stay
		if ( keyFrame > 0 )
			drawBackground( g, viewPortWidth - imgw, alpha );
		else
			drawBackground( g, x, alpha );

		drawImg( g, leftarrow, x, alpha );

		repaint = keyFrame != 3;
	}

	/**
	 * Cosine shape acceleration/ deceleration curve  of linear [0,1]
	 */
	private double cos( final double t )
	{
		return 0.5 - 0.5 * Math.cos( Math.PI * t );
	}

	private void getAlpha( final boolean fadeIn )
	{
		if ( fadeIn )
		{
			alpha += ( 1 / 250.0 ) * time_since_last_update;
			alpha = Math.min( 1, alpha );
		}
		else
		{
			alpha -= ( 1 / 250.0 ) * time_since_last_update;
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
		return repaint;
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
				enterBorderRegionX();
				nearBorderX = true;
			}
		}
		else
		{
			if ( nearBorderX )
			{
				exitBorderRegionX();
				nearBorderX = false;
			}

		}

		// TODO: expand trigger region (>imgh) ?
		if ( Math.abs( viewPortHeight - 2 * mouseY ) < imgh )
		{
			if ( !nearBorderY )
			{
				enterBorderRegionY();
				nearBorderY = true;
			}
		}
		else
		{
			if ( nearBorderY )
			{
				exitBorderRegionY();
				nearBorderY = false;
			}
		}
	}

	private void exitBorderRegionY()
	{
	}

	private void enterBorderRegionY()
	{
	}

	private void exitBorderRegionX()
	{

	}

	private void enterBorderRegionX()
	{
		splitPanel.getViewerPanel().getDisplay().repaint();
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		if ( mouseX > viewPortWidth - imgw && e.getY() < ( viewPortHeight / 2 ) + 50 && e.getY() > ( viewPortHeight / 2 ) - 50 )
		{
			collapsed = !collapsed;
			repaint = true;
			splitPanel.collapseUI();
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

	}

	@Override
	public void mouseExited( final MouseEvent e )
	{

	}
}
