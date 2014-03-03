package bdv.img.catmaid;

import java.io.File;

import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

import org.jdom2.Element;

import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

public class CatmaidImageLoader implements ViewerImgLoader< UnsignedShortType, VolatileUnsignedShortType >
{
	private long width;

	private long height;

	private long depth;

	private double resXY;

	private double resZ;

	private String baseUrl;

	private int tileWidth;

	private int tileHeight;

	private int numScales;

	private double[][] mipmapResolutions;

	private long[][] imageDimensions;

	private int[][] blockDimensions;

	protected VolatileGlobalCellCache< VolatileShortArray > cache;

	@Override
	public void init( final Element elem, final File basePath )
	{
		width = 1987;
		height = 1441;
		depth = 460;

		resXY = 5.6;
		resZ = 11.2;

//		baseUrl = "file:/Users/pietzsch/Desktop/data/catmaid/xy/";
		baseUrl = "http://fly.mpi-cbg.de/map/fib/aligned/xy/";

		tileWidth = 256;
		tileHeight = 256;


		numScales = getNumScales( width, height, tileWidth, tileHeight );
		System.out.println( "numScales = " + numScales );

		mipmapResolutions = new double[ numScales ][];
		imageDimensions = new long[ numScales ][];
		blockDimensions = new int[ numScales ][];
		for ( int l = 0; l < numScales; ++l )
		{

			mipmapResolutions[ l ] = new double[] { 1 << l, 1 << l, 1 };
			imageDimensions[ l ] = new long[] { width >> l, height >> l, depth };
			blockDimensions[ l ] = new int[] { tileWidth, tileHeight, 1 };
		}

		final int[] maxLevels = new int[] { numScales - 1 };
		cache = new VolatileGlobalCellCache< VolatileShortArray >(
				new CatmaidVolatileShortArrayLoader( baseUrl, tileWidth, tileHeight ), 1, 1, numScales, maxLevels, 10 );
	}

	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	@Override
	public Element toXml( final File basePath )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< FloatType > getFloatImage( final View view )
	{
		throw new UnsupportedOperationException( "not implemented" );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final View view )
	{
		return getImage( view, 0 );
	}

	@Override
	public RandomAccessibleInterval< UnsignedShortType > getImage( final View view, final int level )
	{
		final CellImg< UnsignedShortType, VolatileShortArray, VolatileCell< VolatileShortArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final UnsignedShortType linkedType = new UnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final View view, final int level )
	{
		final CellImg< VolatileUnsignedShortType, VolatileShortArray, VolatileCell< VolatileShortArray > >  img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public double[][] getMipmapResolutions( final int setup )
	{
		return mipmapResolutions;
	}

	@Override
	public int numMipmapLevels( final int setup )
	{
		return numScales;
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache.
	 * The created image needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked type} before it can be used.
	 * The type should be either {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = blockDimensions[ level ];

		final CellCache< VolatileShortArray > c = cache.new VolatileCellCache( view.getTimepointIndex(), view.getSetupIndex(), level, loadingStrategy );
		final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, 1, dimensions, cellDimensions );
		final CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > > img = new CellImg< T, VolatileShortArray, VolatileCell< VolatileShortArray > >( null, cells );
		return img;
	}

	public Cache getCache()
	{
		return cache;
	}

	private final UnsignedShortType type = new UnsignedShortType();

	private final VolatileUnsignedShortType volatileType = new VolatileUnsignedShortType();

	@Override
	public UnsignedShortType getImageType()
	{
		return type;
	}

	@Override
	public VolatileUnsignedShortType getVolatileImageType()
	{
		return volatileType;
	}

}
