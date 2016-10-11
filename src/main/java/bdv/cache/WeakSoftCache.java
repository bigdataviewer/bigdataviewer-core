package bdv.cache;

// TODO: rename, refactor, document
public interface WeakSoftCache< K, V >
{
	public void putWeak( final K key, final V value );

	public void putSoft( final K key, final V value );

	public V get( final Object key );

	public void finalizeRemovedCacheEntries(); // TODO: rename to cleanUp() ?

	public void clearCache();

	public static < K, V > WeakSoftCache< K, V > getInstance()
	{
		return new WeakSoftCacheImp<>();
	}
}
