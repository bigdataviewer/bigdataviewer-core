package viewer;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

/**
 * SPIM data source (for one angle) and a converter to ARGBType.
 */
public class SourceAndConverter< T extends NumericType< T > >
{
	/**
	 * provides image data for all timepoints of one view.
	 */
	final protected SpimSource< T > spimSource;

	/**
	 * converts {@link #spimSource} type T to ARGBType for display
	 */
	final protected Converter< T, ARGBType > converter;

	public SourceAndConverter( final SpimSource< T > spimSource, final Converter< T, ARGBType > converter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
	}

	/**
	 * Get the {@link SpimSource} (provides image data for all timepoints of one
	 * angle).
	 */
	public SpimSource< T > getSpimSource()
	{
		return spimSource;
	}

	/**
	 * Get the {@link Converter} (converts {@link #source} type T to ARGBType
	 * for display).
	 */
	public Converter< T, ARGBType > getConverter()
	{
		return converter;
	}
}
