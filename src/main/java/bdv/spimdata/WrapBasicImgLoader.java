package bdv.spimdata;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;

public class WrapBasicImgLoader< T > implements ViewerImgLoader< T, Volatile< T > >
{
	/**
	 * If the {@link BasicImgLoader image loader} of {@code spimData} is not a
	 * {@link ViewerImgLoader}, then replace it with a wrapper that presents it
	 * as {@link ViewerImgLoader}.
	 *
	 * However, note that trying to call {@link #getVolatileImage(ViewId, int)}
	 * or {@link #getVolatileImageType()} on the wrapper will throw an
	 * {@link UnsupportedOperationException}.
	 *
	 * @param spimData
	 * @return {@code true} if wrapping was necessary, {@code false} if
	 *         {@code spimData} had a {@link ViewerImgLoader} already.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static boolean wrapImgLoaderIfNecessary( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader< ? > imgLoader = seq.getImgLoader();
		if ( ! ( imgLoader instanceof ViewerImgLoader ) )
		{
			setImgLoader( seq, new WrapBasicImgLoader( imgLoader ) );
			return true;
		}
		else
			return false;
	}

	public static boolean removeWrapperIfPresent( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader< ? > imgLoader = seq.getImgLoader();
		if ( imgLoader instanceof WrapBasicImgLoader )
		{
			setImgLoader( seq, ( ( WrapBasicImgLoader< ? > ) imgLoader ).source );
			return true;
		}
		else
			return false;
	}

	@SuppressWarnings( "unchecked" )
	private static < L extends BasicImgLoader< ? > > void setImgLoader( final AbstractSequenceDescription< ?, ?, L  > seq, final BasicImgLoader< ? > newLoader )
	{
		seq.setImgLoader( ( L ) newLoader );
	}

	private final BasicImgLoader< T > source;

	private static final double[][] mipmapResolutions = new double[][] { { 1, 1, 1 } };

	private static final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[] { new AffineTransform3D() };

	private static final Cache cache = new Cache.Dummy();

	public WrapBasicImgLoader( final BasicImgLoader< T > source )
	{
		this.source = source;

	}

	@Override
	public RandomAccessibleInterval< T > getImage( final ViewId view )
	{
		return source.getImage( view );
	}

	@Override
	public T getImageType()
	{
		return source.getImageType();
	}

	@Override
	public RandomAccessibleInterval< T > getImage( final ViewId view, final int level )
	{
		return getImage( view );
	}

	@Override
	public RandomAccessibleInterval< Volatile< T > > getVolatileImage( final ViewId view, final int level )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Volatile< T > getVolatileImageType()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] getMipmapResolutions( final int setupId )
	{
		return mipmapResolutions;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setupId )
	{
		return mipmapTransforms;
	}

	@Override
	public int numMipmapLevels( final int setupId )
	{
		return 1;
	}

	@Override
	public Cache getCache()
	{
		return cache;
	}
}
