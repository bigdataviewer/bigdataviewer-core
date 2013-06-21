package viewer.gui.brightness;

import net.imglib2.type.numeric.ARGBType;

public interface ConverterSetup
{
	public int getSetupId();

	public void setDisplayRange( int min, int max );

	public void setColor( final ARGBType color );

	public int getDisplayRangeMin();

	public int getDisplayRangeMax();

	public ARGBType getColor();
}