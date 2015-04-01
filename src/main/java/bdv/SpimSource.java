package bdv;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

public class SpimSource< T extends NumericType< T > > extends AbstractSpimSource< T >
{
	protected final ViewerSetupImgLoader< T, ? > imgLoader;

	@SuppressWarnings( "unchecked" )
	public SpimSource( final AbstractSpimData< ? > spimData, final int setup, final String name )
	{
		super( spimData, setup, name );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		imgLoader = ( ViewerSetupImgLoader< T, ? > ) ( ( ViewerImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
		loadTimepoint( 0 );
	}

	@Override
	public T getType()
	{
		return imgLoader.getImageType();
	}

	@Override
	protected RandomAccessibleInterval< T > getImage( final int timepointId, final int level )
	{
		return imgLoader.getImage( timepointId, level );
	}

	@Override
	protected AffineTransform3D[] getMipmapTransforms()
	{
		return imgLoader.getMipmapTransforms();
	}
}
