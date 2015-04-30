package bdv.cl;

import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

/**
 * A {@link TransformEventHandler} that changes an {@link AffineTransform2D}
 * when the canvas size changes.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class FixedTransformEventHandler2D implements TransformEventHandler< AffineTransform2D >
{
	final static private TransformEventHandlerFactory< AffineTransform2D > factory = new TransformEventHandlerFactory< AffineTransform2D >()
	{
		@Override
		public TransformEventHandler< AffineTransform2D > create( final TransformListener< AffineTransform2D > transformListener )
		{
			return new FixedTransformEventHandler2D( transformListener );
		}
	};

	public static TransformEventHandlerFactory< AffineTransform2D > factory()
	{
		return factory;
	}

	/**
	 * Current source to screen transform.
	 */
	final private AffineTransform2D affine = new AffineTransform2D();

	/**
	 * Whom to notify when the {@link #affine current transform} is changed.
	 */
	private TransformListener< AffineTransform2D > listener;

	/**
	 * The screen size of the canvas (the component displaying the image and
	 * generating mouse events).
	 */
	private int canvasW = 1, canvasH = 1;

	public FixedTransformEventHandler2D( final TransformListener< AffineTransform2D > listener )
	{
		this.listener = listener;
	}

	@Override
	public AffineTransform2D getTransform()
	{
		synchronized ( affine )
		{
			return affine.copy();
		}
	}

	@Override
	public void setTransform( final AffineTransform2D transform )
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
				affine.set( affine.get( 0, 2 ) - canvasW / 2, 0, 2 );
				affine.set( affine.get( 1, 2 ) - canvasH / 2, 1, 2 );
				affine.scale( ( double ) width / canvasW );
				affine.set( affine.get( 0, 2 ) + width / 2, 0, 2 );
				affine.set( affine.get( 1, 2 ) + height / 2, 1, 2 );
				update();
			}
		}
		canvasW = width;
		canvasH = height;
	}

	@Override
	public void setTransformListener( final TransformListener< AffineTransform2D > transformListener )
	{
		listener = transformListener;
	}

	@Override
	public String getHelpString()
	{
		return "";
	}

	/**
	 * notifies {@link #listener} that the current transform changed.
	 */
	private void update()
	{
		if ( listener != null )
			listener.transformChanged( affine );
	}
}
