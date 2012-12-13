package viewer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

/**
 * SPIM data source (for one angle) and a converter to ARGBType.
 */
public class SourceAndConverter< T extends NumericType< T > > implements SpimAngleSource< T >
{
	/**
	 * provides image data for all timepoint of one view.
	 */
	final protected SpimAngleSource< T > source;

	/**
	 * converts {@link #source} type T to ARGBType for display
	 */
	final protected Converter< T, ARGBType > converter;

	public SourceAndConverter( final SpimAngleSource< T > source, final Converter< T, ARGBType > converter )
	{
		this.source = source;
		this.converter = converter;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return source.getSource( t, level );
	}

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		return source.getSourceTransform( t, level );
	}

	@Override
	public String getName()
	{
		return source.getName();
	}

	@Override
	public T getType()
	{
		return source.getType();
	}
}