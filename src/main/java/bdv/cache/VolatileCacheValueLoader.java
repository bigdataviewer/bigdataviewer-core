package bdv.cache;

/**
 * Loader that can load a specific value, and also create an create an
 * {@link VolatileCacheValue#isValid() invalid} placeholder for it.
 *
 * @param <V>
 *            value type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheValueLoader< V extends VolatileCacheValue >
{
	/**
	 * Create an empty, that is, invalid (see
	 * {@link VolatileCacheValue#isValid()}) value.
	 *
	 * @return an empty placeholder value
	 */
	public V createEmptyValue();

	/**
	 * Load the value. The returned value is {@link VolatileCacheValue#isValid()
	 * valid}.
	 *
	 * @return the loaded value
	 * @throws InterruptedException
	 */
	public V load() throws InterruptedException;
}
