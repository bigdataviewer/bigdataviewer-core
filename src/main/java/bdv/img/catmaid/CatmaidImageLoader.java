package bdv.img.catmaid;

import java.io.File;

import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

import org.jdom2.Element;

import bdv.AbstractViewerImgLoader;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

public class CatmaidImageLoader extends AbstractViewerImgLoader< ARGBType, VolatileARGBType >
{
	private String urlFormat;

	private long width;
	private long height;
	private long depth;

	private int tileWidth;
	private int tileHeight;

	private int numScales;

	private double[][] mipmapResolutions;
	private AffineTransform3D[] mipmapTransforms;
	private long[][] imageDimensions;
	
	private double zScale;
	private int[] zScales;

	private VolatileGlobalCellCache< VolatileIntArray > cache;

	public CatmaidImageLoader()
	{
		super( new ARGBType(), new VolatileARGBType() );
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		width = Long.parseLong( elem.getChildText( "width" ) );
		height = Long.parseLong( elem.getChildText( "height" ) );
		depth = Long.parseLong( elem.getChildText( "depth" ) );

		final double resXY = Double.parseDouble( elem.getChildText( "resXY" ) );
		final double resZ = Double.parseDouble( elem.getChildText( "resZ" ) );
		
		zScale = resZ / resXY;

		urlFormat = elem.getChildText( "urlFormat" );

		tileWidth = Integer.parseInt( elem.getChildText( "tileWidth" ) );
		tileHeight = Integer.parseInt( elem.getChildText( "tileHeight" ) );

		final String numScalesString = elem.getChildText( "numScales" );
		if ( numScalesString == null )
			numScales = getNumScales( width, height, tileWidth, tileHeight );
		else
			numScales = Integer.parseInt( numScalesString );

		mipmapResolutions = new double[ numScales ][];
		imageDimensions = new long[ numScales ][];
		mipmapTransforms = new AffineTransform3D[ numScales ];
		zScales = new int[ numScales ];
		for ( int l = 0; l < numScales; ++l )
		{
			final int sixy = 1 << l;
			int siz = Math.max( 1, ( int )Math.round( sixy / zScale ) );
			
			mipmapResolutions[ l ] = new double[] { sixy, sixy, siz };
			imageDimensions[ l ] = new long[] { width >> l, height >> l, depth / siz };
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

		final int[] maxLevels = new int[] { numScales - 1 };
		
		cache = new VolatileGlobalCellCache< VolatileIntArray >(
				new CatmaidVolatileIntArrayLoader( urlFormat, tileWidth, tileHeight, zScales ), 1, 1, numScales, maxLevels, 10 );
	}

	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getImage( final View view, final int level )
	{
		final CellImg< ARGBType, VolatileIntArray, VolatileCell< VolatileIntArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final ARGBType linkedType = new ARGBType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileARGBType > getVolatileImage( final View view, final int level )
	{
		final CellImg< VolatileARGBType, VolatileIntArray, VolatileCell< VolatileIntArray > >  img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final VolatileARGBType linkedType = new VolatileARGBType( img );
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
	 * The type should be either {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = new int[]{ tileWidth, tileHeight, 1 };

		final CellCache< VolatileIntArray > c = cache.new VolatileCellCache( view.getTimepointIndex(), view.getSetupIndex(), level, loadingStrategy );
		final VolatileImgCells< VolatileIntArray > cells = new VolatileImgCells< VolatileIntArray >( c, 1, dimensions, cellDimensions );
		final CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > > img = new CellImg< T, VolatileIntArray, VolatileCell< VolatileIntArray > >( null, cells );
		return img;
	}

	@Override
	public VolatileGlobalCellCache< VolatileIntArray > getCache()
	{
		return cache;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( final int setup )
	{
		return mipmapTransforms;
	}
}
