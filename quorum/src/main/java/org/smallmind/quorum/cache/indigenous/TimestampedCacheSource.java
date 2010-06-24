package org.smallmind.quorum.cache.indigenous;

import org.smallmind.quorum.cache.CacheException;

public abstract class TimestampedCacheSource<K, V> implements CacheSource<K, V, TimestampOrderedCacheEntry<V>> {

   public abstract V obtainValue (K key, Object... parameters)
      throws Exception;

   public TimestampOrderedCacheEntry<V> createEntry (K key, Object... parameters)
      throws CacheException {

      V value;

      try {
         if ((value = obtainValue(key, parameters)) == null) {
            return null;
         }

         return new TimestampOrderedCacheEntry<V>(value);
      }
      catch (Exception exception) {
         throw new CacheException(exception);
      }
   }

   public CacheReference<K, TimestampOrderedCacheEntry<V>> wrapReference (K key, V value)
      throws CacheException {

      return new CacheReference<K, TimestampOrderedCacheEntry<V>>(key, new TimestampOrderedCacheEntry<V>(value));
   }
}