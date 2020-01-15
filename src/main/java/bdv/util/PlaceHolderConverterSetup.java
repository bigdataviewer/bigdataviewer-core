package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;

public final class PlaceHolderConverterSetup implements ConverterSetup
{
	private final int setupId;

	private double min;

	private double max;

	private final ARGBType color;

	private final boolean supportsColor;

	private final Listeners.List< SetupChangeListener > listeners;

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
		this.listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public Listeners< SetupChangeListener > setupChangeListeners()
	{
		return listeners;
	}

	@Override
	public int getSetupId()
	{
		return setupId;
	}

	@Override
	public void setDisplayRange( final double min, final double max )
	{
		if ( this.min == min && this.max == max )
			return;

		this.min = min;
		this.max = max;
		listeners.list.forEach( l -> l.setupParametersChanged( this ) );
	}

	@Override
	public void setColor( final ARGBType color )
	{
		setColor( color.get() );
	}

	public void setColor( final int rgb )
	{
		if ( !supportsColor() || color.get() == rgb )
			return;

		color.set( rgb );
		listeners.list.forEach( l -> l.setupParametersChanged( this ) );
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
}
