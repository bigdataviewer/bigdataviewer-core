package bdv.cache;

public interface VolatileCacheEntry< K, V extends VolatileCacheValue >
{
	public K getKey();

	public V getValue();

	public long getEnqueueFrame();

	public void setEnqueueFrame( long f );

	public void loadIfNotValid() throws InterruptedException;
}
