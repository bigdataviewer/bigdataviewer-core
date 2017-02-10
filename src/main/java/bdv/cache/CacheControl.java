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
package bdv.cache;

import java.util.concurrent.CopyOnWriteArrayList;

import bdv.img.cache.VolatileGlobalCellCache;

/**
 * This is the part of the {@link VolatileGlobalCellCache} interface that is
 * exposed to the renderer directly (that is, not via images). It comprises
 * methods to control cache behavior. If the renderer is used without
 * {@link VolatileGlobalCellCache}, these can be simply implemented to do
 * nothing.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface CacheControl
{
	/**
	 * Prepare the cache for providing data for the "next frame".
	 * <p>
	 * For the {@link VolatileGlobalCellCache}, this means that
	 * <ul>
	 * <li>the contents of fetch queues is moved to the prefetch. and
	 * <li>the internal frame counter is incremented, which will enable
	 * previously enqueued requests to be enqueued again for the new frame.
	 * </ul>
	 */
	public void prepareNextFrame();

	/**
	 * {@link CacheControl} that does nothing.
	 */
	public static class Dummy implements CacheControl
	{
		@Override
		public void prepareNextFrame()
		{}
	}

	/**
	 * {@link CacheControl} backed by a set of {@link CacheControl}s.
	 * {@link #prepareNextFrame()} forwards to all of them.
	 */
	public static class CacheControls implements CacheControl
	{
		private final CopyOnWriteArrayList< CacheControl > cacheControls = new CopyOnWriteArrayList<>();

		public synchronized void addCacheControl( final CacheControl cacheControl, final int index )
		{
			cacheControls.remove( cacheControl );
			final int s = cacheControls.size();
			cacheControls.add( index < 0 ? 0 : index > s ? s : index, cacheControl );
		}

		public synchronized void addCacheControl( final CacheControl cacheControl )
		{
			if ( !cacheControls.contains( cacheControl ) )
			{
				cacheControls.add( cacheControl );
			}
		}

		public synchronized void removeCacheControl( final CacheControl cacheControl )
		{
			cacheControls.remove( cacheControl );
		}

		@Override
		public void prepareNextFrame()
		{
			for ( final CacheControl c : cacheControls )
				c.prepareNextFrame();
		}
	}
}
