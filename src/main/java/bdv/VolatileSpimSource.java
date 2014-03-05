package bdv;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class VolatileSpimSource< T extends NumericType< T >, V extends Volatile< T > & NumericType< V >  > extends AbstractSpimSource< V >
{
	protected final SpimSource< T > nonVolatileSource;

	protected final ViewerImgLoader< ?, V > imgLoader;

	@SuppressWarnings( "unchecked" )
	public VolatileSpimSource( final SequenceViewsLoader loader, final int setup, final String name )
	{
		super( loader, setup, name );
		nonVolatileSource = new SpimSource< T >( loader, setup, name );
		final SequenceDescription seq = loader.getSequenceDescription();
		imgLoader = ( ViewerImgLoader< ?, V > ) seq.imgLoader;
		loadTimepoint( 0 );
	}

	@Override
	public V getType()
	{
		return imgLoader.getVolatileImageType();
	}

	public SpimSource< T > nonVolatile()
	{
		return nonVolatileSource;
	}

	@Override
	protected RandomAccessibleInterval< V > getImage( final View view, final int level )
	{
		return imgLoader.getVolatileImage( view, level );
	}

	@Override
	protected AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return imgLoader.getMipmapTransforms( setup );
	}
}
