/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.img.openconnectome;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;

public class OpenConnectomeImageLoader extends AbstractViewerSetupImgLoader< UnsignedByteType, VolatileUnsignedByteType > implements ViewerImgLoader
{
	private final int numScales;

	private final double[][] mipmapResolutions;

	private final long[][] imageDimensions;

	private final int[][] blockDimensions;

	private final AffineTransform3D[] mipmapTransforms;

	private final VolatileGlobalCellCache cache;

	private final OpenConnectomeVolatileArrayLoader loader;

	public OpenConnectomeImageLoader( final String baseUrl, final String token, final String mode )
	{
		super( new UnsignedByteType(), new VolatileUnsignedByteType() );

		final OpenConnectomeTokenInfo info = tryFetchTokenInfo( baseUrl, token, 20 );

		numScales = info.dataset.cube_dimension.size();

		mipmapResolutions = info.getLevelScales( mode );
		imageDimensions = info.getLevelDimensions( mode );
		blockDimensions = info.getLevelCellDimensions();
		mipmapTransforms = info.getLevelTransforms( mode );

		cache = new VolatileGlobalCellCache( numScales, 10 );
		System.out.println( info.getOffsets( mode )[ 0 ][ 2 ] + " " + imageDimensions[ 0 ][ 2 ] );

		loader = new OpenConnectomeVolatileArrayLoader(
				baseUrl,
				token,
				mode,
				Math.round( info.getOffsets( mode )[ 0 ][ 2 ] ) );
	}

	/**
	 * Fetch the list of public tokens from an OpenConnectome volume cutout
	 * service, e.g. "http://openconnecto.me/ocp/ca/public_tokens/".
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @return a list of {@link String Strings}
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public String[] fetchTokenList( final String baseUrl ) throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/public_tokens/" );
		final String[] tokens = gson.fromJson( new InputStreamReader( url.openStream() ), String[].class );
		return tokens;
	}

	/**
	 * Fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g. "http://openconnecto.me/ocp/ca/&lt;token&gt;/info/".
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param token
	 *            the token whose information is desired
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 *         token information
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public OpenConnectomeTokenInfo fetchTokenInfo( final String baseUrl, final String token )
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( baseUrl + "/" + token + "/info/" );

		System.out.println( "Fetching token from " + url );

		return gson.fromJson( new InputStreamReader( url.openStream() ), OpenConnectomeTokenInfo.class );
	}

	/**
	 * Try to fetch the list of public tokens from an OpenConnectome volume
	 * cutout service, e.g.
	 * "http://openconnecto.me/ocp/ca/public_tokens/".
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param maxNumTrials
	 *            the maximum number of trials
	 *
	 * @return a list of {@link String Strings} or <code>null</code> if
	 *         <code>maxNumTrials</code> were executed without success
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
			catch ( final Exception e )
			{}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e )
			{}
		}
		return tokens;
	}

	/**
	 * Try to fetch information for a token from an OpenConnectome volume cutout
	 * service, e.g. "http://openconnecto.me/ocp/ca/&lt;token&gt;/info/".
	 *
	 * @param baseUrl
	 *            e.g. "http://openconnecto.me/ocp/ca"
	 * @param token
	 * @param maxNumTrials
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 *         token information or <code>null</code> if
	 *         <code>maxNumTrials</code> were executed without success
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
			catch ( final Exception e )
			{
				e.printStackTrace( System.err );
			}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e )
			{}
		}
		return info;
	}

	@Override
	public RandomAccessibleInterval< UnsignedByteType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, 0, level, LoadingStrategy.BLOCKING, type );
	}

	@Override
	public RandomAccessibleInterval< VolatileUnsignedByteType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, 0, level, LoadingStrategy.VOLATILE, volatileType );
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return mipmapResolutions;
	}

	@Override
	public int numMipmapLevels()
	{
		return numScales;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return mipmapTransforms;
	}

	/**
	 * Create a {@link VolatileCachedCellImg} backed by the cache. The type
	 * should be either {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > VolatileCachedCellImg< T, VolatileByteArray > prepareCachedImage( final int timepointId, final int setupId, final int level, final LoadingStrategy loadingStrategy, final T type )
	{
		final long[] dimensions = imageDimensions[ level ];
		final int[] cellDimensions = blockDimensions[ level ];
		final CellGrid grid = new CellGrid( dimensions, cellDimensions );

		final int priority = numScales - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
	}

	@Override
	public CacheControl getCacheControl()
	{
		return cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
	}
}
