package viewer.gui.brightness;

import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import viewer.SpimViewer;

public class RealARGBColorConverterSetup< T extends RealType< T > > implements ConverterSetup
{
	protected final int id;

	protected final RealARGBColorConverter< T > converter;

	protected SpimViewer viewer;

	public RealARGBColorConverterSetup( final int setupId, final RealARGBColorConverter< T > converter )
	{
		this.id = setupId;
		this.converter = converter;
		this.viewer = null;
	}

	@Override
	public void setDisplayRange( final int min, final int max )
	{
		converter.setMin( min );
		converter.setMax( max );
		if ( viewer != null )
			viewer.requestRepaint();
	}

	@Override
	public void setColor( final ARGBType color )
	{
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
		return ( int ) converter.getMin();
	}

	@Override
	public int getDisplayRangeMax()
	{
		return ( int ) converter.getMax();
	}

	@Override
	public ARGBType getColor()
	{
		return converter.getColor();
	}

	public void setViewer( final SpimViewer viewer )
	{
		this.viewer = viewer;
	}
}