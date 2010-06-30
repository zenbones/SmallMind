package org.smallmind.quorum.cache;

public interface LockingCacheProvider extends CacheProvider {

   public abstract <K, V> LockingCache<K, V> getLockingCache (String instance, String region);
}