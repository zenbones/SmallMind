package org.smallmind.quorum.cache;

public interface LockingCacheManager<K, V> {

  public abstract LockingCache<K, V> getLockingCache (String name);
}