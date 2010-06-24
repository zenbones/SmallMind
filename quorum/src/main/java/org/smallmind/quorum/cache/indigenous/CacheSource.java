package org.smallmind.quorum.cache.indigenous;

import org.smallmind.quorum.cache.CacheException;

public interface CacheSource<K, V, E extends CacheEntry<V>> {

   public abstract E createEntry (K key, Object... parameters)
      throws CacheException;

   public abstract CacheReference<K, E> wrapReference (K key, V value)
      throws CacheException;
}
