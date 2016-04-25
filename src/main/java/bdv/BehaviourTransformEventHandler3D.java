package bdv;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.InputTriggerAdder;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.TriggerBehaviourBindings;
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
public class BehaviourTransformEventHandler3D implements BehaviourTransformEventHandler< AffineTransform3D >
{
	public static TransformEventHandlerFactory< AffineTransform3D > factory()
	{
		return new BehaviourTransformEventHandler3DFactory();
	}

	public static class BehaviourTransformEventHandler3DFactory implements BehaviourTransformEventHandlerFactory< AffineTransform3D >
	{
		private InputTriggerConfig config = new InputTriggerConfig();

		@Override
		public void setConfig( final InputTriggerConfig config )
		{
			this.config = config;
		}

		@Override
		public BehaviourTransformEventHandler3D create( final TransformListener< AffineTransform3D > transformListener )
		{
			return new BehaviourTransformEventHandler3D( transformListener, config );
		}
	}

	/**
	 * Current source to screen transform.
	 */
	private final AffineTransform3D affine = new AffineTransform3D();

	/**
	 * Whom to notify when the {@link #affine current transform} is changed.
	 */
	private TransformListener< AffineTransform3D > listener;

	private final BehaviourMap behaviourMap = new BehaviourMap();

	private final InputTriggerMap inputMap = new InputTriggerMap();

	private final InputTriggerAdder inputAdder;

	/**
	 * Copy of {@link #affine current transform} when mouse dragging started.
	 */
	final private AffineTransform3D affineDragStart = new AffineTransform3D();

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

	public BehaviourTransformEventHandler3D( final TransformListener< AffineTransform3D > listener, final InputTriggerConfig config )
	{
		this.listener = listener;

		final String DRAG_TRANSLATE = "drag translate";
		final String ZOOM_NORMAL = "scroll zoom";
		final String SELECT_AXIS_X = "axis x";
		final String SELECT_AXIS_Y = "axis y";
		final String SELECT_AXIS_Z = "axis z";

		final double[] speed =      { 1.0,     10.0,     0.1 };
		final String[] SPEED_NAME = {  "",  " fast", " slow" };
		final String[] speedMod =   {  "", "shift ", "ctrl " };

		final String DRAG_ROTATE = "drag rotate";
		final String SCROLL_Z = "scroll browse z";
		final String ROTATE_LEFT = "rotate left";
		final String ROTATE_RIGHT = "rotate right";
		final String KEY_ZOOM_IN = "zoom in";
		final String KEY_ZOOM_OUT = "zoom out";
		final String KEY_FORWARD_Z = "forward z";
		final String KEY_BACKWARD_Z = "backward z";

		inputAdder = config.inputTriggerAdder( inputMap, "bdv" );

		new TranslateXY( DRAG_TRANSLATE, "button2", "button3" ).register();
		new Zoom( speed[ 0 ], ZOOM_NORMAL, "meta scroll", "ctrl shift scroll" ).register();
		new SelectRotationAxis( 0, SELECT_AXIS_X, "X" ).register();
		new SelectRotationAxis( 1, SELECT_AXIS_Y, "Y" ).register();
		new SelectRotationAxis( 2, SELECT_AXIS_Z, "Z" ).register();

		for ( int s = 0; s < 3; ++s )
		{
			new Rotate( speed[ s ], DRAG_ROTATE + SPEED_NAME[ s ], speedMod[ s ] + "button1" ).register();
			new TranslateZ( speed[ s ], SCROLL_Z + SPEED_NAME[ s ], speedMod[ s ] + "scroll" ).register();
			new KeyRotate( speed[ s ], ROTATE_LEFT + SPEED_NAME[ s ], speedMod[ s ] + "LEFT" ).register();
			new KeyRotate( -speed[ s ], ROTATE_RIGHT + SPEED_NAME[ s ], speedMod[ s ] + "RIGHT" ).register();
			new KeyZoom( speed[ s ], KEY_ZOOM_IN + SPEED_NAME[ s ], speedMod[ s ] + "UP" ).register();
			new KeyZoom( -speed[ s ], KEY_ZOOM_OUT + SPEED_NAME[ s ], speedMod[ s ] + "DOWN" ).register();
			new KeyTranslateZ( speed[ s ], KEY_FORWARD_Z + SPEED_NAME[ s ], speedMod[ s ] + "COMMA" ).register();
			new KeyTranslateZ( -speed[ s ], KEY_BACKWARD_Z + SPEED_NAME[ s ], speedMod[ s ] + "PERIOD" ).register();
		}
	}

	@Override
	public void install( final TriggerBehaviourBindings bindings )
	{
		bindings.addBehaviourMap( "transform", behaviourMap );
		bindings.addInputTriggerMap( "transform", inputMap );
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
		private final double speed;

		public KeyRotate( final double speed, final String name, final String ... defaultTriggers )
		{
			super( name, defaultTriggers );
			this.speed = speed;
		}

		@Override
		public void click( final int x, final int y )
		{
			synchronized ( affine )
			{
				rotate( axis, step * speed );
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
