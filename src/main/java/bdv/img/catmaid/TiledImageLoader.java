/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.img.catmaid;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

public class TiledImageLoader< A extends VolatileAccess, T extends NativeType< T >, V extends Volatile< T > & NativeType< V > > implements ViewerImgLoader
{
	protected final int numScales;

	protected final double[][] mipmapResolutions;

	protected final AffineTransform3D[] mipmapTransforms;

	protected final long[][] imageDimensions;

	protected final int[][] blockDimensions;

	protected VolatileGlobalCellCache cache;

	final protected HashMap< Integer, TiledSetupImageLoader > setupLoaders = new HashMap<>();

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

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int tileWidth,
			final int tileHeight,
			final int[][] blockDimensions,
			final boolean topLeft )
	{
		this.numScales = blockDimensions.length;

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

			if ( topLeft )
			{
				mipmapTransform.set( 0.5 * ( sixy - 1 ), 0, 3 );
				mipmapTransform.set( 0.5 * ( sixy - 1 ), 1, 3 );
			}
			mipmapTransform.set( 0.5 * ( zScale * siz - 1 ), 2, 3 );

			mipmapTransforms[ l ] = mipmapTransform;
		}

		for ( int i = 0; i < loaders.size(); ++i )
			setupLoaders.put( i, new TiledSetupImageLoader( loaders.get( i ), type, vType, i ) );

		cache = new VolatileGlobalCellCache( numScales, 10 );
	}

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int tileWidth,
			final int tileHeight,
			final int[][] blockDimensions )
	{
		this( loaders, type, vType, width, height, depth, zScale, tileWidth, tileHeight, blockDimensions, true );
	}

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final int tileWidth,
			final int tileHeight,
			final int blockWidth,
			final int blockHeight,
			final boolean topLeft )
	{
		this( loaders, type, vType, width, height, depth, zScale, tileWidth, tileHeight, blockDimensions( blockWidth, blockHeight, numScales ), topLeft );
	}

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final int tileWidth,
			final int tileHeight,
			final int blockWidth,
			final int blockHeight )
	{
		this( loaders, type, vType, width, height, depth, zScale, tileWidth, tileHeight, blockDimensions( blockWidth, blockHeight, numScales ), true );
	}

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final int tileWidth,
			final int tileHeight,
			final boolean topLeft )
	{
		this( loaders, type, vType, width, height, depth, zScale, numScales, tileWidth, tileHeight, tileWidth, tileHeight, topLeft );
	}

	public TiledImageLoader(
			final List< CacheArrayLoader< A > > loaders,
			final T type,
			final V vType,
			final long width,
			final long height,
			final long depth,
			final double zScale,
			final int numScales,
			final int tileWidth,
			final int tileHeight )
	{
		this( loaders, type, vType, width, height, depth, zScale, numScales, tileWidth, tileHeight, true );
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
	 */
	protected < N extends NativeType< N > > CachedCellImg< N, A > prepareCachedImage(
			final CacheArrayLoader< A > loader,
			final int timepointId,
			final int setupId,
			final int level,
			final LoadingStrategy loadingStrategy )
	{
		final long[] dimensions = imageDimensions[ level ];

		final int priority = numScales - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< A > c = cache.new VolatileCellCache<>( timepointId, setupId, level, cacheHints, loader );
		final VolatileImgCells< A > cells = new VolatileImgCells<>( c, new Fraction(), dimensions, blockDimensions[ level ] );
		final CachedCellImg< N, A > img = new CachedCellImg<>( cells );
		return img;
	}

	@Override
	public VolatileGlobalCellCache getCacheControl()
	{
		return cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return setupLoaders.get( setupId );
	}

	/**
	 * Reflection hack because there is no T NativeType<T>.create(NativeImg<?, A>) method in ImgLib2
	 * Note that for this method to be introduced, NativeType would need an additional generic parameter A
	 * that specifies the accepted family of access objects that can be used in the NativeImg... big change
	 *
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected void linkType( final NativeType t, final CachedCellImg img ) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		final Constructor constructor = t.getClass().getDeclaredConstructor( NativeImg.class );
		if ( constructor != null )
		{
			final NativeType linkedType = ( NativeType )constructor.newInstance( img );
			img.setLinkedType( linkedType );
		}
	}

	public void setCache( final VolatileGlobalCellCache cache )
	{
		this.cache = cache;
	}

	public class TiledSetupImageLoader extends AbstractViewerSetupImgLoader< T, V >
	{
		final protected int setupId;

		final protected CacheArrayLoader< A > loader;

		public TiledSetupImageLoader( final CacheArrayLoader< A > loader, final T type, final V volatileType, final int setupId )
		{
			super( type, volatileType );

			this.loader = loader;
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			try
			{
				final CachedCellImg< T, A >  img = prepareCachedImage( loader, timepointId, setupId, level, LoadingStrategy.BLOCKING );
				linkType( type, img );
				return img;
			}
			catch ( final Exception e )
			{
				e.printStackTrace( System.err );
				return null;
			}
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			try
			{
				final CachedCellImg< V, A > img = prepareCachedImage( loader, timepointId, setupId, level, LoadingStrategy.VOLATILE );
				linkType( volatileType, img );
				return img;
			}
			catch ( final Exception e )
			{
				e.printStackTrace( System.err );
				return null;
			}
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
}
