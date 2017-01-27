package bdv.cache.revised;

import java.util.concurrent.ExecutionException;

import bdv.cache.CacheHints;

public interface VolatileCache< K, V >
{
	/**
	 * Returns the value associated with {@code key} in this cache, or
	 * {@code null} if there is no cached value for {@code key}.
	 */
	V getIfPresent( Object key, final CacheHints cacheHints ) throws ExecutionException;

	/**
	 * Returns the value associated with {@code key} in this cache, atomically
	 * obtaining that value from {@code loader} if necessary. No observable
	 * state associated with this cache is modified until loading completes.
	 * This method provides a simple substitute for the conventional
	 * "if cached, return; otherwise create, cache and return" pattern.
	 *
	 * @throws ExecutionException
	 *             if a checked exception was thrown while loading the value
	 */
	V get( K key, VolatileLoader< ? extends V > loader, final CacheHints cacheHints ) throws ExecutionException;

	void invalidateAll();

//	void cleanUp();
//	void invalidate( Object key );
//	long size();
}
