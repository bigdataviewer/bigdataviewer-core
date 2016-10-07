package bdv.cache;

/**
 * Value (or placeholder for value) that is stored in a cache and may be
 * initially invalid. The value can only go from invalid state to valid, never
 * vice-versa.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheValue
{
	/**
	 * Check whether the value is currently valid. The value can only go from
	 * invalid state to valid, never vice-versa.
	 *
	 * @return whether the value is valid.
	 */
	public boolean isValid();
}
