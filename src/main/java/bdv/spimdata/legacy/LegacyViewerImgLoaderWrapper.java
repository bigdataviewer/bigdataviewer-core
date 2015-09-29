package bdv.spimdata.legacy;

import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.Cache;

//@Deprecated
public class LegacyViewerImgLoaderWrapper< T, V extends Volatile< T >, I extends LegacyViewerImgLoader< T, V > > implements ViewerImgLoader
{
	protected final I legacyImgLoader;

	private final HashMap< Integer, SetupImgLoaderWrapper > setupImgLoaders;

	public LegacyViewerImgLoaderWrapper( final I legacyImgLoader )
	{
		this.legacyImgLoader = legacyImgLoader;
		setupImgLoaders = new HashMap< Integer, SetupImgLoaderWrapper >();
	}

	@Override
	public synchronized SetupImgLoaderWrapper getSetupImgLoader( final int setupId )
	{
		SetupImgLoaderWrapper sil = setupImgLoaders.get( setupId );
		if ( sil == null )
		{
			sil = new SetupImgLoaderWrapper( setupId );
			setupImgLoaders.put( setupId, sil );
		}
		return sil;
	}

	@Override
	public Cache getCache()
	{
		return legacyImgLoader.getCache();
	}

	public class SetupImgLoaderWrapper implements ViewerSetupImgLoader< T, V >
	{
		private final int setupId;

		protected SetupImgLoaderWrapper( final int setupId )
		{
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getImage( new ViewId( timepointId, setupId ) );
		}

		@Override
		public T getImageType()
		{
			return legacyImgLoader.getImageType();
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getImage( new ViewId( timepointId, setupId ), level );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return legacyImgLoader.getMipmapResolutions( setupId );
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return legacyImgLoader.getMipmapTransforms( setupId );
		}

		@Override
		public int numMipmapLevels()
		{
			return legacyImgLoader.numMipmapLevels( setupId );
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getVolatileImage( new ViewId( timepointId, setupId ), level );
		}

		@Override
		public V getVolatileImageType()
		{
			return legacyImgLoader.getVolatileImageType();
		}
	}
}
