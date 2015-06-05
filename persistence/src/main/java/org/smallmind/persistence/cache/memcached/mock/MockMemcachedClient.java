package org.smallmind.persistence.cache.memcached.mock;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MockMemcachedClient<T> {

  private HashMap<String, Holder<T>> internalMap = new HashMap<String, Holder<T>>();
  private AtomicLong counter = new AtomicLong(0);

  public synchronized T get (String key) {

    Holder<T> holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return holder.getValue();
  }

  public synchronized MockGetsResponse<T> gets (String key) {

    Holder<T> holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return new MockGetsResponse<T>(holder.getValue(), holder.getCas());
  }

  public synchronized boolean cas (String key, int expiration, T value, long cas) {

    Holder<T> holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {
      internalMap.put(key, new Holder<T>(expiration, value));

      return true;
    }
    else if (cas == holder.getCas()) {
      internalMap.put(key, new Holder<T>(expiration, value));

      return true;
    }

    return false;
  }

  private class Holder<T> {

    private T value;
    private long cas;
    private long creation;
    private int expiration;

    public Holder (int expiration, T value) {

      if (expiration < 0) {
        throw new IllegalArgumentException();
      }

      this.expiration = expiration;
      this.value = value;

      cas = counter.incrementAndGet();
      creation = System.currentTimeMillis();
    }

    public T getValue () {

      return value;
    }

    public long getCas () {

      return cas;
    }

    public boolean isExpired () {

      return (expiration > 0) && System.currentTimeMillis() >= creation + (expiration * 1000);
    }
  }
}
