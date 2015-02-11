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
 * @author Tobias Pietzsch
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
