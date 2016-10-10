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
	// TODO: this is all just used from LoadingVolatileCache. shouldn't be here in this generic package
	public K getKey();

	// TODO: this is all just used from LoadingVolatileCache. shouldn't be here in this generic package
	public V getValue();

	// TODO: this is all just used from LoadingVolatileCache. shouldn't be here in this generic package
	public long getEnqueueFrame();

	// TODO: this is all just used from LoadingVolatileCache. shouldn't be here in this generic package
	public void setEnqueueFrame( long f );

	// TODO: this is all just used from LoadingVolatileCache. shouldn't be here in this generic package
	public void loadIfNotValid() throws InterruptedException;
}
