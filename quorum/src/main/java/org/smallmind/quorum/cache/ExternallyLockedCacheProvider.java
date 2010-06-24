package org.smallmind.quorum.cache;

public interface ExternallyLockedCacheProvider {

  public abstract <K, V> ExternallyLockedCache<K, V> createExternallyLockedCache (String name);
}