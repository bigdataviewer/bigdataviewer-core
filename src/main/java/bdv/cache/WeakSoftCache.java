package bdv.cache;

// TODO: rename, refactor, document
public interface WeakSoftCache< K, V >
{
	public void putWeak( final K key, final V value );

	public void putSoft( final K key, final V value );

	public V get( final Object key );

	/**
	 * Performs pending maintenance operations needed by the cache. Exactly
	 * which activities are performed is implementation-dependent. This should
	 * be called periodically
	 */
	public void cleanUp();

	/**
	 * Discards all entries in the cache.
	 */
	void invalidateAll();

	/**
	 * Create a new {@link WeakSoftCache}.
	 * <p>
	 * This is here so we can swap out implementations easily and will probably
	 * be replaced by a scijava service later.
	 */
	public static < K, V > WeakSoftCache< K, V > newInstance()
	{
		return new WeakSoftCacheImp<>();
	}
}
