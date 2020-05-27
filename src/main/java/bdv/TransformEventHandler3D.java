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

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformListener;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * A {@link TransformEventHandler} that changes an {@link AffineTransform3D}
 * through a set of {@link Behaviour}s.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch
 */
public class TransformEventHandler3D implements TransformEventHandler< AffineTransform3D >
{
	// -- behaviour names --

	public static final String DRAG_TRANSLATE = "drag translate";
	public static final String ZOOM_NORMAL = "scroll zoom";
	public static final String SELECT_AXIS_X = "axis x";
	public static final String SELECT_AXIS_Y = "axis y";
	public static final String SELECT_AXIS_Z = "axis z";

	public static final String DRAG_ROTATE = "drag rotate";
	public static final String SCROLL_Z = "scroll browse z";
	public static final String ROTATE_LEFT = "rotate left";
	public static final String ROTATE_RIGHT = "rotate right";
	public static final String KEY_ZOOM_IN = "zoom in";
	public static final String KEY_ZOOM_OUT = "zoom out";
	public static final String KEY_FORWARD_Z = "forward z";
	public static final String KEY_BACKWARD_Z = "backward z";

	public static final String DRAG_ROTATE_FAST = "drag rotate fast";
	public static final String SCROLL_Z_FAST = "scroll browse z fast";
	public static final String ROTATE_LEFT_FAST = "rotate left fast";
	public static final String ROTATE_RIGHT_FAST = "rotate right fast";
	public static final String KEY_ZOOM_IN_FAST = "zoom in fast";
	public static final String KEY_ZOOM_OUT_FAST = "zoom out fast";
	public static final String KEY_FORWARD_Z_FAST = "forward z fast";
	public static final String KEY_BACKWARD_Z_FAST = "backward z fast";

	public static final String DRAG_ROTATE_SLOW = "drag rotate slow";
	public static final String SCROLL_Z_SLOW = "scroll browse z slow";
	public static final String ROTATE_LEFT_SLOW = "rotate left slow";
	public static final String ROTATE_RIGHT_SLOW = "rotate right slow";
	public static final String KEY_ZOOM_IN_SLOW = "zoom in slow";
	public static final String KEY_ZOOM_OUT_SLOW = "zoom out slow";
	public static final String KEY_FORWARD_Z_SLOW = "forward z slow";
	public static final String KEY_BACKWARD_Z_SLOW = "backward z slow";

	// -- default shortcuts --

	private static final String[] DRAG_TRANSLATE_KEYS = new String[] { "button2", "button3" };
	private static final String[] ZOOM_NORMAL_KEYS = new String[] { "meta scroll", "ctrl shift scroll" };
	private static final String[] SELECT_AXIS_X_KEYS = new String[] { "X" };
	private static final String[] SELECT_AXIS_Y_KEYS = new String[] { "Y" };
	private static final String[] SELECT_AXIS_Z_KEYS = new String[] { "Z" };

	private static final String[] DRAG_ROTATE_KEYS = new String[] { "button1" };
	private static final String[] SCROLL_Z_KEYS = new String[] { "scroll" };
	private static final String[] ROTATE_LEFT_KEYS = new String[] { "LEFT" };
	private static final String[] ROTATE_RIGHT_KEYS = new String[] { "RIGHT" };
	private static final String[] KEY_ZOOM_IN_KEYS = new String[] { "UP" };
	private static final String[] KEY_ZOOM_OUT_KEYS = new String[] { "DOWN" };
	private static final String[] KEY_FORWARD_Z_KEYS = new String[] { "COMMA" };
	private static final String[] KEY_BACKWARD_Z_KEYS = new String[] { "PERIOD" };

	private static final String[] DRAG_ROTATE_FAST_KEYS = new String[] { "shift button1" };
	private static final String[] SCROLL_Z_FAST_KEYS = new String[] { "shift scroll" };
	private static final String[] ROTATE_LEFT_FAST_KEYS = new String[] { "shift LEFT" };
	private static final String[] ROTATE_RIGHT_FAST_KEYS = new String[] { "shift RIGHT" };
	private static final String[] KEY_ZOOM_IN_FAST_KEYS = new String[] { "shift UP" };
	private static final String[] KEY_ZOOM_OUT_FAST_KEYS = new String[] { "shift DOWN" };
	private static final String[] KEY_FORWARD_Z_FAST_KEYS = new String[] { "shift COMMA" };
	private static final String[] KEY_BACKWARD_Z_FAST_KEYS = new String[] { "shift PERIOD" };

	private static final String[] DRAG_ROTATE_SLOW_KEYS = new String[] { "ctrl button1" };
	private static final String[] SCROLL_Z_SLOW_KEYS = new String[] { "ctrl scroll" };
	private static final String[] ROTATE_LEFT_SLOW_KEYS = new String[] { "ctrl LEFT" };
	private static final String[] ROTATE_RIGHT_SLOW_KEYS = new String[] { "ctrl RIGHT" };
	private static final String[] KEY_ZOOM_IN_SLOW_KEYS = new String[] { "ctrl UP" };
	private static final String[] KEY_ZOOM_OUT_SLOW_KEYS = new String[] { "ctrl DOWN" };
	private static final String[] KEY_FORWARD_Z_SLOW_KEYS = new String[] { "ctrl COMMA" };
	private static final String[] KEY_BACKWARD_Z_SLOW_KEYS = new String[] { "ctrl PERIOD" };

	// -- behaviours --

	private final TranslateXY dragTranslate;
	private final Zoom zoom;
	private final SelectRotationAxis selectRotationAxisX;
	private final SelectRotationAxis selectRotationAxisY;
	private final SelectRotationAxis selectRotationAxisZ;
	private final Rotate dragRotate;
	private final Rotate dragRotateFast;
	private final Rotate dragRotateSlow;
	private final TranslateZ translateZ;
	private final TranslateZ translateZFast;
	private final TranslateZ translateZSlow;
	private final KeyRotate rotateLeft;
	private final KeyRotate rotateLeftFast;
	private final KeyRotate rotateLeftSlow;
	private final KeyRotate rotateRight;
	private final KeyRotate rotateRightFast;
	private final KeyRotate rotateRightSlow;
	private final KeyZoom keyZoomIn;
	private final KeyZoom keyZoomInFast;
	private final KeyZoom keyZoomInSlow;
	private final KeyZoom keyZoomOut;
	private final KeyZoom keyZoomOutFast;
	private final KeyZoom keyZoomOutSlow;
	private final KeyTranslateZ keyForwardZ;
	private final KeyTranslateZ keyForwardZFast;
	private final KeyTranslateZ keyForwardZSlow;
	private final KeyTranslateZ keyBackwardZ;
	private final KeyTranslateZ keyBackwardZFast;
	private final KeyTranslateZ keyBackwardZSlow;

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
	 * Current rotation axis for rotating with keyboard, indexed {@code x->0, y->1,
	 * z->2}.
	 */
	private int axis = 0;

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

	public TransformEventHandler3D()
	{
		dragTranslate = new TranslateXY();
		zoom = new Zoom();
		selectRotationAxisX = new SelectRotationAxis( 0 );
		selectRotationAxisY = new SelectRotationAxis( 1 );
		selectRotationAxisZ = new SelectRotationAxis( 2 );

		dragRotate = new Rotate( speed[ 0 ] );
		dragRotateFast = new Rotate( speed[ 1 ] );
		dragRotateSlow = new Rotate( speed[ 2 ] );

		translateZ = new TranslateZ( speed[ 0 ] );
		translateZFast = new TranslateZ( speed[ 1 ] );
		translateZSlow = new TranslateZ( speed[ 2 ] );

		rotateLeft = new KeyRotate( speed[ 0 ] );
		rotateLeftFast = new KeyRotate( speed[ 1 ] );
		rotateLeftSlow = new KeyRotate( speed[ 2 ] );
		rotateRight = new KeyRotate( -speed[ 0 ] );
		rotateRightFast = new KeyRotate( -speed[ 1 ] );
		rotateRightSlow = new KeyRotate( -speed[ 2 ] );

		keyZoomIn = new KeyZoom( speed[ 0 ] );
		keyZoomInFast = new KeyZoom( speed[ 1 ] );
		keyZoomInSlow = new KeyZoom( speed[ 2 ] );
		keyZoomOut = new KeyZoom( -speed[ 0 ] );
		keyZoomOutFast = new KeyZoom( -speed[ 1 ] );
		keyZoomOutSlow = new KeyZoom( -speed[ 2 ] );

		keyForwardZ = new KeyTranslateZ( speed[ 0 ] );
		keyForwardZFast = new KeyTranslateZ( speed[ 1 ] );
		keyForwardZSlow = new KeyTranslateZ( speed[ 2 ] );
		keyBackwardZ = new KeyTranslateZ( -speed[ 0 ] );
		keyBackwardZFast = new KeyTranslateZ( -speed[ 1 ] );
		keyBackwardZSlow = new KeyTranslateZ( -speed[ 2 ] );
	}

	@Override
	public void install( final Behaviours behaviours )
	{
		behaviours.behaviour( dragTranslate, DRAG_TRANSLATE, DRAG_TRANSLATE_KEYS );
		behaviours.behaviour( zoom, ZOOM_NORMAL, ZOOM_NORMAL_KEYS );

		behaviours.behaviour( selectRotationAxisX, SELECT_AXIS_X, SELECT_AXIS_X_KEYS );
		behaviours.behaviour( selectRotationAxisY, SELECT_AXIS_Y, SELECT_AXIS_Y_KEYS );
		behaviours.behaviour( selectRotationAxisZ, SELECT_AXIS_Z, SELECT_AXIS_Z_KEYS );

		behaviours.behaviour( dragRotate, DRAG_ROTATE, DRAG_ROTATE_KEYS );
		behaviours.behaviour( dragRotateFast, DRAG_ROTATE_FAST, DRAG_ROTATE_FAST_KEYS );
		behaviours.behaviour( dragRotateSlow, DRAG_ROTATE_SLOW, DRAG_ROTATE_SLOW_KEYS );

		behaviours.behaviour( translateZ, SCROLL_Z, SCROLL_Z_KEYS );
		behaviours.behaviour( translateZFast, SCROLL_Z_FAST, SCROLL_Z_FAST_KEYS );
		behaviours.behaviour( translateZSlow, SCROLL_Z_SLOW, SCROLL_Z_SLOW_KEYS );

		behaviours.behaviour( rotateLeft, ROTATE_LEFT, ROTATE_LEFT_KEYS );
		behaviours.behaviour( rotateLeftFast, ROTATE_LEFT_FAST, ROTATE_LEFT_FAST_KEYS );
		behaviours.behaviour( rotateLeftSlow, ROTATE_LEFT_SLOW, ROTATE_LEFT_SLOW_KEYS );
		behaviours.behaviour( rotateRight, ROTATE_RIGHT, ROTATE_RIGHT_KEYS );
		behaviours.behaviour( rotateRightFast, ROTATE_RIGHT_FAST, ROTATE_RIGHT_FAST_KEYS );
		behaviours.behaviour( rotateRightSlow, ROTATE_RIGHT_SLOW, ROTATE_RIGHT_SLOW_KEYS );

		behaviours.behaviour( keyZoomIn, KEY_ZOOM_IN, KEY_ZOOM_IN_KEYS );
		behaviours.behaviour( keyZoomInFast, KEY_ZOOM_IN_FAST, KEY_ZOOM_IN_FAST_KEYS );
		behaviours.behaviour( keyZoomInSlow, KEY_ZOOM_IN_SLOW, KEY_ZOOM_IN_SLOW_KEYS );
		behaviours.behaviour( keyZoomOut, KEY_ZOOM_OUT, KEY_ZOOM_OUT_KEYS );
		behaviours.behaviour( keyZoomOutFast, KEY_ZOOM_OUT_FAST, KEY_ZOOM_OUT_FAST_KEYS );
		behaviours.behaviour( keyZoomOutSlow, KEY_ZOOM_OUT_SLOW, KEY_ZOOM_OUT_SLOW_KEYS );

		behaviours.behaviour( keyForwardZ, KEY_FORWARD_Z, KEY_FORWARD_Z_KEYS );
		behaviours.behaviour( keyForwardZFast, KEY_FORWARD_Z_SLOW, KEY_FORWARD_Z_SLOW_KEYS );
		behaviours.behaviour( keyForwardZSlow, KEY_FORWARD_Z_FAST, KEY_FORWARD_Z_FAST_KEYS );
		behaviours.behaviour( keyBackwardZ, KEY_BACKWARD_Z, KEY_BACKWARD_Z_KEYS );
		behaviours.behaviour( keyBackwardZFast, KEY_BACKWARD_Z_FAST, KEY_BACKWARD_Z_FAST_KEYS );
		behaviours.behaviour( keyBackwardZSlow, KEY_BACKWARD_Z_SLOW, KEY_BACKWARD_Z_SLOW_KEYS );
	}

	private Transform< AffineTransform3D > transformStore;

	@Override
	public void setTransformStore( final Transform< AffineTransform3D > store )
	{
		transformStore = store;
	}

	@Override
	public void setTransformStore( final Supplier< AffineTransform3D > get, final Consumer< AffineTransform3D > set )
	{
		transformStore = new Transform< AffineTransform3D >()
		{
			@Override
			public AffineTransform3D getTransform()
			{
				return get.get();
			}

			@Override
			public void setTransform( final AffineTransform3D transform )
			{
				set.accept( transform );
			}
		};
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
			final AffineTransform3D affine = transformStore.getTransform();
			affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
			affine.scale( ( double ) width / canvasW );
			affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
			affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
			transformStore.setTransform( affine );
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
		final AffineTransform3D affine = transformStore.getTransform();

		// center shift
		affine.set( affine.get( 0, 3 ) - x, 0, 3 );
		affine.set( affine.get( 1, 3 ) - y, 1, 3 );

		// scale
		affine.scale( s );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + x, 0, 3 );
		affine.set( affine.get( 1, 3 ) + y, 1, 3 );

		transformStore.setTransform( affine );
	}

	/**
	 * Rotate by d radians around axis. Keep screen coordinates (
	 * {@link #centerX}, {@link #centerY}) fixed.
	 */
	private void rotate( final int axis, final double d )
	{
		final AffineTransform3D affine = transformStore.getTransform();

		// center shift
		affine.set( affine.get( 0, 3 ) - centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) - centerY, 1, 3 );

		// rotate
		affine.rotate( axis, d );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) + centerY, 1, 3 );

		transformStore.setTransform( affine );
	}

	private class Rotate implements DragBehaviour
	{
		private final double speed;

		public Rotate( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			affineDragStart.set( transformStore.getTransform() );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final double dX = oX - x;
			final double dY = oY - y;

			affineDragCurrent.set( affineDragStart );

			// center shift
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) - oX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) - oY, 1, 3 );

			final double v = step * speed;
			affineDragCurrent.rotate( 0, -dY * v );
			affineDragCurrent.rotate( 1, dX * v );

			// center un-shift
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) + oX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) + oY, 1, 3 );

			transformStore.setTransform( affineDragCurrent );
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class TranslateXY implements DragBehaviour
	{
		@Override
		public void init( final int x, final int y )
		{
			oX = x;
			oY = y;
			affineDragStart.set( transformStore.getTransform() );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final double dX = oX - x;
			final double dY = oY - y;

			affineDragCurrent.set( affineDragStart );
			affineDragCurrent.set( affineDragCurrent.get( 0, 3 ) - dX, 0, 3 );
			affineDragCurrent.set( affineDragCurrent.get( 1, 3 ) - dY, 1, 3 );

			transformStore.setTransform( affineDragCurrent );
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class TranslateZ implements ScrollBehaviour
	{
		private final double speed;

		public TranslateZ( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			final AffineTransform3D affine = transformStore.getTransform();

			final double dZ = speed * -wheelRotation;
			// TODO (optionally) correct for zoom
			affine.set( affine.get( 2, 3 ) - dZ, 2, 3 );

			transformStore.setTransform( affine );
		}
	}

	private class Zoom implements ScrollBehaviour
	{
		private final double speed = 1.0;

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			final double s = speed * wheelRotation;
			final double dScale = 1.0 + 0.05;
			if ( s > 0 )
				scale( 1.0 / dScale, x, y );
			else
				scale( dScale, x, y );
		}
	}

	private class SelectRotationAxis implements ClickBehaviour
	{
		private final int axis;

		public SelectRotationAxis( final int axis )
		{
			this.axis = axis;
		}

		@Override
		public void click( final int x, final int y )
		{
			TransformEventHandler3D.this.axis = axis;
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
			rotate( axis, step * speed );
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

	private class KeyTranslateZ implements ClickBehaviour
	{
		private final double speed;

		public KeyTranslateZ( final double speed )
		{
			this.speed = speed;
		}

		@Override
		public void click( final int x, final int y )
		{
			final AffineTransform3D affine = transformStore.getTransform();
			affine.set( affine.get( 2, 3 ) + speed, 2, 3 );
			transformStore.setTransform( affine );
		}
	}
}
