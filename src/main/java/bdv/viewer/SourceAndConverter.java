package bdv.viewer;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

/**
 * Data source (for one view setup) and a converter to ARGBType.
 */
public class SourceAndConverter< T >
{
	/**
	 * provides image data for all timepoints of one view.
	 */
	protected final Source< T > spimSource;

	/**
	 * converts {@link #spimSource} type T to ARGBType for display
	 */
	protected final Converter< T, ARGBType > converter;

	protected final SourceAndConverter< ? extends Volatile< T > > volatileSourceAndConverter;

	public SourceAndConverter( final Source< T > spimSource, final Converter< T, ARGBType > converter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
		this.volatileSourceAndConverter = null;
	}

	public SourceAndConverter( final Source< T > spimSource, final Converter< T, ARGBType > converter, final SourceAndConverter< ? extends Volatile< T > > volatileSourceAndConverter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
		this.volatileSourceAndConverter = volatileSourceAndConverter;
	}

	/**
	 * copy constructor
	 * @param soc
	 */
	protected SourceAndConverter( final SourceAndConverter< T > soc )
	{
		this.spimSource = soc.spimSource;
		this.converter = soc.converter;
		this.volatileSourceAndConverter = soc.volatileSourceAndConverter;
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
	 * Get the {@link Converter} (converts source type T to ARGBType
	 * for display).
	 */
	public Converter< T, ARGBType > getConverter()
	{
		return converter;
	}

	public SourceAndConverter< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceAndConverter;
	}
}
