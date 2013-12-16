package viewer.hdf5.img;


public interface Cache
{
	/**
	 * TODO
	 */
	public void clearQueue();

	/**
	 * TODO
	 */
	public void initIoTimeBudget( final long[] partialBudget, final boolean reinitialize );

	/**
	 * TODO
	 */
	public ThreadManager getThreadManager();
}
