package bdv.img.cache;


/**
 * This is the part of the {@link VolatileGlobalCellCache} interface that is exposed
 * to the renderer directly (that is, not via images). It comprises methods to
 * control cache behavior. If the renderer is used without
 * {@link VolatileGlobalCellCache}, these can be simply implemented to do nothing,
 * or return null, respectively.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
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
	public void initIoTimeBudget( final long[] partialBudget, final boolean reinitialize );

	/**
	 * @return the {@link ThreadManager} for the cache's fetcher threads.
	 */
	public ThreadManager getThreadManager();
}
