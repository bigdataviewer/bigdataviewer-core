/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.util.volatiles;

import bdv.cache.CacheControl;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;

/**
 * Metadata associated with a {@link VolatileView}. It comprises the types
 * of the original and volatile image, a {@link CacheControl} for the
 * volatile cache, and the wrapped {@link RandomAccessible}.
 * <p>
 * {@link VolatileViewData} is used while wrapping deeper layers of a view
 * cascade (ending at a {@link CachedCellImg}) and only on the top layer
 * wrapped as a {@link RandomAccessible} / {@link RandomAccessibleInterval}.
 * </p>
 *
 * @param <T>
 *            original image pixel type
 * @param <V>
 *            corresponding volatile pixel type
 *
 * @author Tobias Pietzsch
 */
public class VolatileViewData< T, V extends Volatile< T > >
{
	private final RandomAccessible< V > img;

	private final CacheControl cacheControl;

	private final T type;

	private final V volatileType;

	public VolatileViewData(
			final RandomAccessible< V > img,
			final CacheControl cacheControl,
			final T type,
			final V volatileType )
	{
		this.img = img;
		this.cacheControl = cacheControl;
		this.type = type;
		this.volatileType = volatileType;
	}

	/**
	 * Get the wrapped {@link RandomAccessible}.
	 *
	 * @return the wrapped {@link RandomAccessible}
	 */
	public RandomAccessible< V > getImg()
	{
		return img;
	}

	/**
	 * Get the {@link CacheControl} for the {@link CachedCellImg}(s) at the
	 * bottom of the view cascade.
	 *
	 * @return the {@link CacheControl} for the {@link CachedCellImg}(s) at the
	 *         bottom of the view cascade
	 */
	public CacheControl getCacheControl()
	{
		return cacheControl;
	}

	/**
	 * Get the pixel type of the original image.
	 *
	 * @return the pixel type of the original image
	 */
	public T getType()
	{
		return type;
	}

	/**
	 * Get the pixel type of the wrapped {@link RandomAccessible}.
	 *
	 * @return the pixel type of the wrapped {@link RandomAccessible}
	 */
	public V getVolatileType()
	{
		return volatileType;
	}
}
