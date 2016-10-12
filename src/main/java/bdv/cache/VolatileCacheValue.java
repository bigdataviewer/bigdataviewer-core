package bdv.cache;

/**
 * Value that may be valid or invalid.
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
