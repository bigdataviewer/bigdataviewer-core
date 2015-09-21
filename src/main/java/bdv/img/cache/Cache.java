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
package bdv.img.cache;

/**
 * This is the part of the {@link VolatileGlobalCellCache} interface that is
 * exposed to the renderer directly (that is, not via images). It comprises
 * methods to control cache behavior. If the renderer is used without
 * {@link VolatileGlobalCellCache}, these can be simply implemented to do
 * nothing.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface Cache
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
	 * (Re-)initialize the IO time budget.
	 */
	public void initIoTimeBudget( final long[] partialBudget );

	/**
	 * Get the {@link CacheIoTiming} that provides per thread-group IO
	 * statistics and budget.
	 */
	public CacheIoTiming getCacheIoTiming();

	public static class Dummy implements Cache
	{
		private CacheIoTiming cacheIoTiming;

		@Override
		public void prepareNextFrame()
		{}

		@Override
		public void initIoTimeBudget( final long[] partialBudget )
		{}

		@Override
		public CacheIoTiming getCacheIoTiming()
		{
			if ( cacheIoTiming == null )
				cacheIoTiming = new CacheIoTiming();
			return cacheIoTiming;
		}
	}
}
