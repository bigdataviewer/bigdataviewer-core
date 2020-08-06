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
package bdv.img.imaris;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import bdv.img.imaris.DataTypes.DataType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;

public class ImarisImageLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > , A extends VolatileAccess > implements ViewerImgLoader
{
	private final DataType< T, V, A > dataType;

	private IHDF5Access hdf5Access;

	private final MipmapInfo mipmapInfo;

	private final long[][] mipmapDimensions;

	private final File hdf5File;

	private final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;

	private VolatileGlobalCellCache cache;

	private CacheArrayLoader< A > loader;

	private final HashMap< Integer, SetupImgLoader > setupImgLoaders;

	public ImarisImageLoader(
			final DataType< T, V, A > dataType,
			final File hdf5File,
			final MipmapInfo mipmapInfo,
			final long[][] mipmapDimensions,
			final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this.dataType = dataType;
		this.hdf5File = hdf5File;
		this.mipmapInfo = mipmapInfo;
		this.mipmapDimensions = mipmapDimensions;
		this.sequenceDescription = sequenceDescription;
		this.setupImgLoaders = new HashMap<>();
	}

	private boolean isOpen = false;

	private void open()
	{
		if ( !isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;
				isOpen = true;

				final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5File );

				final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();

				final int maxNumLevels = mipmapInfo.getNumLevels();

				try
				{
					hdf5Access = new HDF5AccessHack( hdf5Reader );
				}
				catch ( final Exception e )
				{
					throw new RuntimeException( e );
				}
				loader = dataType.createArrayLoader( hdf5Access );
				cache = new VolatileGlobalCellCache( maxNumLevels, 1 );

				for ( final BasicViewSetup setup : setups )
				{
					final int setupId = setup.getId();
					setupImgLoaders.put( setupId, new SetupImgLoader( setupId ) );
				}
			}
		}
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache. The created image
	 * needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked
	 * type} before it can be used. The type should be either
	 * {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > AbstractCellImg< T, A, ?, ? > prepareCachedImage( final ViewLevelId id, final LoadingStrategy loadingStrategy, final T type )
	{
		open();
		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final long[] dimensions = mipmapDimensions[ level ];
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];
		final CellGrid grid = new CellGrid( dimensions, cellDimensions );

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
	}

	@Override
	public CacheControl getCacheControl()
	{
		open();
		return cache;
	}

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		open();
		return setupImgLoaders.get( setupId );
	}

	public class SetupImgLoader extends AbstractViewerSetupImgLoader< T, V >
	{
		private final int setupId;

		protected SetupImgLoader( final int setupId )
		{
			super( dataType.getType(), dataType.getVolatileType() );
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			return prepareCachedImage( id, LoadingStrategy.BLOCKING, dataType.getType() );
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			return prepareCachedImage( id, LoadingStrategy.BUDGETED, dataType.getVolatileType() );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapInfo.getResolutions();
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapInfo.getTransforms();
		}

		@Override
		public int numMipmapLevels()
		{
			return mipmapInfo.getNumLevels();
		}
	}
}
