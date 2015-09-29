package bdv.spimdata.legacy;

import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.legacy.LegacyImgLoader;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.Cache;

//@Deprecated
public class LegacyViewerImgLoaderExtWrapper< T, V extends Volatile< T >, I extends LegacyViewerImgLoader< T, V > & LegacyImgLoader< T > > implements ViewerImgLoader, ImgLoader
{
	protected final I legacyImgLoader;

	private final HashMap< Integer, SetupImgLoaderWrapper > setupImgLoaders;

	public LegacyViewerImgLoaderExtWrapper( final I legacyImgLoader )
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

	public class SetupImgLoaderWrapper implements ViewerSetupImgLoader< T, V >, SetupImgLoader< T >
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

		@Override
		public RandomAccessibleInterval< FloatType > getFloatImage( final int timepointId, final boolean normalize, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getFloatImage( new ViewId( timepointId, setupId ), normalize );
		}

		@Override
		public Dimensions getImageSize( final int timepointId )
		{
			return legacyImgLoader.getImageSize( new ViewId( timepointId, setupId ) );
		}

		@Override
		public VoxelDimensions getVoxelSize( final int timepointId )
		{
			return legacyImgLoader.getVoxelSize( new ViewId( timepointId, setupId ) );
		}
	}
}
