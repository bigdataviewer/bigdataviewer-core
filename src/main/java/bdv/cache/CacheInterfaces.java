package bdv.cache;

import java.util.concurrent.ExecutionException;

public class CacheInterfaces
{
	public interface CacheLoader< K, V >
	{
		V get( K key ) throws Exception;
	}

	public interface RemovalListener< K, V >
	{
		void onRemoval( K key, V value );
	}

	public interface AbstractCache< K, V >
	{
		V getIfPresent( K key );

		void invalidateAll();
//		void cleanUp();
//		void invalidate( Object key );
//		long size();
	}

	public interface Cache< K, V > extends AbstractCache< K, V >
	{
		V get(
				K key,
				CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException;
	}

	public interface LoadingCache< K, V > extends AbstractCache< K, V >, CacheLoader< K, V >
	{
		@Override
		V get( K key ) throws ExecutionException;
	}

	public interface ListenableCache< K, V > extends AbstractCache< K, V >
	{
		V get(
				K key,
				CacheLoader< ? super K, ? extends V > loader,
				RemovalListener< ? super K, ? super V > remover ) throws ExecutionException;
	}

	public interface ListenableLoadingCache< K, V > extends AbstractCache< K, V >
	{
		V get(
				K key,
				RemovalListener< ? super K, ? super V > remover ) throws ExecutionException;
	}

	/*
	 * Volatile
	 */

	public interface VolatileCacheLoader< K, V > extends CacheLoader< K, V >
	{
		public V createInvalid( K key ) throws Exception;
	}

	public interface AbstractVolatileCache< K, V >
	{
		V getIfPresent(
				K key,
				CacheHints cacheHints ) throws ExecutionException;

		void invalidateAll();
//		void cleanUp();
//		void invalidate( Object key );
//		long size();
	}

	public interface VolatileCache< K, V > extends AbstractVolatileCache< K, V >
	{
		V get(
				K key,
				VolatileCacheLoader< ? super K, ? extends V > loader,
				CacheHints cacheHints ) throws ExecutionException;
	}

	public interface VolatileLoadingCache< K, V > extends AbstractVolatileCache< K, V >
	{
		V get(
				K key,
				CacheHints cacheHints ) throws ExecutionException;
	}

	public interface VolatileListenableCache< K, V > extends AbstractVolatileCache< K, V >
	{
		V get(
				K key,
				VolatileCacheLoader< ? super K, ? extends V > loader,
				RemovalListener< ? super K, ? super V > remover,
				CacheHints cacheHints ) throws ExecutionException;
	}

	public interface VolatileListenableLoadingCache< K, V > extends VolatileListenableCache< K, V >, VolatileLoadingCache< K, V >
	{
		V get(
				K key,
				RemovalListener< ? super K, ? super V > remover,
				CacheHints cacheHints ) throws ExecutionException;
	}

	/*
	 * Adapters that translate keys.
	 */

	public interface KeyBimap< K, L >
	{
		L getTarget( K key );

		K getSource( L key );
	}

	public static class AbstractCacheAdapter< K, L, V, C extends AbstractCache< L, V > >
			implements AbstractCache< K, V >
	{
		protected final C cache;

		protected final KeyBimap< K, L > keymap;

		public AbstractCacheAdapter( final C cache, final KeyBimap< K, L > keymap )
		{
			this.cache = cache;
			this.keymap = keymap;
		}

		@Override
		public V getIfPresent( final K key )
		{
			return cache.getIfPresent( keymap.getTarget( key ) );
		}

		@Override
		public void invalidateAll()
		{
			cache.invalidateAll();
		}
	}

	public static class CacheAdapter< K, L, V, C extends Cache< L, V > >
			extends AbstractCacheAdapter< K, L, V, C >
			implements Cache< K, V >
	{
		public CacheAdapter( final C cache, final KeyBimap< K, L > keymap )
		{
			super( cache, keymap );
		}

		@Override
		public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
		{
			return cache.get(
					keymap.getTarget( key ),
					k -> loader.get( keymap.getSource( k ) ) );
		}
	}

	public static class LoadingCacheAdapter< K, L, V, C extends LoadingCache< L, V > >
			extends AbstractCacheAdapter< K, L, V, C >
			implements LoadingCache< K, V >
	{
		public LoadingCacheAdapter( final C cache, final KeyBimap< K, L > keymap )
		{
			super( cache, keymap );
		}

		@Override
		public V get( final K key ) throws ExecutionException
		{
			return cache.get( keymap.getTarget( key ) );
		}
	}

	public static class ListenableCacheAdapter< K, L, V, C extends ListenableCache< L, V > >
			extends AbstractCacheAdapter< K, L, V, C >
			implements ListenableCache< K, V >
	{
		public ListenableCacheAdapter( final C cache, final KeyBimap< K, L > keymap )
		{
			super( cache, keymap );
		}

		@Override
		public V get( final K key, final CacheLoader< ? super K, ? extends V > loader, final RemovalListener< ? super K, ? super V > remover ) throws ExecutionException
		{
			return cache.get(
					keymap.getTarget( key ),
					k -> loader.get( keymap.getSource( k ) ),
					( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
		}
	}

	public static class ListenableLoadingCacheAdapter< K, L, V, C extends ListenableLoadingCache< L, V > >
			extends AbstractCacheAdapter< K, L, V, C >
			implements ListenableLoadingCache< K, V >
	{
		public ListenableLoadingCacheAdapter( final C cache, final KeyBimap< K, L > keymap )
		{
			super( cache, keymap );
		}

		@Override
		public V get( final K key, final RemovalListener< ? super K, ? super V > remover ) throws ExecutionException
		{
			return cache.get(
					keymap.getTarget( key ),
					( k, v ) -> remover.onRemoval( keymap.getSource( k ), v ) );
		}
	}
}
