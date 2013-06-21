package viewer.gui.brightness;

public interface ConverterSetup
{
	public int getSetupId();

	public void setDisplayRange( int min, int max );

	public int getDisplayRangeMin();

	public int getDisplayRangeMax();
}