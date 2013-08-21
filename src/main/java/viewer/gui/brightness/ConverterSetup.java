package viewer.gui.brightness;

import mpicbg.spim.data.ViewSetup;
import net.imglib2.type.numeric.ARGBType;

/**
 * Modify the range and color of the converter for a source. Because each source
 * can have its own converter, this is used to adjust brightness, contrast, and
 * color of individual sources.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public interface ConverterSetup
{
	/**
	 * Get the id of the {@link ViewSetup} this converter acts on.
	 *
	 * @return the id of the {@link ViewSetup} this converter acts on.
	 */
	public int getSetupId();

	/**
	 * Set the range of source values that is mapped to the full range of the
	 * target type. Source values outside of the specified range are clamped.
	 *
	 * @param min
	 *            source value to map to minimum of the target range.
	 * @param max
	 *            source value to map to maximum of the target range.
	 */
	public void setDisplayRange( int min, int max );

	/**
	 * Set the color for this converter.
	 */
	public void setColor( final ARGBType color );

	/**
	 * Get the (largest) source value that is mapped to the minimum of the
	 * target range.
	 *
	 * @return source value that is mapped to the minimum of the target range.
	 */
	public int getDisplayRangeMin();

	/**
	 * Get the (smallest) source value that is mapped to the maximum of the
	 * target range.
	 *
	 * @return source value that is mapped to the maximum of the target range.
	 */
	public int getDisplayRangeMax();

	/**
	 * Get the color for this converter.
	 *
	 * @return the color for this converter.
	 */
	public ARGBType getColor();
}
