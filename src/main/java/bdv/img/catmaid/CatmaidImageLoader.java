package bdv.img.catmaid;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Fraction;
import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

public class CatmaidImageLoader extends AbstractViewerSetupImgLoader< ARGBType, VolatileARGBType > implements ViewerImgLoader
{
	private final int numScales;

	private final int tileWidth;

	private final int tileHeight;

	private final double[][] mipmapResolutions;

	private final AffineTransform3D[] mipmapTransforms;

	private final long[][] imageDimensions;

	private final int[][] blockDimensions;

	private final VolatileGlobalCellCache cache;

	private final CatmaidVolatileIntArrayLoader loader;

	final static private int[][] blockDimensions(
			final int tileWidth,
			final int tileHeight,
			final int numScales )
	{
		final int[][] blockDimensions = new int[ numScales ][];
		for ( int i = 0; i < numScales; ++i )
			blockDimensions[ i ] = new int[]{ tileWidth, tileHeight, 1 };

		return blockDimensions;
	}

	public CatmaidImageLoader(
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final String urlFormat,
			final int tileWidth,
			final int tileHeight,
			final int[][] blockDimensions )
	{
		super( new ARGBType(), new VolatileARGBType() );
		this.numScales = blockDimensions.length;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;

		mipmapResolutions = new double[ numScales ][];
		imageDimensions = new long[ numScales ][];
		mipmapTransforms = new AffineTransform3D[ numScales ];
		final int[] zScales = new int[ numScales ];
		this.blockDimensions = new int[ numScales ][];
		for ( int l = 0; l < numScales; ++l )
		{
			final int sixy = 1 << l;
			final int siz = Math.max( 1, ( int )Math.round( sixy / zScale ) );

			mipmapResolutions[ l ] = new double[] { sixy, sixy, siz };
			imageDimensions[ l ] = new long[] { width >> l, height >> l, depth / siz };
			this.blockDimensions[ l ] = blockDimensions[ l ].clone();
			zScales[ l ] = siz;

			final AffineTransform3D mipmapTransform = new AffineTransform3D();

			mipmapTransform.set( sixy, 0, 0 );
			mipmapTransform.set( sixy, 1, 1 );
			mipmapTransform.set( zScale * siz, 2, 2 );

			mipmapTransform.set( 0.5 * ( sixy - 1 ), 0, 3 );
			mipmapTransform.set( 0.5 * ( sixy - 1 ), 1, 3 );
			mipmapTransform.set( 0.5 * ( zScale * siz - 1 ), 2, 3 );

			mipmapTransforms[ l ] = mipmapTransform;
		}

		loader = new CatmaidVolatileIntArrayLoader( urlFormat, tileWidth, tileHeight, zScales );
		cache = new VolatileGlobalCellCache( 1, 1, numScales, 10 );
	}

	public CatmaidImageLoader(
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final String urlFormat,
			final int tileWidth,
			final int tileHeight,
			final int blockWidth,
			final int blockHeight )
	{
		this( width, height, depth, zScale, urlFormat, tileWidth, tileHeight, blockDimensions( blockWidth, blockHeight, numScales ) );
	}

	public CatmaidImageLoader(
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final String urlFormat,
			final int tileWidth,
			final int tileHeight )
	{
		this( width, height, depth, zScale, numScales, urlFormat, tileWidth, tileHeight, tileWidth, tileHeight );
	}

	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	/**
	 * (Almost) create a {@link CachedCellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, VolatileIntArray > prepareCachedImage(
			final int timepointId,
			final int setupId,
			final int level,
			final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];

		final int priority = numScales - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< VolatileIntArray > c = cache.new VolatileCellCache< VolatileIntArray >( timepointId, setupId, level, cacheHints, loader );
		final VolatileImgCells< VolatileIntArray > cells = new VolatileImgCells< VolatileIntArray >( c, new Fraction(), dimensions, blockDimensions[ level ] );
		final CachedCellImg< T, VolatileIntArray > img = new CachedCellImg< T, VolatileIntArray >( cells );
		return img;
	}

	@Override
	public VolatileGlobalCellCache getCache()
	{
		return cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getImage( final int timepointId, final int level )
	{
		final CachedCellImg< ARGBType, VolatileIntArray >  img = prepareCachedImage( timepointId, 0, level, LoadingStrategy.BLOCKING );
		final ARGBType linkedType = new ARGBType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileARGBType > getVolatileImage( final int timepointId, final int level )
	{
		final CachedCellImg< VolatileARGBType, VolatileIntArray >  img = prepareCachedImage( timepointId, 0, level, LoadingStrategy.VOLATILE );
		final VolatileARGBType linkedType = new VolatileARGBType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return mipmapResolutions;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return mipmapTransforms;
	}

	@Override
	public int numMipmapLevels()
	{
		return numScales;
	}
}
