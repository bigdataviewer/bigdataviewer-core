package bdv.img.remote;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;
import bdv.AbstractViewerImgLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;

import com.google.gson.GsonBuilder;

public class RemoteImageLoader extends AbstractViewerImgLoader< UnsignedShortType, VolatileUnsignedShortType >
{
	protected String baseUrl;

	protected RemoteImageLoaderMetaData metadata;

	protected HashMap< ViewLevelId, int[] > cellsDimensions;

	protected VolatileGlobalCellCache< VolatileShortArray > cache;

	public RemoteImageLoader( final String baseUrl ) throws IOException
	{
		super( new UnsignedShortType(), new VolatileUnsignedShortType() );

		this.baseUrl = baseUrl;
		final URL url = new URL( baseUrl + "?p=init" );
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( AffineTransform3D.class, new AffineTransform3DJsonSerializer() );
		metadata = gsonBuilder.create().fromJson(
				new InputStreamReader( url.openStream() ),
				RemoteImageLoaderMetaData.class );
		cache = new VolatileGlobalCellCache< VolatileShortArray >(
				new RemoteVolatileShortArrayLoader( this ),
				metadata.maxNumTimepoints,
				metadata.maxNumSetups,
				metadata.maxNumLevels,
				10 );
		cellsDimensions = metadata.createCellsDimensions();
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found.",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, new UnsignedShortType() );
		}
		final CachedCellImg< UnsignedShortType, VolatileShortArray >  img = prepareCachedImage( id, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final ViewId view, final int level )
	{
		final ViewLevelId id = new ViewLevelId( view, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found.",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, new VolatileUnsignedShortType() );
		}
		final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray >  img = prepareCachedImage( id, LoadingStrategy.BUDGETED );
		final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public VolatileGlobalCellCache< VolatileShortArray > getCache()
	{
		return cache;
	}

	@Override
	public double[][] getMipmapResolutions( final int setupId )
	{
		return getMipmapInfo( setupId ).getResolutions();
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setupId )
	{
		return getMipmapInfo( setupId ).getTransforms();
	}

	@Override
	public int numMipmapLevels( final int setupId )
	{
		return getMipmapInfo( setupId ).getNumLevels();
	}

	public MipmapInfo getMipmapInfo( final int setupId )
	{
		return metadata.perSetupMipmapInfo.get( setupId );
	}

	/**
	 * Checks whether the given image data is present on the server.
	 *
	 * @return true, if the given image data is present.
	 */
	public boolean existsImageData( final ViewLevelId id )
	{
		return getDimsAndExistence( id ).exists();
	}

	/**
	 * For images that are missing in the hdf5, a constant image is created.
	 * If the dimension of the missing image is present in {@link #cachedDimensions} then use that.
	 * Otherwise create a 1x1x1 image.
	 */
	protected < T > RandomAccessibleInterval< T > getMissingDataImage( final ViewLevelId id, final T constant )
	{
		final long[] d = getDimsAndExistence( id ).getDimensions();
		return Views.interval( new ConstantRandomAccessible< T >( constant, 3 ), new FinalInterval( d ) );
	}

	public DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		return metadata.dimsAndExistence.get( id );
	}

	int getCellIndex( final int timepoint, final int setup, final int level, final long[] globalPosition )
	{
		final int[] cellDims = cellsDimensions.get( new ViewLevelId( timepoint, setup, level ) );
		final int[] cellSize = getMipmapInfo( setup ).getSubdivisions()[ level ];
		final int[] cellPos = new int[] {
				( int ) globalPosition[ 0 ] / cellSize[ 0 ],
				( int ) globalPosition[ 1 ] / cellSize[ 1 ],
				( int ) globalPosition[ 2 ] / cellSize[ 2 ] };
		return IntervalIndexer.positionToIndex( cellPos, cellDims );
	}

	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage( final ViewLevelId id, final LoadingStrategy loadingStrategy )
	{
		if ( cache == null )
			throw new RuntimeException( "no connection open" );

		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final MipmapInfo mipmapInfo = metadata.perSetupMipmapInfo.get( setupId );

		final long[] dimensions = metadata.dimsAndExistence.get( id ).getDimensions();
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileShortArray > c = cache.new VolatileCellCache( timepointId, setupId, level, cacheHints );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, 1, dimensions, cellDimensions );
		final CachedCellImg< T, VolatileShortArray > img = new CachedCellImg< T, VolatileShortArray >( cells );
		return img;
	}

}
