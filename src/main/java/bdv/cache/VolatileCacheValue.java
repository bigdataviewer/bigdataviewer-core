package bdv.cache;

/**
 * Value (or placeholder for value) that is stored in a
 * {@link Loadable} and may be invalid. The value associated with a
 * {@link Loadable} can only go from invalid state to valid, never
 * vice-versa.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheValue
{
	/**
	 * Check whether this value is currently valid.
	 *
	 * @return whether the value is valid.
	 */
	public boolean isValid();
}
