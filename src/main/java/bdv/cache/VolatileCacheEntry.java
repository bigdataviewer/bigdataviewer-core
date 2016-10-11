package bdv.cache;

/**
 * A {@link VolatileCache} entry, comprising a key/value pair.
 *
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheEntry< K, V extends VolatileCacheValue >
{
	/**
	 * If this entry's value is not currently
	 * {@link VolatileCacheValue#isValid() valid}, then load it. After the
	 * method returns, the value is guaranteed to be
	 * {@link VolatileCacheValue#isValid() valid}.
	 * <p>
	 * This must be implemented in a thread-safe manner. Multiple threads are
	 * allowed to call this method at the same time. The expected behaviour is
	 * that the value is loaded only once and the result is visible on all
	 * threads.
	 */
	public void loadIfNotValid() throws InterruptedException;
}
