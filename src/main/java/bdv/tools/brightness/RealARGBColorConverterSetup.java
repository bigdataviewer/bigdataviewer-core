package bdv.tools.brightness;

import java.util.Arrays;
import java.util.List;

import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import bdv.viewer.ViewerPanel;

public class RealARGBColorConverterSetup implements ConverterSetup
{
	protected final int id;

	protected final List< ColorConverter > converters;

	protected ViewerPanel viewer;

	public RealARGBColorConverterSetup( final int setupId, final ColorConverter ... converters )
	{
		this( setupId, Arrays.< ColorConverter >asList( converters ) );
	}

	public RealARGBColorConverterSetup( final int setupId, final List< ColorConverter > converters )
	{
		this.id = setupId;
		this.converters = converters;
		this.viewer = null;
	}

	@Override
	public void setDisplayRange( final double min, final double max )
	{
		for ( final ColorConverter converter : converters )
		{
			converter.setMin( min );
			converter.setMax( max );
		}
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public void setColor( final ARGBType color )
	{
		for ( final ColorConverter converter : converters )
			converter.setColor( color );
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public boolean supportsColor()
	{
		return converters.get( 0 ).supportsColor();
	}

	@Override
	public int getSetupId()
	{
		return id;
	}

	@Override
	public double getDisplayRangeMin()
	{
		return converters.get( 0 ).getMin();
	}

	@Override
	public double getDisplayRangeMax()
	{
		return converters.get( 0 ).getMax();
	}

	@Override
	public ARGBType getColor()
	{
		return converters.get( 0 ).getColor();
	}

	public void setViewer( final ViewerPanel viewer )
	{
		this.viewer = viewer;
	}
}
