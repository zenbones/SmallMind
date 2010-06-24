package org.smallmind.quorum.cache;

public interface CacheManager<K, V> {

  public abstract Cache<K, V> getCache (String name);
}
