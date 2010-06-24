package org.smallmind.quorum.cache.indigenous;

public interface OrderedCacheEntry<D extends CacheMetaData, V> extends CacheEntry<V> {

   public D getCacheMetaData ();
}
