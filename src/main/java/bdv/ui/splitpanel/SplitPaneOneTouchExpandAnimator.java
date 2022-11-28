/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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

import bdv.ui.UIUtils;
import bdv.viewer.animate.OverlayAnimator;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.function.BooleanSupplier;
import javax.swing.ImageIcon;

import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_COLLAPSE;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.SHOW_EXPAND;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_COLLAPSE;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.HIDE_EXPAND;
import static bdv.ui.splitpanel.SplitPaneOneTouchExpandAnimator.AnimationType.NONE;

/**
 * @author Tim-Oliver Buchholz
 * @author Tobias Pietzsch
 */
class SplitPaneOneTouchExpandAnimator implements OverlayAnimator
{
	private final BooleanSupplier isCollapsed;

	private ImageIcon rightArrowIcon;
	private ImageIcon leftArrowIcon;
	private int imgw;
	private int imgh;

	private int borderWidth;
	private int triggerHeight;

	private final double animationSpeed = 0.09;
	private final float backgroundAlpha = 0.65f;

	private float alpha = 1.0f;

	private int viewPortWidth;
	private int viewPortHeight;

	private double uiScale = -1;

	/**
	 * @param isCollapsed provides collapsed state to decide whether to display left-arrow of right-arrow icon
	 */
	public SplitPaneOneTouchExpandAnimator( final BooleanSupplier isCollapsed )
	{
		this.isCollapsed = isCollapsed;
	}

	public enum AnimationType
	{
		SHOW_EXPAND,
		HIDE_EXPAND,
		SHOW_COLLAPSE,
		HIDE_COLLAPSE,
		NONE
	}

	public synchronized void startAnimation( final AnimationType animationType )
	{
		requestedAnimationType = animationType;
	}

	private AnimationType requestedAnimationType = NONE;

	private long last_time;

	private void updateUIScale()
	{
		final double s = UIUtils.getUIScaleFactor( this );
		if ( s != uiScale )
		{
			uiScale = s;

			rightArrowIcon = new ImageIcon( SplitPaneOneTouchExpandAnimator.class.getResource( "rightdoublearrow_tiny.png" ) );
			leftArrowIcon = new ImageIcon( SplitPaneOneTouchExpandAnimator.class.getResource( "leftdoublearrow_tiny.png" ) );
			imgw = leftArrowIcon.getIconWidth();
			imgh = leftArrowIcon.getIconHeight();

			// TODO: created images for different scales and load the appropriate one.
			//       similar to how it's done in bdv.ui.viewermodepanel.DisplaySettingsPanel.
			if ( uiScale != 1 )
			{
				rightArrowIcon.setImage(
						rightArrowIcon.getImage().getScaledInstance(
								( int ) (uiScale * imgw), ( int ) (uiScale * imgh), Image.SCALE_SMOOTH ) );
				leftArrowIcon.setImage(
						leftArrowIcon.getImage().getScaledInstance(
								( int ) (uiScale * imgw), ( int ) (uiScale * imgh), Image.SCALE_SMOOTH ) );
				imgw = leftArrowIcon.getIconWidth();
				imgh = leftArrowIcon.getIconHeight();
			}

			borderWidth = ( int ) ( imgw + 10 * uiScale );
			triggerHeight = ( int ) ( imgh + 10 * uiScale );
		}
	}

	@Override
	public void paint( final Graphics2D g, final long time )
	{
		updateUIScale();

		viewPortWidth = g.getClipBounds().width;
		viewPortHeight = g.getClipBounds().height;

		if ( requestedAnimationType != NONE )
		{
			if ( animator == null || animator.animationType() != requestedAnimationType )
			{
				last_time = time;

				switch ( requestedAnimationType )
				{
				case SHOW_EXPAND:
					animator = new ShowExpandButton( animator );
					break;
				case HIDE_EXPAND:
					animator = new HideExpandButton( animator );
					break;
				case SHOW_COLLAPSE:
					animator = new ShowCollapseButton();
					break;
				case HIDE_COLLAPSE:
					animator = new HideCollapseButton();
					break;
				}
			}
			requestedAnimationType = NONE;
		}

		if ( animator != null )
		{
			final long delta_time = time - last_time;
			last_time = time;

			paintState = animator.animate( delta_time );
			if ( animator.isComplete() )
				animator = null;
		}

		if ( paintState != null )
			paint( g, paintState );
	}

	void clearPaintState()
	{
		paintState = null;
	}

	@Override
	public boolean isComplete()
	{
		return false;
	}

	@Override
	public boolean requiresRepaint()
	{
		return animator != null;
	}


	// == TRIGGER REGION =====================================================

	public boolean isInBorderRegion( final int x, final int y )
	{
		return x > viewPortWidth - borderWidth;
	}

	public boolean isInTriggerRegion( final int x, final int y )
	{
		return x > viewPortWidth - borderWidth && Math.abs( viewPortHeight - 2 * y ) < triggerHeight;
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
		drawImg( g, isCollapsed.getAsBoolean() ? leftArrowIcon : rightArrowIcon, imgX, y, state.alpha );
	}

	private void drawBackground( final Graphics2D g, final int x, final int y, final float alpha )
	{
		final int width = imgw + 60;
		final int height = imgh;

		g.setColor( new Color( 0.28f, 0.5f, 0.96f, Math.min( alpha, backgroundAlpha ) ) );
		g.fillRoundRect( x, y, width, height, 25, 25 );
	}

	private void drawImg( final Graphics2D g, final ImageIcon img, final int x, final int y, final float alpha )
	{
		Composite oldComposite = g.getComposite();
		final AlphaComposite alcom = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha );
		g.setComposite( alcom );
		g.drawImage( img.getImage(), x, y, null );
		g.setComposite(oldComposite);
	}


	// == ANIMATOR HELPERS ===================================================

	/**
	 * Cosine shape acceleration/ deceleration curve  of linear [0,1]
	 */
	private static double cos( final double t )
	{
		return 0.5 - 0.5 * Math.cos( Math.PI * t );
	}

	// TODO: Animator implementations should maintain their own private alpha instead of using this shared method
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

	private interface Animator
	{
		PaintState animate( final long delta_time );

		AnimationType animationType();

		boolean isComplete();
	}

	private Animator animator;

	// =======================================================================

	private class HideCollapseButton implements Animator
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
		public AnimationType animationType()
		{
			return HIDE_COLLAPSE;
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}

	// =======================================================================

	private class ShowCollapseButton implements Animator
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
		public AnimationType animationType()
		{
			return SHOW_COLLAPSE;
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}

	// =======================================================================

	private class HideExpandButton implements Animator
	{
		// TODO (?)
		private final double expandAnimationSpeed = animationSpeed / imgw;

		// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
		private double expandRatio;

		private boolean complete = false;

		public HideExpandButton( Animator currentAnimator )
		{
			if ( currentAnimator != null && !currentAnimator.isComplete() && currentAnimator instanceof ShowExpandButton )
				// if an incomplete ShowExpandButton animation is running, initialize expandRatio to match
				expandRatio = ( ( ShowExpandButton ) currentAnimator ).expandRatio;
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
		public AnimationType animationType()
		{
			return HIDE_EXPAND;
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}

	// =======================================================================

	private class ShowExpandButton implements Animator
	{
		// TODO (?)
		private final double expandAnimationSpeed = animationSpeed / imgw;

		private int keyFrame;

		// ratio to which the image is expanded (0 = hidden, 1 = fully expanded)
		private double expandRatio;

		private boolean complete = false;

		public ShowExpandButton( Animator currentAnimator )
		{
			if ( currentAnimator != null && !currentAnimator.isComplete() && currentAnimator instanceof HideExpandButton )
				// if an incomplete HideExpandButton animation is running, initialize expandRatio to match
				expandRatio = ( ( HideExpandButton ) currentAnimator ).expandRatio;
			else
				// otherwise start from fully hidden image
				expandRatio = 0;

			// initialize keyFrame based on expandRatio
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
		public AnimationType animationType()
		{
			return SHOW_EXPAND;
		}

		@Override
		public boolean isComplete()
		{
			return complete;
		}
	}
}
