/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.spimdata;

import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.img.cache.Cache;

public class WrapBasicImgLoader implements ViewerImgLoader
{
	/**
	 * If the {@link BasicImgLoader image loader} of {@code spimData} is not a
	 * {@link ViewerImgLoader}, then replace it with a wrapper that presents it
	 * as {@link ViewerImgLoader}.
	 *
	 * However, note that trying to call
	 * {@link ViewerSetupImgLoader#getVolatileImage(int, int, ImgLoaderHint...)}
	 * or {@link ViewerSetupImgLoader#getVolatileImageType()} on the wrapper
	 * will throw an {@link UnsupportedOperationException}.
	 *
	 * @param spimData
	 * @return {@code true} if wrapping was necessary, {@code false} if
	 *         {@code spimData} had a {@link ViewerImgLoader} already.
	 */
	public static boolean wrapImgLoaderIfNecessary( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader imgLoader = seq.getImgLoader();
		if ( !( imgLoader instanceof ViewerImgLoader ) )
		{
			setImgLoader( seq, new WrapBasicImgLoader( imgLoader, seq.getViewSetups() ) );
			return true;
		}
		else
			return false;
	}

	public static boolean removeWrapperIfPresent( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader imgLoader = seq.getImgLoader();
		if ( imgLoader instanceof WrapBasicImgLoader )
		{
			setImgLoader( seq, ( ( WrapBasicImgLoader ) imgLoader ).source );
			return true;
		}
		else
			return false;
	}

	@SuppressWarnings( "unchecked" )
	private static < L extends BasicImgLoader > void setImgLoader( final AbstractSequenceDescription< ?, ?, L > seq, final BasicImgLoader newLoader )
	{
		seq.setImgLoader( ( L ) newLoader );
	}

	private static final double[][] mipmapResolutions = new double[][] { { 1, 1, 1 } };

	private static final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[] { new AffineTransform3D() };

	private static final Cache cache = new Cache.Dummy();

	private final HashMap< Integer, WrapSetupImgLoader< ?, ? > > wrapped;

	private final BasicImgLoader source;

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public WrapBasicImgLoader( final BasicImgLoader source, final Map< Integer, ? > setupsMap )
	{
		this.source = source;
		wrapped = new HashMap< Integer, WrapSetupImgLoader< ?, ? > >();
		for ( final int setupId : setupsMap.keySet() )
			wrapped.put( setupId, new WrapSetupImgLoader( source.getSetupImgLoader( setupId ) ) );
	}

	@Override
	public ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId )
	{
		return wrapped.get( setupId );
	}

	@Override
	public Cache getCache()
	{
		return cache;
	}

	private class WrapSetupImgLoader< T, V extends Volatile< T > > implements ViewerSetupImgLoader< T, V >
	{
		private final BasicSetupImgLoader< T > source;

		private WrapSetupImgLoader( final BasicSetupImgLoader< T > source )
		{
			this.source = source;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
		{
			return source.getImage( timepointId, hints );
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			return source.getImage( timepointId, hints );
		}

		@Override
		public T getImageType()
		{
			return source.getImageType();
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public V getVolatileImageType()
		{
			throw new UnsupportedOperationException();
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
			return 1;
		}
	}
}
