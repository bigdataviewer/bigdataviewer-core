package bdv;

import bdv.behaviour.Behaviour;
import bdv.behaviour.BehaviourMap;
import bdv.behaviour.ClickBehaviour;
import bdv.behaviour.DragBehaviour;
import bdv.behaviour.InputTriggerAdder;
import bdv.behaviour.InputTriggerMap;
import bdv.behaviour.MouseAndKeyHandler;
import bdv.behaviour.ScrollBehaviour;
import bdv.behaviour.io.InputTriggerConfig;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

/**
 * A {@link TransformEventHandler} that changes an {@link AffineTransform3D}
 * through a set of {@link Behaviour}s.
 *
 * @author Stephan Saalfeld
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BehaviourTransformEventHandler3D extends MouseAndKeyHandler implements TransformEventHandler< AffineTransform3D >
{
	public static TransformEventHandlerFactory< AffineTransform3D > factory( final InputTriggerConfig config )
	{
		return new TransformEventHandlerFactory< AffineTransform3D >()
		{
			@Override
			public TransformEventHandler< AffineTransform3D > create( final TransformListener< AffineTransform3D > transformListener )
			{
				return new BehaviourTransformEventHandler3D( transformListener, config );
			}
		};
	}

	/**
	 * Current source to screen transform.
	 */
	final protected AffineTransform3D affine = new AffineTransform3D();

	/**
	 * Whom to notify when the {@link #affine current transform} is changed.
	 */
	protected TransformListener< AffineTransform3D > listener;

	protected final BehaviourMap behaviourMap;

	protected final InputTriggerMap inputMap;

	protected final InputTriggerAdder inputAdder;

	/**
	 * Copy of {@link #affine current transform} when mouse dragging started.
	 */
	final protected AffineTransform3D affineDragStart = new AffineTransform3D();

	/**
	 * Coordinates where mouse dragging started.
	 */
	protected double oX, oY;

	/**
	 * Current rotation axis for rotating with keyboard, indexed {@code x->0, y->1,
	 * z->2}.
	 */
	protected int axis = 0;

	/**
	 * The screen size of the canvas (the component displaying the image and
	 * generating mouse events).
	 */
	protected int canvasW = 1, canvasH = 1;

	/**
	 * Screen coordinates to keep centered while zooming or rotating with the
	 * keyboard. These are set to <em>(canvasW/2, canvasH/2)</em>
	 */
	protected int centerX = 0, centerY = 0;

	public BehaviourTransformEventHandler3D( final TransformListener< AffineTransform3D > listener, final InputTriggerConfig config )
	{
		this.listener = listener;
		final double normalSpeed = 1.0;
		final double fastSpeed = 10.0;
		final double slowSpeed = 0.1;

		final String DRAG_ROTATE_NORMAL = "drag rotate normal";
		final String DRAG_ROTATE_FAST = "drag rotate fast";
		final String DRAG_ROTATE_SLOW = "drag rotate slow";
		final String DRAG_TRANSLATE = "drag translate";
		final String SCROLL_Z_NORMAL = "scroll browse z normal";
		final String SCROLL_Z_FAST = "scroll browse z fast";
		final String SCROLL_Z_SLOW = "scroll browse z slow";
		final String ZOOM_NORMAL = "scroll zoom";
		final String SELECT_AXIS_X = "axis x";
		final String SELECT_AXIS_Y = "axis y";
		final String SELECT_AXIS_Z = "axis z";
		final String ROTATE_LEFT_NORMAL = "rotate left normal";
		final String ROTATE_LEFT_FAST = "rotate left fast";
		final String ROTATE_LEFT_SLOW = "rotate left slow";
		final String ROTATE_RIGHT_NORMAL = "rotate right normal";
		final String ROTATE_RIGHT_FAST = "rotate right fast";
		final String ROTATE_RIGHT_SLOW = "rotate right slow";
		final String KEY_ZOOM_IN_NORMAL = "zoom in normal";
		final String KEY_ZOOM_IN_FAST = "zoom in fast";
		final String KEY_ZOOM_IN_SLOW = "zoom in slow";
		final String KEY_ZOOM_OUT_NORMAL = "zoom out normal";
		final String KEY_ZOOM_OUT_FAST = "zoom out fast";
		final String KEY_ZOOM_OUT_SLOW = "zoom out slow";
		final String KEY_FORWARD_Z_NORMAL = "forward z normal";
		final String KEY_FORWARD_Z_FAST = "forward z fast";
		final String KEY_FORWARD_Z_SLOW = "forward z slow";
		final String KEY_BACKWARD_Z_NORMAL = "backward z normal";
		final String KEY_BACKWARD_Z_FAST = "backward z fast";
		final String KEY_BACKWARD_Z_SLOW = "backward z slow";

		behaviourMap = new BehaviourMap();
		inputMap = new InputTriggerMap();
		inputAdder = config.inputTriggerAdder( inputMap, "bdv" );

		new Rotate( normalSpeed, DRAG_ROTATE_NORMAL, "win button1" ).register();
		new Rotate( fastSpeed, DRAG_ROTATE_FAST, "shift button1" ).register();
		new Rotate( slowSpeed, DRAG_ROTATE_SLOW, "ctrl button1" ).register();
		new TranslateXY( DRAG_TRANSLATE, "button2", "button3" ).register();
		new TranslateZ( normalSpeed, SCROLL_Z_NORMAL, "scroll" ).register();
		new TranslateZ( fastSpeed, SCROLL_Z_FAST, "shift scroll" ).register();
		new TranslateZ( slowSpeed, SCROLL_Z_SLOW, "ctrl scroll" ).register();
		new Zoom( normalSpeed, ZOOM_NORMAL, "meta scroll", "ctrl shift scroll" ).register();
		new SelectRotationAxis( 0, SELECT_AXIS_X, "X" ).register();
		new SelectRotationAxis( 1, SELECT_AXIS_Y, "Y" ).register();
		new SelectRotationAxis( 2, SELECT_AXIS_Z, "Z" ).register();
		new KeyRotate( normalSpeed, ROTATE_LEFT_NORMAL, "LEFT" ).register();
		new KeyRotate( fastSpeed, ROTATE_LEFT_FAST, "shift LEFT" ).register();
		new KeyRotate( slowSpeed, ROTATE_LEFT_SLOW, "ctrl LEFT" ).register();
		new KeyRotate( -normalSpeed, ROTATE_RIGHT_NORMAL, "RIGHT" ).register();
		new KeyRotate( -fastSpeed, ROTATE_RIGHT_FAST, "shift RIGHT" ).register();
		new KeyRotate( -slowSpeed, ROTATE_RIGHT_SLOW, "ctrl RIGHT" ).register();
		new KeyZoom( normalSpeed, KEY_ZOOM_IN_NORMAL, "UP" ).register();
		new KeyZoom( fastSpeed, KEY_ZOOM_IN_FAST, "shift UP" ).register();
		new KeyZoom( slowSpeed, KEY_ZOOM_IN_SLOW, "ctrl UP" ).register();
		new KeyZoom( -normalSpeed, KEY_ZOOM_OUT_NORMAL, "DOWN" ).register();
		new KeyZoom( -fastSpeed, KEY_ZOOM_OUT_FAST, "shift DOWN" ).register();
		new KeyZoom( -slowSpeed, KEY_ZOOM_OUT_SLOW, "ctrl DOWN" ).register();
		new KeyTranslateZ( normalSpeed, KEY_FORWARD_Z_NORMAL, "COMMA" ).register();
		new KeyTranslateZ( fastSpeed, KEY_FORWARD_Z_FAST, "shift COMMA" ).register();
		new KeyTranslateZ( slowSpeed, KEY_FORWARD_Z_SLOW, "ctrl COMMA" ).register();
		new KeyTranslateZ( -normalSpeed, KEY_BACKWARD_Z_NORMAL, "PERIOD" ).register();
		new KeyTranslateZ( -fastSpeed, KEY_BACKWARD_Z_FAST, "shift PERIOD" ).register();
		new KeyTranslateZ( -slowSpeed, KEY_BACKWARD_Z_SLOW, "ctrl PERIOD" ).register();

		this.setBehaviourMap( behaviourMap );
		this.setInputMap( inputMap );
	}

	@Override
	public AffineTransform3D getTransform()
	{
		synchronized ( affine )
		{
			return affine.copy();
		}
	}

	@Override
	public void setTransform( final AffineTransform3D transform )
	{
		synchronized ( affine )
		{
			affine.set( transform );
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height, final boolean updateTransform )
	{
		if ( updateTransform )
		{
			synchronized ( affine )
			{
				affine.set( affine.get( 0, 3 ) - canvasW / 2, 0, 3 );
				affine.set( affine.get( 1, 3 ) - canvasH / 2, 1, 3 );
				affine.scale( ( double ) width / canvasW );
				affine.set( affine.get( 0, 3 ) + width / 2, 0, 3 );
				affine.set( affine.get( 1, 3 ) + height / 2, 1, 3 );
				notifyListener();
			}
		}
		canvasW = width;
		canvasH = height;
		centerX = width / 2;
		centerY = height / 2;
	}

	@Override
	public void setTransformListener( final TransformListener< AffineTransform3D > transformListener )
	{
		listener = transformListener;
	}

	@Override
	public String getHelpString()
	{
		return helpString;
	}

	/**
	 * notifies {@link #listener} that the current transform changed.
	 */
	private void notifyListener()
	{
		if ( listener != null )
			listener.transformChanged( affine );
	}

	/**
	 * One step of rotation (radian).
	 */
	final private static double step = Math.PI / 180;

	final private static String NL = System.getProperty( "line.separator" );

	final private static String helpString =
			"Mouse control:" + NL + " " + NL +
					"Pan and tilt the volume by left-click and dragging the image in the canvas, " + NL +
					"move the volume by middle-or-right-click and dragging the image in the canvas, " + NL +
					"browse alongside the z-axis using the mouse-wheel, and" + NL +
					"zoom in and out using the mouse-wheel holding CTRL+SHIFT or META." + NL + " " + NL +
					"Key control:" + NL + " " + NL +
					"X - Select x-axis as rotation axis." + NL +
					"Y - Select y-axis as rotation axis." + NL +
					"Z - Select z-axis as rotation axis." + NL +
					"CURSOR LEFT - Rotate clockwise around the choosen rotation axis." + NL +
					"CURSOR RIGHT - Rotate counter-clockwise around the choosen rotation axis." + NL +
					"CURSOR UP - Zoom in." + NL +
					"CURSOR DOWN - Zoom out." + NL +
					"./> - Forward alongside z-axis." + NL +
					",/< - Backward alongside z-axis." + NL +
					"SHIFT - Rotate and browse 10x faster." + NL +
					"CTRL - Rotate and browse 10x slower.";

	private void scale( final double s, final double x, final double y )
	{
		// center shift
		affine.set( affine.get( 0, 3 ) - x, 0, 3 );
		affine.set( affine.get( 1, 3 ) - y, 1, 3 );

		// scale
		affine.scale( s );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + x, 0, 3 );
		affine.set( affine.get( 1, 3 ) + y, 1, 3 );
	}

	/**
	 * Rotate by d radians around axis. Keep screen coordinates (
	 * {@link #centerX}, {@link #centerY}) fixed.
	 */
	private void rotate( final int axis, final double d )
	{
		// center shift
		affine.set( affine.get( 0, 3 ) - centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) - centerY, 1, 3 );

		// rotate
		affine.rotate( axis, d );

		// center un-shift
		affine.set( affine.get( 0, 3 ) + centerX, 0, 3 );
		affine.set( affine.get( 1, 3 ) + centerY, 1, 3 );

	}

	private abstract class SelfRegisteringBehaviour implements Behaviour
	{
		private final String name;

		private final String[] defaultTriggers;

		public SelfRegisteringBehaviour( final String name, final String ... defaultTriggers )
		{
			this.name = name;
			this.defaultTriggers = defaultTriggers;
		}

		public void register()
		{
			behaviourMap.put( name, this );
			inputAdder.put( name, defaultTriggers );
		}
	}

	private class Rotate extends SelfRegisteringBehaviour implements DragBehaviour
	{
		private final double speed;

		public Rotate( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.speed = speed;
		}

		@Override
		public void init( final int x, final int y )
		{
			synchronized ( affine )
			{
				oX = x;
				oY = y;
				affineDragStart.set( affine );
			}
		}

		@Override
		public void drag( final int x, final int y )
		{
			synchronized ( affine )
			{
				final double dX = oX - x;
				final double dY = oY - y;

				affine.set( affineDragStart );

				// center shift
				affine.set( affine.get( 0, 3 ) - oX, 0, 3 );
				affine.set( affine.get( 1, 3 ) - oY, 1, 3 );

				final double v = step * speed;
				affine.rotate( 0, -dY * v );
				affine.rotate( 1, dX * v );

				// center un-shift
				affine.set( affine.get( 0, 3 ) + oX, 0, 3 );
				affine.set( affine.get( 1, 3 ) + oY, 1, 3 );
				notifyListener();
			}
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class TranslateXY extends SelfRegisteringBehaviour implements DragBehaviour
	{
		public TranslateXY( final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
		}

		@Override
		public void init( final int x, final int y )
		{
			synchronized ( affine )
			{
				oX = x;
				oY = y;
				affineDragStart.set( affine );
			}
		}

		@Override
		public void drag( final int x, final int y )
		{
			synchronized ( affine )
			{
				final double dX = oX - x;
				final double dY = oY - y;

				affine.set( affineDragStart );
				affine.set( affine.get( 0, 3 ) - dX, 0, 3 );
				affine.set( affine.get( 1, 3 ) - dY, 1, 3 );
				notifyListener();
			}
		}

		@Override
		public void end( final int x, final int y )
		{}
	}

	private class TranslateZ extends SelfRegisteringBehaviour implements ScrollBehaviour
	{
		private final double speed;

		public TranslateZ( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			synchronized ( affine )
			{
				final double dZ = speed * -wheelRotation;
				// TODO (optionally) correct for zoom
				affine.set( affine.get( 2, 3 ) - dZ, 2, 3 );
				notifyListener();
			}
		}
	}

	private class Zoom extends SelfRegisteringBehaviour implements ScrollBehaviour
	{
		private final double speed;

		public Zoom( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.speed = speed;
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			synchronized ( affine )
			{
				final double s = speed * wheelRotation;
				final double dScale = 1.0 + 0.05;
				if ( s > 0 )
					scale( 1.0 / dScale, x, y );
				else
					scale( dScale, x, y );
				notifyListener();
			}
		}
	}

	private class SelectRotationAxis extends SelfRegisteringBehaviour implements ClickBehaviour
	{
		private final int axis;

		public SelectRotationAxis( final int axis, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.axis = axis;
		}

		@Override
		public void click( final int x, final int y )
		{
			BehaviourTransformEventHandler3D.this.axis = axis;
		}
	}

	private class KeyRotate extends SelfRegisteringBehaviour implements ClickBehaviour
	{
		private final double velocity;

		public KeyRotate( final double velocity, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.velocity = velocity;
		}

		@Override
		public void click( final int x, final int y )
		{
			synchronized ( affine )
			{
				rotate( axis, step * velocity );
				notifyListener();
			}
		}
	}

	private class KeyZoom extends SelfRegisteringBehaviour implements ClickBehaviour
	{
		private final double dScale;

		public KeyZoom( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			if ( speed > 0 )
				dScale = 1.0 + 0.1 * speed;
			else
				dScale = 1.0 / ( 1.0 - 0.1 * speed );
		}

		@Override
		public void click( final int x, final int y )
		{
			synchronized ( affine )
			{
				scale( dScale, centerX, centerY );
				notifyListener();
			}
		}
	}

	private class KeyTranslateZ extends SelfRegisteringBehaviour implements ClickBehaviour
	{
		private final double speed;

		public KeyTranslateZ( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.speed = speed;
		}

		@Override
		public void click( final int x, final int y )
		{
			synchronized ( affine )
			{
				affine.set( affine.get( 2, 3 ) + speed, 2, 3 );
				notifyListener();
			}
		}
	}
}
