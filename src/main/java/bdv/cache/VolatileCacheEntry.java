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
	public K getKey();

	public V getValue();

	public long getEnqueueFrame();

	public void setEnqueueFrame( long f );

	public void loadIfNotValid() throws InterruptedException;
}
