package bdv.cache;

/**
 * Loader that can create {@link VolatileCacheValue}s for specified keys.
 *
 * @param <K> key type.
 * @param <V> value type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheValueLoader< K, V extends VolatileCacheValue >
{
	/**
	 * Create an empty, that is, invalid (see
	 * {@link VolatileCacheValue#isValid()}) value for the given key.
	 *
	 * @param key
	 * @return
	 */
	public V createEmptyValue( K key );

	/**
	 * Load the value for the given key. The returned value is
	 * {@link VolatileCacheValue#isValid() valid}.
	 *
	 * @param key
	 * @return
	 * @throws InterruptedException
	 */
	public V load( K key ) throws InterruptedException;
}
