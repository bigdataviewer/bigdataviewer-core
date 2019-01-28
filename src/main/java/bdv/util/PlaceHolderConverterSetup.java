package bdv.util;

import java.util.ArrayList;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.RequestRepaint;
import net.imglib2.type.numeric.ARGBType;

public final class PlaceHolderConverterSetup implements ConverterSetup
{
	private final int setupId;

	private double min;

	private double max;

	private final ARGBType color;

	private final boolean supportsColor;

	private RequestRepaint viewer;

	private final ArrayList< SetupChangeListener > listeners;

	public interface SetupChangeListener
	{
		void setupParametersChanged();
	}

	public PlaceHolderConverterSetup(
			final int setupId,
			final double min,
			final double max,
			final int rgb )
	{
		this( setupId, min, max, new ARGBType( rgb ) );
	}

	public PlaceHolderConverterSetup(
			final int setupId,
			final double min,
			final double max,
			final ARGBType color )
	{
		this.setupId = setupId;
		this.min = min;
		this.max = max;
		this.color = new ARGBType();
		if ( color != null )
			this.color.set( color );
		this.supportsColor = color != null;
		this.viewer = null;
		this.listeners = new ArrayList<>();
	}

	@Override
	public int getSetupId()
	{
		return setupId;
	}

	@Override
	public void setDisplayRange( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		synchronized ( listeners )
		{
			for ( final SetupChangeListener l : listeners )
				l.setupParametersChanged();
		}
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public void setColor( final ARGBType color )
	{
		setColor( color.get() );
	}

	public void setColor( final int rgb )
	{
		this.color.set( rgb );
		synchronized ( listeners )
		{
			for ( final SetupChangeListener l : listeners )
				l.setupParametersChanged();
		}
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public boolean supportsColor()
	{
		return supportsColor;
	}

	@Override
	public double getDisplayRangeMin()
	{
		return min;
	}

	@Override
	public double getDisplayRangeMax()
	{
		return max;
	}

	@Override
	public ARGBType getColor()
	{
		return color;
	}

	@Override
	public void setViewer( final RequestRepaint viewer )
	{
		this.viewer = viewer;
	}

	/**
	 * Registers a SetupChangeListener, that will be notified when the display
	 * range or the color of this {@link ConverterSetup} changes.
	 *
	 * @param listener
	 *            the listener to register.
	 * @return {@code true} if the listener was successfully registered.
	 *         {@code false} if it was already registered.
	 */
	public boolean addSetupChangeListener( final SetupChangeListener listener )
	{
		synchronized( listeners )
		{
			if ( !listeners.contains( listener ) )
			{
				listeners.add( listener );
				return true;
			}
			return false;
		}
	}

	/**
	 * Removes the specified listener.
	 *
	 * @param listener
	 *            the listener to remove.
	 * @return {@code true} if the listener was present in the listeners of
	 *         this model and was successfully removed.
	 */
	public boolean removeSetupChangeListener( final SetupChangeListener listener )
	{
		synchronized( listeners )
		{
			return listeners.remove( listener );
		}
	}
}
