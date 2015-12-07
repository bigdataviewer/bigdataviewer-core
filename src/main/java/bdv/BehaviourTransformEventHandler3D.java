package bdv;

import bdv.behaviour.Behaviour;
import bdv.behaviour.BehaviourMap;
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

		final BehaviourMap behaviourMap = new BehaviourMap();
		behaviourMap.put( DRAG_ROTATE_NORMAL, new Rotate( normalSpeed ) );
		behaviourMap.put( DRAG_ROTATE_FAST, new Rotate( fastSpeed ) );
		behaviourMap.put( DRAG_ROTATE_SLOW, new Rotate( slowSpeed ) );
		behaviourMap.put( DRAG_TRANSLATE, new TranslateXY() );
		behaviourMap.put( SCROLL_Z_NORMAL, new TranslateZ( normalSpeed ) );
		behaviourMap.put( SCROLL_Z_FAST, new TranslateZ( fastSpeed ) );
		behaviourMap.put( SCROLL_Z_SLOW, new TranslateZ( slowSpeed ) );
		behaviourMap.put( ZOOM_NORMAL, new Zoom( normalSpeed ) );

		final InputTriggerMap inputMap = new InputTriggerMap();
		final InputTriggerAdder map = config.inputTriggerAdder( inputMap, "bdv" );
		map.put( DRAG_ROTATE_NORMAL, "button1" );
		map.put( DRAG_ROTATE_FAST, "shift button1" );
		map.put( DRAG_ROTATE_SLOW, "ctrl button1" );
		map.put( DRAG_TRANSLATE, "button2", "button3" );
		map.put( SCROLL_Z_NORMAL, "scroll" );
		map.put( SCROLL_Z_FAST, "shift scroll" );
		map.put( SCROLL_Z_SLOW, "ctrl scroll" );
		map.put( ZOOM_NORMAL, "meta scroll", "ctrl shift scroll" );

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

	private class TranslateXY implements DragBehaviour
	{
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
			synchronized ( affine )
			{
				final double dZ = speed * -wheelRotation;
				// TODO (optionally) correct for zoom
				affine.set( affine.get( 2, 3 ) - dZ, 2, 3 );
				notifyListener();
			}
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
}
