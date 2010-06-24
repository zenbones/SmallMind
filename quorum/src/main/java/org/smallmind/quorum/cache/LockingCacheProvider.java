package org.smallmind.quorum.cache;

public interface LockingCacheProvider {

  public abstract <K, V> LockingCache<K, V> createLockingCache (String name);
}