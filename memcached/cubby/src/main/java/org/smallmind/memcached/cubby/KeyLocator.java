package org.smallmind.memcached.cubby;

public interface KeyLocator {

  MemcachedHost find (ServerPool serverPool, String key);
}
