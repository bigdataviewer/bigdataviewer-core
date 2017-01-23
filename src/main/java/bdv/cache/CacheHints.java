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

import bdv.cache.util.BlockingFetchQueues;

/**
 * Hints to a {@link LoadingVolatileCache} on how to handle data requests.
 * Consists of the {@link LoadingStrategy} for entries, the priority with which
 * to enqueue entries to the {@link BlockingFetchQueues} (if they are enqueued)
 * and whether they should be enqueued to the front (most recent requests are
 * handled first) or back (requests are handled in order) of the respective
 * priority level.
 * <p>
 * The number of priority levels <em>n</em> is fixed when the
 * {@link LoadingVolatileCache} is constructed. Priorities are consecutive
 * integers <em>0 ... n-1</em>, where 0 is the highest priority.
 * <p>
 * In BigDataViewer, priorities usually correspond to resolution levels in some
 * way. For example this can be used to load low-resolution data first.
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

		int value = loadingStrategy.hashCode();
		value = 31 * value + queuePriority;
		value = 31 * value + Boolean.hashCode( enqueuToFront );
		hashcode = value;
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
	 * Get the priority with which entry requests are enqueued (if they are
	 * enqueued).
	 *
	 * @return the priority with which requests are enqueued. lower values mean
	 *         higher priority.
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


	@Override
	public boolean equals( final Object other )
	{
		if ( this == other )
			return true;
		if ( !( other instanceof CacheHints ) )
			return false;
		final CacheHints that = ( CacheHints ) other;
		return ( this.loadingStrategy == that.loadingStrategy ) && ( this.queuePriority == that.queuePriority ) && ( this.enqueuToFront == that.enqueuToFront );
	}

	private final int hashcode;

	@Override
	public int hashCode()
	{
		return hashcode;
	}

	@Override
	public String toString()
	{
		return "(" + loadingStrategy + ", " + queuePriority + ", " + Boolean.toString( enqueuToFront ) + ")";
	}
}
