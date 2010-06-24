package org.smallmind.quorum.cache.indigenous;

public abstract class AbstractTimestampOrderedCacheEntry<V> implements OrderedCacheEntry<TimestampedCacheMetaData, V> {

   private V value;
   private TimestampedCacheMetaData metaData;

   public AbstractTimestampOrderedCacheEntry (V value) {

      this.value = value;

      metaData = new TimestampedCacheMetaData();
   }

   public abstract void cacheHit ();

   public abstract void expire ();

   public abstract void close ();

   public TimestampedCacheMetaData getCacheMetaData () {

      return metaData;
   }

   public V getEntry () {

      return value;
   }
}
