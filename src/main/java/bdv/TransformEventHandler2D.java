/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * A {@link TransformEventHandler} that changes an {@link AffineTransform3D}
 * through a set of {@link Behaviour}s.
 *
 * @author Tobias Pietzsch
 */
public class TransformEventHandler2D implements TransformEventHandler
{
	// -- behaviour names --

	private static final String DRAG_TRANSLATE = "2d drag translate";
	private static final String DRAG_ROTATE = "2d drag rotate";

	private static final String ZOOM_NORMAL = "2d scroll zoom";
	private static final String SCROLL_TRANSLATE = "2d scroll translate";
	private static final String SCROLL_ROTATE = "2d scroll rotate";
	private static final String ROTATE_LEFT = "2d rotate left";
	private static final String ROTATE_RIGHT = "2d rotate right";
	private static final String KEY_ZOOM_IN = "2d zoom in";
	private static final String KEY_ZOOM_OUT = "2d zoom out";

	private static final String ZOOM_FAST = "2d scroll zoom fast";
	private static final String SCROLL_TRANSLATE_FAST = "2d scroll translate fast";
	private static final String SCROLL_ROTATE_FAST = "2d scroll rotate fast";
	private static final String ROTATE_LEFT_FAST = "2d rotate left fast";
	private static final String ROTATE_RIGHT_FAST = "2d rotate right fast";
	private static final String KEY_ZOOM_IN_FAST = "2d zoom in fast";
	private static final String KEY_ZOOM_OUT_FAST = "2d zoom out fast";

	private static final String ZOOM_SLOW = "2d scroll zoom slow";
	private static final String SCROLL_TRANSLATE_SLOW = "2d scroll translate slow";
	private static final String SCROLL_ROTATE_SLOW = "2d scroll rotate slow";
	private static final String ROTATE_LEFT_SLOW = "2d rotate left slow";
	private static final String ROTATE_RIGHT_SLOW = "2d rotate right slow";
	private static final String KEY_ZOOM_IN_SLOW = "2d zoom in slow";
	private static final String KEY_ZOOM_OUT_SLOW = "2d zoom out slow";

	// -- default shortcuts --

	private static final String[] DRAG_TRANSLATE_KEYS = new String[] { "button2", "button3" };
	private static final String[] DRAG_ROTATE_KEYS = new String[] { "button1" };

	private static final String[] ZOOM_NORMAL_KEYS = new String[] { "meta scroll", "ctrl shift scroll" };
	private static final String[] SCROLL_TRANSLATE_KEYS = new String[] { "not mapped" };
	private static final String[] SCROLL_ROTATE_KEYS = new String[] { "scroll" };
	private static final String[] ROTATE_LEFT_KEYS = new String[] { "LEFT" };
	private static final String[] ROTATE_RIGHT_KEYS = new String[] { "RIGHT" };
	private static final String[] KEY_ZOOM_IN_KEYS = new String[] { "UP" };
	private static final String[] KEY_ZOOM_OUT_KEYS = new String[] { "DOWN" };

	private static final String[] ZOOM_FAST_KEYS = new String[] { "shift scroll" };
	private static final String[] SCROLL_TRANSLATE_FAST_KEYS = new String[] { "not mapped" };
	private static final String[] SCROLL_TRANSLATE_SLOW_KEYS = new String[] { "not mapped" };
	private static final String[] ROTATE_LEFT_FAST_KEYS = new String[] { "shift LEFT" };
	private static final String[] ROTATE_RIGHT_FAST_KEYS = new String[] { "shift RIGHT" };
	private static final String[] KEY_ZOOM_IN_FAST_KEYS = new String[] { "shift UP" };
	private static final String[] KEY_ZOOM_OUT_FAST_KEYS = new String[] { "shift DOWN" };

	private static final String[] ZOOM_SLOW_KEYS = new String[] { "ctrl scroll" };
	private static final String[] SCROLL_ROTATE_FAST_KEYS = new String[] { "shift scroll" };
	private static final String[] SCROLL_ROTATE_SLOW_KEYS = new String[] { "ctrl scroll" };
	private static final String[] ROTATE_LEFT_SLOW_KEYS = new String[] { "ctrl LEFT" };
	private static final String[] ROTATE_RIGHT_SLOW_KEYS = new String[] { "ctrl RIGHT" };
	private static final String[] KEY_ZOOM_IN_SLOW_KEYS = new String[] { "ctrl UP" };
	private static final String[] KEY_ZOOM_OUT_SLOW_KEYS = new String[] { "ctrl DOWN" };

	// -- behaviours --

	private final DragTranslate dragTranslate;
	private final DragRotate dragRotate;
	private final Zoom zoom;
	private final Zoom zoomFast;
	private final Zoom zoomSlow;
	private final ScrollTranslate scrollTranslate;
	private final ScrollTranslate scrollTranslateFast;
	private final ScrollTranslate scrollTranslateSlow;
	private final ScrollRotate scrollRotate;
	private final ScrollRotate scrollRotateFast;
	private final ScrollRotate scrollRotateSlow;
	private final KeyRotate keyRotateLeft;
	private final KeyRotate keyRotateLeftFast;
	private final KeyRotate keyRotateLeftSlow;
	private final KeyRotate keyRotateRight;
	private final KeyRotate keyRotateRightFast;
	private final KeyRotate keyRotateRightSlow;
	private final KeyZoom keyZoomIn;
	private final KeyZoom keyZoomInFast;
	private final KeyZoom keyZoomInSlow;
	private final KeyZoom keyZoomOut;
	private final KeyZoom keyZoomOutFast;
	private final KeyZoom keyZoomOutSlow;

	private static final double[] speed = { 1.0, 10.0, 0.1 };

	/**
	 * Copy of transform when mouse dragging started.
	 */
	private final AffineTransform3D affineDragStart = new AffineTransform3D();

	/**
	 * Current transform during mouse dragging.
	 */
	private final AffineTransform3D affineDragCurrent = new AffineTransform3D();

	/**
	 * Coordinates where mouse dragging started.
	 */
	private double oX, oY;

	/**
	 * The screen size of the canvas (the component displaying the image and
	 * generating mouse events).
	 */
	private int canvasW = 1, canvasH = 1;

	/**
	 * Screen coordinates to keep centered while zooming or rotating with the
	 * keyboard. These are set to <em>(canvasW/2, canvasH/2)</em>
	 */
	private int centerX = 0, centerY = 0;

	private final TransformState transform;

	public TransformEventHandler2D( final TransformState transform )
	{
		this.transform = transform;

		dragTranslate = new DragTranslate();
		dragRotate = new DragRotate();

		scrollTranslate = new ScrollTranslate( speed[ 0 ] );
		scrollTranslateFast = new ScrollTranslate( speed[ 1 ] );
		scrollTranslateSlow = new ScrollTranslate( speed[ 2 ] );

		zoom = new Zoom( speed[ 0 ] );
		zoomFast = new Zoom( speed[ 1 ] );
		zoomSlow = new Zoom( speed[ 2 ] );

		scrollRotate = new ScrollRotate( 2 * speed[ 0 ] );
		scrollRotateFast = new ScrollRotate( 2 * speed[ 1 ] );
		scrollRotateSlow = new ScrollRotate( 2 * speed[ 2 ] );

		keyRotateLeft = new KeyRotate( speed[ 0 ] );
		keyRotateLeftFast = new KeyRotate( speed[ 1 ] );
		keyRotateLeftSlow = new KeyRotate( speed[ 2 ] );
		keyRotateRight = new KeyRotate( -speed[ 0 ] );
		keyRotateRightFast = new KeyRotate( -speed[ 1 ] );
		keyRotateRightSlow = new KeyRotate( -speed[ 2 ] );

		keyZoomIn = new KeyZoom( speed[ 0 ] );
		keyZoomInFast = new KeyZoom( speed[ 1 ] );
		keyZoomInSlow = new KeyZoom( speed[ 2 ] );
		keyZoomOut = new KeyZoom( -speed[ 0 ] );
		keyZoomOutFast = new KeyZoom( -speed[ 1 ] );
		keyZoomOutSlow = new KeyZoom( -speed[ 2 ] );
	}

	@Override
	public void install( final Behaviours behaviours )
	{
		behaviours.behaviour( dragTranslate, DRAG_TRANSLATE, DRAG_TRANSLATE_KEYS );
		behaviours.behaviour( dragRotate, DRAG_ROTATE,  DRAG_ROTATE_KEYS );

		behaviours.behaviour( scrollTranslate, SCROLL_TRANSLATE, SCROLL_TRANSLATE_KEYS );
		behaviours.behaviour( scrollTranslateFast, SCROLL_TRANSLATE_FAST, SCROLL_TRANSLATE_FAST_KEYS );
		behaviours.behaviour( scrollTranslateSlow, SCROLL_TRANSLATE_SLOW, SCROLL_TRANSLATE_SLOW_KEYS );

		behaviours.behaviour( zoom, ZOOM_NORMAL, ZOOM_NORMAL_KEYS );
		behaviours.behaviour( zoomFast, ZOOM_FAST, ZOOM_FAST_KEYS );
		behaviours.behaviour( zoomSlow, ZOOM_SLOW, ZOOM_SLOW_KEYS );

		behaviours.behaviour( scrollRotate, SCROLL_ROTATE, SCROLL_ROTATE_KEYS );
		behaviours.behaviour( scrollRotateFast, SCROLL_ROTATE_FAST, SCROLL_ROTATE_FAST_KEYS );
		behaviours.behaviour( scrollRotateSlow, SCROLL_ROTATE_SLOW, SCROLL_ROTATE_SLOW_KEYS );

		behaviours.behaviour( keyRotateLeft, ROTATE_LEFT, ROTATE_LEFT_KEYS );
		behaviours.behaviour( keyRotateLeftFast, ROTATE_LEFT_FAST, ROTATE_LEFT_FAST_KEYS );
		behaviours.behaviour( keyRotateLeftSlow, ROTATE_LEFT_SLOW, ROTATE_LEFT_SLOW_KEYS );
		behaviours.behaviour( keyRotateRight, ROTATE_RIGHT, ROTATE_RIGHT_KEYS );
		behaviours.behaviour( keyRotateRightFast, ROTATE_RIGHT_FAST, ROTATE_RIGHT_FAST_KEYS );
		behaviours.behaviour( keyRotateRightSlow, ROTATE_RIGHT_SLOW, ROTATE_RIGHT_SLOW_KEYS );

		behaviours.behaviour( keyZoomIn,  KEY_ZOOM_IN, KEY_ZOOM_IN_KEYS );
		behaviours.behaviour( keyZoomInFast, KEY_ZOOM_IN_FAST, KEY_ZOOM_IN_FAST_KEYS );
		behaviours.behaviour( keyZoomInSlow, KEY_ZOOM_IN_SLOW, KEY_ZOOM_IN_SLOW_KEYS );
		behaviours.behaviour( keyZoomOut, KEY_ZOOM_OUT, KEY_ZOOM_OUT_KEYS );
		behaviours.behaviour( keyZoomOutFast, KEY_ZOOM_OUT_FAST, KEY_ZOOM_OUT_FAST_KEYS );
		behaviours.behaviour( keyZoomOutSlow, KEY_ZOOM_OUT_SLOW, KEY_ZOOM_OUT_SLOW_KEYS );
	}

	@Override
	public void setCanvasSize( final int width, final int height, final boolean updateTransform )
	{
		if ( width == 0 || height == 0 ) {
			// NB: We are probably in some intermediate layout scenario.
			// Attempting to trigger a transform update with 0 size will result
			// in the exception "Matrix is singular" from imglib2-realtrasform.
			return;
		}
		if ( updateTransform )
		{
			final AffineTransform3D affine = transform.get();
			affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
			affine.scale( ( double ) width / canvasW );
			affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
			transform.set( affine );
		}
		canvasW = width;
		canvasH = height;
		centerX = width / 2;
		centerY = height / 2;
	}

	/**
	 * One step of rotation (radian).
	 */
	final private static double step = Math.PI / 180;

	private void scale( final double s, final double x, final double y )
	{
		final AffineTransform3D affine = transform.get();

		// center shift
		affine.set( affine.get( 0, 3 ) - x, 0, 3 );
		affine.set( affine.get( 1, 3 ) - y, 1, 3 );

		// scale
		affine.scale( s );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + x, 0, 3 );
		affine.set( affine.get( 1, 3 ) + y, 1, 3 );

		transform.set( affine );
	}

	/**
	 * Rotate by d radians around Z axis. Keep screen coordinates {@code (centerX, centerY)} fixed.
	 */
	private void rotate( final AffineTransform3D affine, final double d )
	{
		// center shift
		affine.set( affine.get( 0, 3 ) - centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) - centerY, 1, 3 );

		// rotate
		affine.rotate( 2, d );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) + centerY, 1, 3 );
	}

	private class DragRotate implements DragBehaviour
	{
		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			transform.get( affineDragStart );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final double dX = x - centerX;
			final double dY = y - centerY;
			final double odX = oX - centerX;
			final double odY = oY - centerY;
			final double theta = Math.atan2( dY, dX ) - Math.atan2( odY, odX );

			affineDragCurrent.set( affineDragStart );
			rotate( affineDragCurrent, theta );
			transform.set( affineDragCurrent );
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class ScrollRotate implements ScrollBehaviour
	{
		private final double speed;

		public ScrollRotate( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			final AffineTransform3D affine = transform.get();

			final double theta = speed * wheelRotation * Math.PI / 180.0;

			// center shift
			affine.set( affine.get( 0, 3 ) - x, 0, 3 );
			affine.set( affine.get( 1, 3 ) - y, 1, 3 );

			affine.rotate( 2, theta );

			// center un-shift
			affine.set( affine.get( 0, 3 ) + x, 0, 3 );
			affine.set( affine.get( 1, 3 ) + y, 1, 3 );

			transform.set( affine );
		}
	}

	private class DragTranslate implements DragBehaviour
	{
		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			transform.get( affineDragStart );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final double dX = oX - x;
			final double dY = oY - y;

			affineDragCurrent.set( affineDragStart );
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) - dX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) - dY, 1, 3 );

			transform.set( affineDragCurrent );
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class ScrollTranslate implements ScrollBehaviour
	{

		private final double speed;

		public ScrollTranslate( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			final AffineTransform3D affine = transform.get();

			final double d = -wheelRotation * 10 * speed;
			if ( isHorizontal )
				affine.translate( d, 0, 0 );
			else
				affine.translate( 0, d, 0 );

			transform.set( affine );
		}
	}

	private class Zoom implements ScrollBehaviour
	{

		private final double speed;

		public Zoom( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			final double s = speed * wheelRotation;
			final double dScale = 1.0 + 0.05 * Math.abs( s );
			if ( s > 0 )
				scale( 1.0 / dScale, x, y );
			else
				scale( dScale, x, y );
		}
	}

	private class KeyRotate implements ClickBehaviour
	{
		private final double speed;

		public KeyRotate( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void click( final int x, final int y )
		{
			final AffineTransform3D affine = transform.get();
			rotate( affine, step * speed );
			transform.set( affine );
		}
	}

	private class KeyZoom implements ClickBehaviour
	{
		private final double dScale;

		public KeyZoom( final double speed )
		{
			if ( speed > 0 )
				dScale = 1.0 + 0.1 * speed;
			else
				dScale = 1.0 / ( 1.0 - 0.1 * speed );
		}

		@Override
		public void click( final int x, final int y )
		{
			scale( dScale, centerX, centerY );
		}
	}
}
