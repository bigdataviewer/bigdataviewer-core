package bdv.cache;

/**
 * A cache entry associating a key to a value that maybe invalid (usually a {@link VolatileCacheValue}).
 * Using {@link #loadIfNotValid()}, the value can be made valid (or replaced by a valid value).
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheEntry
{
	/**
	 * If this entry's value is not currently valid, then load it. After the
	 * method returns, the value is guaranteed to be valid.
	 * <p>
	 * This must be implemented in a thread-safe manner. Multiple threads are
	 * allowed to call this method at the same time. The expected behaviour is
	 * that the value is loaded only once and the result is visible on all
	 * threads.
	 * <p>
	 * Note, that loading may be implemented either as
	 * <ol>
	 * <li>modify the existing value and change its state to valid, or</li>
	 * <li>replace the existing value by a valid one (this is done in
	 * BigDataViewer).</li>
	 * </ol>
	 */
	public void loadIfNotValid() throws InterruptedException;
}
