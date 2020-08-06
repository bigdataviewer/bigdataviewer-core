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
package bdv.img.catmaid;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

public class CatmaidImageLoader extends AbstractViewerSetupImgLoader< ARGBType, VolatileARGBType > implements ViewerImgLoader
{
	private final int numScales;

	private final double[][] mipmapResolutions;

	private final AffineTransform3D[] mipmapTransforms;

	private final long[][] imageDimensions;

	private final int[][] blockDimensions;

	private VolatileGlobalCellCache cache;

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
			final int[][] blockDimensions,
			final boolean topLeft )
	{
		super( new ARGBType(), new VolatileARGBType() );
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

		loader = new CatmaidVolatileIntArrayLoader( urlFormat, tileWidth, tileHeight, zScales );
		cache = new VolatileGlobalCellCache( numScales, 10 );
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
		this( width, height, depth, zScale, urlFormat, tileWidth, tileHeight, blockDimensions, true );
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
			final int blockHeight,
			final boolean topLeft )
	{
		this( width, height, depth, zScale, urlFormat, tileWidth, tileHeight, blockDimensions( blockWidth, blockHeight, numScales ), topLeft );
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
		this( width, height, depth, zScale, urlFormat, tileWidth, tileHeight, blockDimensions( blockWidth, blockHeight, numScales ), true );
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
			final boolean topLeft )
	{
		this( width, height, depth, zScale, numScales, urlFormat, tileWidth, tileHeight, tileWidth, tileHeight, topLeft );
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
		this( width, height, depth, zScale, numScales, urlFormat, tileWidth, tileHeight, true );
	}

	final static public int getNumScales( long width, long height, final long tileWidth, final long tileHeight )
	{
		int i = 1;

		while ( ( width >>= 1 ) > tileWidth && ( height >>= 1 ) > tileHeight )
			++i;

		return i;
	}

	/**
	 * Create a {@link VolatileCachedCellImg} backed by the cache. The type
	 * should be either {@link ARGBType} and {@link VolatileARGBType}.
	 */
	protected < T extends NativeType< T > > VolatileCachedCellImg< T, VolatileIntArray > prepareCachedImage(
			final int timepointId,
			final int setupId,
			final int level,
			final LoadingStrategy loadingStrategy,
			final T type )
	{
		final long[] dimensions = imageDimensions[ level ];
		final CellGrid grid = new CellGrid( dimensions, blockDimensions[ level ] );

		final int priority = numScales - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
	}

	@Override
	public VolatileGlobalCellCache getCacheControl()
	{
		return cache;
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return this;
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, 0, level, LoadingStrategy.BLOCKING, type );
	}

	@Override
	public RandomAccessibleInterval< VolatileARGBType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, 0, level, LoadingStrategy.VOLATILE, volatileType );
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

	public void setCache( final VolatileGlobalCellCache cache )
	{
		this.cache = cache;
	}
}
