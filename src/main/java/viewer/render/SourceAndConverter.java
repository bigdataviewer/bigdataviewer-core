package viewer.render;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

/**
 * Data source (for one view setup) and a converter to ARGBType.
 */
public class SourceAndConverter< T extends NumericType< T > >
{
	/**
	 * provides image data for all timepoints of one view.
	 */
	final protected Source< T > spimSource;

	/**
	 * converts {@link #spimSource} type T to ARGBType for display
	 */
	final protected Converter< T, ARGBType > converter;

	public SourceAndConverter( final Source< T > spimSource, final Converter< T, ARGBType > converter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
	}

	/**
	 * Get the {@link Source} (provides image data for all timepoints of one
	 * angle).
	 */
	public Source< T > getSpimSource()
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
