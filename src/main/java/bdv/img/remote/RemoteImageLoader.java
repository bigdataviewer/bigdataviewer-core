/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.img.remote;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import bdv.cache.CacheOverrider;
import com.google.gson.GsonBuilder;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import bdv.util.ConstantRandomAccessible;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.view.Views;

public class RemoteImageLoader implements ViewerImgLoader, CacheOverrider
{
	protected String baseUrl;

	protected RemoteImageLoaderMetaData metadata;

	protected HashMap< ViewLevelId, int[] > cellsDimensions;

	protected VolatileGlobalCellCache cache;

	protected RemoteVolatileShortArrayLoader shortLoader;

	/**
	 * TODO
	 */
	protected final HashMap< Integer, SetupImgLoader > setupImgLoaders;

	public RemoteImageLoader( final String baseUrl ) throws IOException
	{
		this( baseUrl, true );
	}

	public RemoteImageLoader( final String baseUrl, final boolean doOpen ) throws IOException
	{
		this.baseUrl = baseUrl;
		setupImgLoaders = new HashMap<>();
		if ( doOpen )
			open();
	}

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		tryopen();
		return setupImgLoaders.get( setupId );
	}

	private boolean isOpen = false;

	private void open() throws IOException
	{
		if ( ! isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;
				isOpen = true;

				final URL url = new URL( baseUrl + "?p=init" );
				final GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.registerTypeAdapter( AffineTransform3D.class, new AffineTransform3DJsonSerializer() );
				metadata = gsonBuilder.create().fromJson(
						new InputStreamReader( url.openStream() ),
						RemoteImageLoaderMetaData.class );
				shortLoader = new RemoteVolatileShortArrayLoader( this );
				cache = new VolatileGlobalCellCache( metadata.maxNumLevels, 10 );
				cellsDimensions = metadata.createCellsDimensions();
				for ( final int setupId : metadata.perSetupMipmapInfo.keySet() )
					setupImgLoaders.put( setupId, new SetupImgLoader( setupId ) );
			}
		}
	}

	private void tryopen()
	{
		try
		{
			open();
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public VolatileGlobalCellCache getCacheControl()
	{
		tryopen();
		return cache;
	}

	@Override
	public synchronized void setCache(VolatileGlobalCellCache cache) {
		if ( isOpen )
		{
			if ( !isOpen )
				return;
			cache.clearCache();
		}
		this.cache = cache;
	}

	public MipmapInfo getMipmapInfo( final int setupId )
	{
		tryopen();
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
	 * For images that are missing in the hdf5, a constant image is created. If
	 * the dimension of the missing image is known (see
	 * {@link #getDimsAndExistence(ViewLevelId)}) then use that. Otherwise
	 * create a 1x1x1 image.
	 */
	protected < T > RandomAccessibleInterval< T > getMissingDataImage( final ViewLevelId id, final T constant )
	{
		final long[] d = getDimsAndExistence( id ).getDimensions();
		return Views.interval( new ConstantRandomAccessible<>( constant, 3 ), new FinalInterval( d ) );
	}

	public DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		tryopen();
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
	 * Create a {@link VolatileCachedCellImg} backed by the cache. The
	 * {@code type} should be either {@link UnsignedShortType} and
	 * {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > RandomAccessibleInterval< T > prepareCachedImage(
			final ViewLevelId id,
			final LoadingStrategy loadingStrategy,
			final T type )
	{
		tryopen();
		if ( cache == null )
			throw new RuntimeException( "no connection open" );

//		final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
		if ( ! existsImageData( id ) )
		{
			System.err.println(	String.format(
					"image data for timepoint %d setup %d level %d could not be found.",
					id.getTimePointId(), id.getViewSetupId(), id.getLevel() ) );
			return getMissingDataImage( id, type );
		}

		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final MipmapInfo mipmapInfo = metadata.perSetupMipmapInfo.get( setupId );

		final long[] dimensions = metadata.dimsAndExistence.get( id ).getDimensions();
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];
		final CellGrid grid = new CellGrid( dimensions, cellDimensions );

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		return cache.createImg( grid, timepointId, setupId, level, cacheHints, shortLoader, type );
	}

	public class SetupImgLoader extends AbstractViewerSetupImgLoader< UnsignedShortType, VolatileUnsignedShortType >
	{
		private final int setupId;

		protected SetupImgLoader( final int setupId )
		{
			super( new UnsignedShortType(), new VolatileUnsignedShortType() );
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< UnsignedShortType > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			return prepareCachedImage( id, LoadingStrategy.BLOCKING, type );
		}

		@Override
		public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			return prepareCachedImage( id, LoadingStrategy.BUDGETED, volatileType );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return getMipmapInfo( setupId ).getResolutions();
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return getMipmapInfo( setupId ).getTransforms();
		}

		@Override
		public int numMipmapLevels()
		{
			return getMipmapInfo( setupId ).getNumLevels();
		}
	}
}
