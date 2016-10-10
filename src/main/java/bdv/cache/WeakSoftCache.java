package bdv.cache;

// TODO: rename, refactor, document
public interface WeakSoftCache
{
	public < K, V > void putWeak( final K key, final V value ); // V == Entry

	public < K, V > void putSoft( final K key, final V value ); // V == Entry

	public < K, V > V get( final K key ); // V == Entry

	public void finalizeRemovedCacheEntries(); // TODO: rename to cleanUp() ?

	public void clearCache();
}
