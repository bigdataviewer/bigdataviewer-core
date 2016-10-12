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

	public void clearCache();

	public static < K, V > WeakSoftCache< K, V > getInstance()
	{
		return new WeakSoftCacheImp<>();
	}
}
