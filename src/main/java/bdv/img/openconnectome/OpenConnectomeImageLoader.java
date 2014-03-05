package bdv.img.openconnectome;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;

import org.jdom2.Element;

import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.VolatileCell;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileGlobalCellCache.LoadingStrategy;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class OpenConnectomeImageLoader implements ViewerImgLoader< UnsignedByteType, VolatileUnsignedByteType >
{
	/**
	 * URL of the 
	 */
	private String baseUrl;
	
	private String token;
	
	private String mode;
	
	private int numScales;

	private double[][] mipmapResolutions;

	private long[][] imageDimensions;

	private int[][] blockDimensions;
	
	private AffineTransform3D[] mipmapTransforms;

	protected VolatileGlobalCellCache< VolatileByteArray > cache;
	
	/**
	 * Fetch the list of public tokens from an OpenConnectome volume cutout
	 * service, e.g.
	 * {@linkplain http://openconnecto.me/emca/public_tokens/}.
	 * 
	 * @param baseUrl e.g. "http://openconnecto.me/emca"
	 * @return a list of {@link String Strings}
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public String[] fetchTokenList( final String baseUrl )
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/public_tokens/" );
		final String[] tokens = gson.fromJson( new InputStreamReader( url.openStream() ), String[].class );
		return tokens;
	}
	
	/**
	 * Fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g.
	 * {@linkplain http://openconnecto.me/emca/<token>/info/}.
	 * 
	 * @param baseUrl e.g. "http://openconnecto.me/emca"
	 * @param token
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the token information
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public OpenConnectomeTokenInfo fetchTokenInfo( final String baseUrl, final String token )
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/" + token + "/info/" );
		return gson.fromJson( new InputStreamReader( url.openStream() ), OpenConnectomeTokenInfo.class );
	}
	
	/**
	 * Try to fetch the list of public tokens from an OpenConnectome volume cutout
	 * service, e.g.
	 * {@linkplain http://openconnecto.me/emca/public_tokens/}.
	 * 
	 * @param baseUrl e.g. "http://openconnecto.me/emca"
	 * @param maxNumTrials the maximum number of trials
	 * 
	 * @return a list of {@link String Strings} or <code>null</code> if
	 * 		<code>maxNumTrials</code> were executed without success
	 */
	final static public String[] tryFetchTokenList( final String baseUrl, final int maxNumTrials )
	{
		String[] tokens = null;
		for ( int i = 0; i < maxNumTrials && tokens == null; ++i )
		{
			try
			{
				tokens = fetchTokenList( baseUrl );
				break;
			}
			catch ( final Exception e ) {}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e ) {}
		}
		return tokens;
	}
	
	
	/**
	 * Try to fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g.
	 * {@linkplain http://openconnecto.me/emca/<token>/info/}.
	 * 
	 * @param baseUrl e.g. "http://openconnecto.me/emca"
	 * @param token
	 * @param maxNumTrials
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 * 		token information or <code>null</code> if <code>maxNumTrials</code>
	 * 		were executed without success
	 */
	final static public OpenConnectomeTokenInfo tryFetchTokenInfo( final String baseUrl, final String token, final int maxNumTrials )
	{
		OpenConnectomeTokenInfo info = null;
		for ( int i = 0; i < maxNumTrials && info == null; ++i )
		{
			try
			{
				info = fetchTokenInfo( baseUrl, token );
				break;
			}
			catch ( final Exception e ) {}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e ) {}
		}
		return info;
	}

	@Override
	public void init( final Element elem, final File basePath )
	{
		baseUrl = elem.getChildText( "baseUrl" );
		token = elem.getChildText( "token" );
		mode = elem.getChildText( "mode" );
		
		final OpenConnectomeTokenInfo info = tryFetchTokenInfo( baseUrl, token, 20 );
		
		numScales = info.dataset.cube_dimension.size();
		
		mipmapResolutions = info.getLevelScales( mode );
		imageDimensions = info.getLevelDimensions( mode );
		blockDimensions = info.getLevelCellDimensions();
		mipmapTransforms = info.getLevelTransforms( mode );

		final int[] maxLevels = new int[]{ numScales - 1 };
		
		cache = new VolatileGlobalCellCache< VolatileByteArray >(
				new OpenConnectomeVolatileArrayLoader( baseUrl, token, mode, info.getMinZ() ), 1, 1, numScales, maxLevels, 10 );
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
	public RandomAccessibleInterval< UnsignedByteType > getImage( final View view )
	{
		return getImage( view, 0 );
	}

	@Override
	public RandomAccessibleInterval< UnsignedByteType > getImage( final View view, final int level )
	{
		final CellImg< UnsignedByteType, VolatileByteArray, VolatileCell< VolatileByteArray > >  img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
		final UnsignedByteType linkedType = new UnsignedByteType( img );
		img.setLinkedType( linkedType );
		return img;
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedByteType > getVolatileImage( final View view, final int level )
	{
		final CellImg< VolatileUnsignedByteType, VolatileByteArray, VolatileCell< VolatileByteArray > >  img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
		final VolatileUnsignedByteType linkedType = new VolatileUnsignedByteType( img );
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
	protected < T extends NativeType< T > > CellImg< T, VolatileByteArray, VolatileCell< VolatileByteArray > > prepareCachedImage( final View view, final int level, final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = blockDimensions[ level ];

		final CellCache< VolatileByteArray > c = cache.new VolatileCellCache( view.getTimepointIndex(), view.getSetupIndex(), level, loadingStrategy );
		final VolatileImgCells< VolatileByteArray > cells = new VolatileImgCells< VolatileByteArray >( c, 1, dimensions, cellDimensions );
		final CellImg< T, VolatileByteArray, VolatileCell< VolatileByteArray > > img = new CellImg< T, VolatileByteArray, VolatileCell< VolatileByteArray > >( null, cells );
		return img;
	}

	@Override
	public Cache getCache()
	{
		return cache;
	}

	private final UnsignedByteType type = new UnsignedByteType();

	private final VolatileUnsignedByteType volatileType = new VolatileUnsignedByteType();

	@Override
	public UnsignedByteType getImageType()
	{
		return type;
	}

	@Override
	public VolatileUnsignedByteType getVolatileImageType()
	{
		return volatileType;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms( int setup )
	{
		return mipmapTransforms;
	}

}
