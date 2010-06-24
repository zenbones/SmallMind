package org.smallmind.quorum.cache.indigenous;

public class CacheReference<K, E extends CacheEntry> {

   private K key;
   private E cacheEntry;

   public CacheReference (K key, E cacheEntry) {

      this.key = key;
      this.cacheEntry = cacheEntry;
   }

   public K getKey () {

      return key;
   }

   public E getCacheEntry () {

      return cacheEntry;
   }
}
