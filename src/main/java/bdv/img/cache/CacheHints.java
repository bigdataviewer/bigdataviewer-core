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

import bdv.img.cache.VolatileGlobalCellCache.VolatileCellCache;

/**
 * Hints to the {@link VolatileCellCache cache} on how to handle cell requests.
 * Consists of the {@link LoadingStrategy} for cells, the priority with which to
 * enqueue cells to the {@link BlockingFetchQueues} (if they are enqueued) and
 * whether they should be enqueued to the front (most recent requests are
 * handled first) or back (requests are handled in order) of the respective
 * priority level.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class CacheHints
{
	private final LoadingStrategy loadingStrategy;

	private final int queuePriority;

	private final boolean enqueuToFront;

	/**
	 *
	 * @param loadingStrategy
	 * @param queuePriority
	 * @param enqueuToFront
	 */
	public CacheHints( final LoadingStrategy loadingStrategy, final int queuePriority, final boolean enqueuToFront )
	{
		this.loadingStrategy = loadingStrategy;
		this.queuePriority = queuePriority;
		this.enqueuToFront = enqueuToFront;

	}

	/**
	 * Get the {@link LoadingStrategy} to use when accessing data that is not in
	 * the cache yet.
	 *
	 * @return {@link LoadingStrategy} to use when accessing data that is not in
	 *         the cache yet.
	 */
	public LoadingStrategy getLoadingStrategy()
	{
		return loadingStrategy;
	}

	/**
	 * Get the priority with which cell requests from this
	 * {@link VolatileCellCache cache} are enqueud if they are enqueud.
	 *
	 * @return the priority with which requests are enqueued
	 */
	public int getQueuePriority()
	{
		return queuePriority;
	}

	/**
	 * Return true if cell requests should be enqueued to the front (most recent
	 * requests are handled first) of the respective {@link #getQueuePriority()
	 * priority level}. Return false, if cell requests should be enqueued to the
	 * back (requests are handled in order).
	 *
	 * @return true if request should be added to the front of the queue, false
	 *         if they should be added to the back
	 */
	public boolean isEnqueuToFront()
	{
		return enqueuToFront;
	}
}
