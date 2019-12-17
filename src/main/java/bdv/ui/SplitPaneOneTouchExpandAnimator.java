package bdv.ui;

import bdv.viewer.animate.OverlayAnimator;
import java.awt.AlphaComposite;
import java.awt.Color;
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

	private float imgX;

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
		this.imgX = 0;
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
			if ( viewPortWidth - mouseX < imgw )
			{
				if ( collapsed )
					activateExpand( g, time_since_last_update );
				else if ( getLocationY() < mouseY && mouseY < getLocationY() + imgh )
					activateCollapse( g, time_since_last_update );
				else
					deactivateCollapse( g, time_since_last_update );
			}
			else
			{
				if ( collapsed )
					deactivateExpand( g, time_since_last_update );
				else
					deactivateCollapse( g, time_since_last_update );
			}
			time_since_last_update = 0;
		}
		last_time = time;
	}

	private void deactivateCollapse( final Graphics2D g, final long time_since_last_update )
	{
		// Fade-out
		getAlpha( false );

		// No move animation required
		final int y = getLocationY();
		imgX = viewPortWidth - imgw;

		drawBackground( g, ( int ) imgX, y, ( int ) imgX + 50, imgh, Math.min( alpha, backgroundAlpha ) );
		drawImg( g, rightarrow, ( int ) imgX, y, alpha );

		// Reset animation keyFrame-frame
		keyFrame = 0;
		repaint = alpha != 0.25;
	}

	private void activateCollapse( final Graphics2D g, final long time_since_last_update )
	{
		// Fade-in
		getAlpha( true );

		// Compute X position of the image
		if ( keyFrame == 0 )
		{
			// Slide image 10px to the left
			imgX -= animationSpeed * time_since_last_update;
			imgX = Math.max( viewPortWidth - imgw - 10, imgX );
			if ( imgX == ( viewPortWidth - imgw - 10 ) )
				keyFrame = 1;

		}
		else if ( keyFrame == 1 )
		{
			// Slide image back to the initial position
			imgX += animationSpeed * time_since_last_update;
			imgX = Math.min( viewPortWidth - imgw, imgX );
			if ( imgX == ( viewPortWidth - imgw ) )
				keyFrame = 2;
		}
		else
		{
			// Animation finished
			imgX = viewPortWidth - imgw;
		}

		imgX = Math.max( viewPortWidth - imgw - 10, imgX );
		final int y = getLocationY();

		drawBackground( g, ( int ) imgX, y, ( int ) imgX + 50, imgh, Math.min( alpha, backgroundAlpha ) );
		drawImg( g, rightarrow, ( int ) imgX, y, alpha );

		repaint = keyFrame != 2 || alpha != 1.0;
	}

	private void deactivateExpand( final Graphics2D g, final long time_since_last_update )
	{
		// Fade-out
		getAlpha( false );

		// Speed up animation by factor 2
		imgX += 2 * animationSpeed * time_since_last_update;
		imgX = Math.max( viewPortWidth - imgw, Math.min( viewPortWidth, imgX ) );
		final int y = getLocationY();

		drawBackground( g, ( int ) imgX, y, ( int ) imgX + 50, imgh, alpha );
		drawImg( g, leftarrow, ( int ) imgX, y, alpha );

		// Set keyFrame-frames back based on image position
		if ( imgX < viewPortWidth - imgw + imgw / 2 )
		{
			keyFrame = 2;
		}
		else if ( imgX < viewPortWidth - imgw )
		{
			keyFrame = 1;
		}
		else
		{
			keyFrame = 0;
		}

		repaint = imgX < viewPortWidth;
	}

	private void activateExpand( final Graphics2D g, final long time_since_last_update )
	{
		// Fade-in
		getAlpha( true );

		// Slide image in
		if ( keyFrame == 0 )
		{
			// Slide image in with doubled speed
			imgX -= 2 * animationSpeed * time_since_last_update;
			imgX = Math.max( viewPortWidth - imgw, imgX );
			if ( imgX == viewPortWidth - imgw )
			{
				keyFrame = 1;
			}
		}
		else if ( keyFrame == 1 )
		{
			// Move it half back
			imgX += animationSpeed * time_since_last_update;
			imgX = Math.min( viewPortWidth - imgw + imgw / 2, imgX );
			if ( imgX == viewPortWidth - imgw + imgw / 2 )
			{
				keyFrame = 2;
			}
		}
		else if ( keyFrame == 2 )
		{
			// And move it again full in
			imgX -= animationSpeed * time_since_last_update;
			imgX = Math.max( viewPortWidth - imgw, imgX );
			if ( imgX == viewPortWidth - imgw )
			{
				keyFrame = 3;
			}
		}
		else
		{
			imgX = viewPortWidth - imgw;
		}
		final int y = getLocationY();

		// Background should only move out and stay
		if ( keyFrame > 0 )
			drawBackground( g, viewPortWidth - imgw, y, imgw + 50, imgh, alpha );
		else
			drawBackground( g, ( int ) imgX, y, ( int ) imgX + 50, imgh, alpha );

		drawImg( g, leftarrow, ( int ) imgX, y, alpha );

		repaint = keyFrame != 3;
	}

	private int getLocationY()
	{
		return ( viewPortHeight - imgh ) / 2;
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

	private void drawBackground( final Graphics2D g, final int x, final int y, final int width, final int height, final float alpha )
	{
		final AlphaComposite alcom = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, Math.min( alpha, backgroundAlpha ) );
		g.setColor( Color.black );
		g.setComposite( alcom );
		g.fillRoundRect( x, y, width, height, 25, 25 );
	}

	private void drawImg( final Graphics2D g, final BufferedImage img, final int x, final int y, final float alpha )
	{
		final AlphaComposite alcom = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, alpha );
		g.setComposite( alcom );
		g.drawImage( img, x, y, null );
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

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		mouseX = e.getX();
		mouseY = e.getY();
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
