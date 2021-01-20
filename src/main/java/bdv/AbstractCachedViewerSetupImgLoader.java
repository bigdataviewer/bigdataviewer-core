/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv;

import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;

/**
 * Abstract {@link ViewerSetupImgLoader} with a VolatileGlobalCellCache.
 *
 * @author Stephan Saalfeld
 */
abstract public class AbstractCachedViewerSetupImgLoader< T extends NativeType< T > , V extends Volatile< T > & NativeType< V >, A extends VolatileAccess >
	extends AbstractViewerSetupImgLoader< T, V >
{
	final protected long[][] dimensions;

	final protected int[][] cellDimensions;

	final protected double[][] resolutions;

	final protected AffineTransform3D[] mipmapTransforms;

	protected VolatileGlobalCellCache cache;

	final protected CacheArrayLoader< A > loader;

	final protected int setupId;

	public AbstractCachedViewerSetupImgLoader(
			final int setupId,
			final long[][] dimensions,
			final int[][] cellDimensions,
			final double[][] resolutions,
			final T type,
			final V vType,
			final CacheArrayLoader< A > loader,
			final VolatileGlobalCellCache cache )
	{
		super( type, vType );
		this.setupId = setupId;
		this.loader = loader;
		this.cellDimensions = cellDimensions;
		this.dimensions = dimensions;
		this.resolutions = resolutions;
		this.mipmapTransforms = new AffineTransform3D[ resolutions.length ];
		for ( int i = 0; i < resolutions.length; ++i )
		{
			final AffineTransform3D mipmapTransform = new AffineTransform3D();
			mipmapTransform.set( resolutions[ i ][ 0 ], 0, 0 );
			mipmapTransform.set( resolutions[ i ][ 1 ], 1, 1 );
			mipmapTransform.set( resolutions[ i ][ 2 ], 2, 2 );
			mipmapTransforms[ i ] = mipmapTransform;
		}

		this.cache = cache;
	}

	@Override
	public double[][] getMipmapResolutions()
	{
		return resolutions;
	}

	@Override
	public int numMipmapLevels()
	{
		return resolutions.length;
	}

	protected < S extends NativeType< S > > VolatileCachedCellImg< S, A > prepareCachedImage(
			final int timepointId,
			final int level,
			final LoadingStrategy loadingStrategy,
			final S t )
	{
		final int priority = resolutions.length - 1 - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellGrid grid = new CellGrid( dimensions[ level ], cellDimensions[level ] );

		return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, t );
	}

	@Override
	public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
	}

	@Override
	public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
	{
		return prepareCachedImage( timepointId, level, LoadingStrategy.VOLATILE, volatileType );
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms()
	{
		return mipmapTransforms;
	}
}
