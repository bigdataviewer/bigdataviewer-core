package bdv.cache.util;

/**
 * Loads something.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface Loader
{
	/**
	 * Load something.
	 *
	 * @throws InterruptedException
	 *             if the loading operation was interrupted.
	 */
	public void load() throws InterruptedException;
}