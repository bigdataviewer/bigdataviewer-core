package bdv.img.cache;

import bdv.img.cache.CacheIoTiming.IoTimeBudget;

/**
 * Describes how the cache processes requests for cells with missing data.
 *
 * Depending on the {@link LoadingStrategy} the following actions are performed
 * if the cell data has not been loaded yet:
 * <ul>
 *   <li> {@link LoadingStrategy#VOLATILE}:
 *        Enqueue the cell for asynchronous loading by a fetcher thread.
 *   <li> {@link LoadingStrategy#BLOCKING}:
 *        Load the cell data immediately.
 *   <li> {@link LoadingStrategy#BUDGETED}:
 *        Load the cell data immediately if there is enough {@link IoTimeBudget}
 *        left for the current thread group. Otherwise enqueue the cell for
 *        asynchronous loading by a fetcher thread.
 *   <li> {@link LoadingStrategy#DONTLOAD}:
 *        Do nothing.
 * </ul>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public enum LoadingStrategy
{
	VOLATILE,
	BLOCKING,
	BUDGETED,
	DONTLOAD
}
