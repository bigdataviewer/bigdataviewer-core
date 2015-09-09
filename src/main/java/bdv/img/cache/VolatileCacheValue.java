package bdv.img.cache;

/**
 * Value (or placeholder for value) that is stored in a cache may be initially
 * invalid.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public interface VolatileCacheValue
{
	public boolean isValid();
}