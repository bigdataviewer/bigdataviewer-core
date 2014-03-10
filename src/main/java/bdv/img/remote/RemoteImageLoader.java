package bdv.img.remote;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import mpicbg.spim.data.View;
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

import org.jdom2.Element;

import bdv.AbstractViewerImgLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.util.MipmapTransforms;

import com.google.gson.Gson;

public class RemoteImageLoader extends AbstractViewerImgLoader< UnsignedShortType, VolatileUnsignedShortType >
{
	protected String baseUrl;

	protected RemoteImageLoaderMetaData metadata;

	protected final ArrayList< AffineTransform3D[] > perSetupMipmapTransforms;

	protected int[][] cellsDimensions;

	protected VolatileGlobalCellCache< VolatileShortArray > cache;

	public RemoteImageLoader()
	{
		super( new UnsignedShortType(), new VolatileUnsignedShortType() );
		perSetupMipmapTransforms = new ArrayList< AffineTransform3D[] >();
	}

	private void open() throws IOException
	{
		final URL url = new URL( baseUrl + "?p=init" );
		metadata = new Gson().fromJson(
				new InputStreamReader( url.openStream() ),
				RemoteImageLoaderMetaData.class );
		cache = new VolatileGlobalCellCache< VolatileShortArray >(
				new RemoteVolatileShortArrayLoader( this ),
				metadata.numTimepoints,
				metadata.numSetups,
				metadata.maxNumLevels,
				metadata.maxLevels,
				10 );
		for ( int setup = 0; setup < metadata.numSetups; ++setup )
		{
			final double[][] mipmapResolutions = metadata.perSetupMipmapResolutions.get( setup );
			final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[ mipmapResolutions.length ];
			for ( int level = 0; level < mipmapResolutions.length; level++ )
				mipmapTransforms[ level ] = MipmapTransforms.getMipmapTransformDefault( mipmapResolutions[ level ] );
			perSetupMipmapTransforms.add( mipmapTransforms );
		}
		cellsDimensions = metadata.createCellsDimensions();
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		try
		{
			baseUrl = elem.getChildText( "baseUrl" );
			open();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final View view, final int level )
	{
		if ( ! existsImageData( view, level ) )
		{
			System.err.println( "image data for " + view.getBasename() + " level " + level + " could not be found. Partition file missing?" );
			return getMissingDataImage( view, level, new UnsignedShortType() );
		}
		final CachedCellImg< UnsignedShortType, VolatileShortArray >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final View view, final int level )
	{
		if ( ! existsImageData( view, level ) )
		{
			System.err.println( "image data for " + view.getBasename() + " level " + level + " could not be found." );
			return getMissingDataImage( view, level, new VolatileUnsignedShortType() );
		}
		final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray >  img = prepareCachedImage( view, level, LoadingStrategy.BUDGETED );
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
	public double[][] getMipmapResolutions( final int setup )
	{
		return metadata.perSetupMipmapResolutions.get( setup );
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return perSetupMipmapTransforms.get( setup );
	}

	public int[][] getSubdivisions( final int setup )
	{
		return metadata.perSetupSubdivisions.get( setup );
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return getMipmapResolutions( setup ).length;
	}

	/**
	 * Checks whether the given image data is present on the server.
	 *
	 * @return true, if the given image data is present.
	 */
	public boolean existsImageData( final View view, final int level )
	{
		final int timepoint = view.getTimepointIndex();
		final int setup = view.getSetupIndex();
		return existsImageData( timepoint, setup, level );
	}

	/**
	 * Checks whether the given image data is present on the server.
	 *
	 * @return true, if the given image data is present.
	 */
	public boolean existsImageData( final int timepoint, final int setup, final int level )
	{
		final int index = getViewInfoCacheIndex( timepoint, setup, level );
		return metadata.existence[ index ];
	}

	/**
	 * For images that are missing on the server, a constant image is created.
	 * If the dimension of the missing image is present in
	 * {@link RemoteImageLoaderMetaData#dimensions} then use that. Otherwise
	 * create a 1x1x1 image.
	 */
	protected < T > RandomAccessibleInterval< T > getMissingDataImage( final View view, final int level, final T constant )
	{
		final int t = view.getTimepointIndex();
		final int s = view.getSetupIndex();
		final int index = getViewInfoCacheIndex( t, s, level );
		long[] d = metadata.dimensions[ index ];
		if ( d == null )
			d = new long[] { 1, 1, 1 };
		return Views.interval( new ConstantRandomAccessible< T >( constant, 3 ), new FinalInterval( d ) );
	}

	public long[] getImageDimension( final int timepoint, final int setup, final int level )
	{
		final int index = getViewInfoCacheIndex( timepoint, setup, level );
		return metadata.dimensions[ index ];
	}

	private int getViewInfoCacheIndex( final int timepoint, final int setup, final int level )
	{
		return level + metadata.maxNumLevels * ( setup + metadata.numSetups * timepoint );
	}

	int getCellIndex( final int timepoint, final int setup, final int level, final long[] globalPosition )
	{
		final int index = getViewInfoCacheIndex( timepoint, setup, level );
		final int[] cellSize = getSubdivisions( setup )[ level ];
		final int[] cellPos = new int[] {
				( int ) globalPosition[ 0 ] / cellSize[ 0 ],
				( int ) globalPosition[ 1 ] / cellSize[ 1 ],
				( int ) globalPosition[ 2 ] / cellSize[ 2 ] };
		return IntervalIndexer.positionToIndex( cellPos, cellsDimensions[ index ] );
	}

	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		if ( cache == null )
			throw new RuntimeException( "no connection open" );

		final int timepoint = view.getTimepointIndex();
		final int setup = view.getSetupIndex();
		final long[] dimensions = getImageDimension( timepoint, setup, level );
		final int[] cellDimensions = metadata.perSetupSubdivisions.get( setup )[ level ];

		final int priority = numMipmapLevels( setup ) - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );

		final CellCache< VolatileShortArray > c = cache.new VolatileCellCache( timepoint, setup, level, cacheHints );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, 1, dimensions, cellDimensions );
		final CachedCellImg< T, VolatileShortArray > img = new CachedCellImg< T, VolatileShortArray >( cells );
		return img;
	}
}
