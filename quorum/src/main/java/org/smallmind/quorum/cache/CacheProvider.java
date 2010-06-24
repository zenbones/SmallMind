package org.smallmind.quorum.cache;

public interface CacheProvider {

  public abstract <K, V> Cache<K, V> createCache (String name);
}
