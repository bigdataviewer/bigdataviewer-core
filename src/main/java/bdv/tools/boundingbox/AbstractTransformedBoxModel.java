package bdv.tools.boundingbox;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

/**
 * A transformed box that can be modified and notifies listeners about changes.
 * Represented as an interval (defined in subclasses) that is placed into
 * global coordinate system by an {@code AffineTransform3D}.
 */
public abstract class AbstractTransformedBoxModel implements TransformedBox
{
	public interface IntervalChangedListener
	{
		void intervalChanged();
	}

	private final AffineTransform3D transform;

	private final Listeners.List< IntervalChangedListener > listeners;

	public AbstractTransformedBoxModel( final AffineTransform3D transform )
	{
		this.transform = transform;
		listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public void getTransform( final AffineTransform3D t )
	{
		t.set( transform );
	}

	public Listeners< IntervalChangedListener > intervalChangedListeners()
	{
		return listeners;
	}

	public abstract void setInterval( RealInterval interval );

	protected void notifyIntervalChanged()
	{
		listeners.list.forEach( IntervalChangedListener::intervalChanged );
	}
}
