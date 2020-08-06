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
package bdv.spimdata.legacy;

import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.legacy.LegacyImgLoader;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.cache.CacheControl;

//@Deprecated
public class LegacyViewerImgLoaderExtWrapper< T, V extends Volatile< T >, I extends LegacyViewerImgLoader< T, V > & LegacyImgLoader< T > > implements ViewerImgLoader, ImgLoader
{
	protected final I legacyImgLoader;

	private final HashMap< Integer, SetupImgLoaderWrapper > setupImgLoaders;

	public LegacyViewerImgLoaderExtWrapper( final I legacyImgLoader )
	{
		this.legacyImgLoader = legacyImgLoader;
		setupImgLoaders = new HashMap<>();
	}

	@Override
	public synchronized SetupImgLoaderWrapper getSetupImgLoader( final int setupId )
	{
		SetupImgLoaderWrapper sil = setupImgLoaders.get( setupId );
		if ( sil == null )
		{
			sil = new SetupImgLoaderWrapper( setupId );
			setupImgLoaders.put( setupId, sil );
		}
		return sil;
	}

	@Override
	public CacheControl getCacheControl()
	{
		return legacyImgLoader.getCache();
	}

	public class SetupImgLoaderWrapper implements ViewerSetupImgLoader< T, V >, SetupImgLoader< T >
	{
		private final int setupId;

		protected SetupImgLoaderWrapper( final int setupId )
		{
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getImage( new ViewId( timepointId, setupId ) );
		}

		@Override
		public T getImageType()
		{
			return legacyImgLoader.getImageType();
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getImage( new ViewId( timepointId, setupId ), level );
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return legacyImgLoader.getMipmapResolutions( setupId );
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return legacyImgLoader.getMipmapTransforms( setupId );
		}

		@Override
		public int numMipmapLevels()
		{
			return legacyImgLoader.numMipmapLevels( setupId );
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getVolatileImage( new ViewId( timepointId, setupId ), level );
		}

		@Override
		public V getVolatileImageType()
		{
			return legacyImgLoader.getVolatileImageType();
		}

		@Override
		public RandomAccessibleInterval< FloatType > getFloatImage( final int timepointId, final boolean normalize, final ImgLoaderHint... hints )
		{
			return legacyImgLoader.getFloatImage( new ViewId( timepointId, setupId ), normalize );
		}

		@Override
		public Dimensions getImageSize( final int timepointId )
		{
			return legacyImgLoader.getImageSize( new ViewId( timepointId, setupId ) );
		}

		@Override
		public VoxelDimensions getVoxelSize( final int timepointId )
		{
			return legacyImgLoader.getVoxelSize( new ViewId( timepointId, setupId ) );
		}
	}
}
