package bdv.mask;

import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.cache.SharedQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * TODO: javadoc
 * <p>
 * Wraps {@code ViewerImgLoader}
 * and adds a constant {@code DoubleType(1.0)} mask.
 *
 * @author Tobias Pietzsch
 */
public class MaskedViewerImgLoader implements ViewerImgLoader
{
	/**
	 * The wrapped ViewerImgLoader
	 */
	private final ViewerImgLoader imgLoader;

	/**
	 * Maps setup id to {@code SetupImgLoader}. Lazily initialized.
	 */
	private final Map< Integer, MaskedViewerSetupImgLoader > setupImgLoaders = new ConcurrentHashMap<>();

	public MaskedViewerImgLoader( ViewerImgLoader imgLoader )
	{
		this.imgLoader = imgLoader;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return setupImgLoaders.computeIfAbsent( setupId, id -> {
			final ViewerSetupImgLoader< ?, ? > setupImgLoader = imgLoader.getSetupImgLoader( id );
			final Object type = setupImgLoader.getImageType();
			if ( !( type instanceof RealType ) )
				throw new IllegalArgumentException( "ImgLoader of type " + type.getClass() + " not supported." );
			final DoubleType mask = new DoubleType( 1 );
			@SuppressWarnings( "rawtypes" )
			MaskedViewerSetupImgLoader< ?, ?, DoubleType > maskedSetupImgLoader = new MaskedViewerSetupImgLoader( setupImgLoader, mask );
			return maskedSetupImgLoader;
		} );
	}

	@Override
	public CacheControl getCacheControl()
	{
		return imgLoader.getCacheControl();
	}

	@Override
	public void setNumFetcherThreads( final int n )
	{
		imgLoader.setNumFetcherThreads( n );
	}

	@Override
	public void setCreatedSharedQueue( final SharedQueue createdSharedQueue )
	{
		imgLoader.setCreatedSharedQueue( createdSharedQueue );
	}
}
