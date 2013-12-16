package viewer.gui.brightness;

import java.util.List;

import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import viewer.ViewerPanel;

public class RealARGBColorConverterSetup implements ConverterSetup
{
	protected final int id;

	protected final List< RealARGBColorConverter< ? > > converters;

	protected ViewerPanel viewer;

	public RealARGBColorConverterSetup( final int setupId, final List< RealARGBColorConverter< ? > > converters )
	{
		this.id = setupId;
		this.converters = converters;
		this.viewer = null;
	}

	@Override
	public void setDisplayRange( final int min, final int max )
	{
		for ( final RealARGBColorConverter< ? > converter : converters )
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
		for ( final RealARGBColorConverter< ? > converter : converters )
			converter.setColor( color );
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public int getSetupId()
	{
		return id;
	}

	@Override
	public int getDisplayRangeMin()
	{
		return ( int ) converters.get( 0 ).getMin();
	}

	@Override
	public int getDisplayRangeMax()
	{
		return ( int ) converters.get( 0 ).getMax();
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