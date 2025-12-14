/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.memcached.utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link ProxyMemcachedClient} for testing without a server.
 */
public class InMemoryMemcachedClient implements ProxyMemcachedClient {

  private final HashMap<String, Holder<?>> internalMap = new HashMap<>();
  private final AtomicLong counter = new AtomicLong(0);

  /**
   * Returns the default timeout for in-memory operations.
   *
   * <p>In-memory calls are immediate, but we mirror the real client behavior by exposing a timeout.</p>
   *
   * @return default timeout in milliseconds
   */
  public long getDefaultTimeout () {

    return 5000L;
  }

  /**
   * Wraps a value and cas into an in-memory {@link ProxyCASResponse}.
   *
   * @param cas   compare-and-swap token
   * @param value cached value
   * @return in-memory CAS response wrapper
   */
  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new InMemoryCASResponse<>(cas, value);
  }

  /**
   * Retrieves a value for the given key if it exists and is not expired.
   *
   * @param key cache key
   * @param <T> expected value type
   * @return cached value or {@code null} if missing or expired
   */
  @Override
  public synchronized <T> T get (String key) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return holder.getValue();
  }

  /**
   * Retrieves multiple values, skipping missing or expired entries.
   *
   * @param keys collection of cache keys
   * @param <T>  expected value type
   * @return map of keys to values for entries that were found
   */
  @Override
  public synchronized <T> Map<String, T> get (Collection<String> keys) {

    Map<String, T> resultMap = new HashMap<>();

    for (String key : keys) {

      T result;

      if ((result = get(key)) != null) {
        resultMap.put(key, result);
      }
    }

    return resultMap;
  }

  /**
   * Retrieves a value and its CAS token if present and unexpired.
   *
   * @param key cache key
   * @param <T> expected value type
   * @return CAS response or {@code null} when absent or expired
   */
  @Override
  public synchronized <T> ProxyCASResponse<T> casGet (String key) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return new InMemoryCASResponse<T>(holder.getCas(), holder.getValue());
  }

  /**
   * Stores a value with an expiration, overwriting any existing entry.
   *
   * @param key        cache key
   * @param expiration expiration in seconds (0 for no expiry)
   * @param value      value to store
   * @param <T>        value type
   * @return {@code true} always (matches client contract)
   */
  @Override
  public synchronized <T> boolean set (String key, int expiration, T value) {

    internalMap.put(key, new Holder<>(expiration, value));

    return true;
  }

  /**
   * Stores a value only if the supplied CAS matches the current entry.
   *
   * @param key        cache key
   * @param expiration expiration in seconds (0 for no expiry)
   * @param value      value to store
   * @param cas        expected CAS token
   * @param <T>        value type
   * @return {@code true} on insert or successful CAS update; {@code false} on CAS mismatch
   */
  @Override
  public synchronized <T> boolean casSet (String key, int expiration, T value, long cas) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {
      internalMap.put(key, new Holder(expiration, value));

      return true;
    } else if (cas == holder.getCas()) {
      internalMap.put(key, new Holder<>(expiration, value));

      return true;
    }

    return false;
  }

  /**
   * Unconditionally removes a key.
   *
   * @param key cache key
   * @return {@code true} always (matches client contract)
   */
  @Override
  public synchronized boolean delete (String key) {

    internalMap.remove(key);

    return true;
  }

  /**
   * Removes a key only if the supplied CAS matches the stored entry.
   *
   * @param key cache key
   * @param cas expected CAS token
   * @return {@code true} on successful removal or when already absent/expired; {@code false} on CAS mismatch
   */
  @Override
  public synchronized boolean casDelete (String key, long cas) {

    Holder holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {

      return true;
    } else if (cas == holder.getCas()) {
      internalMap.remove(key);

      return true;
    }

    return false;
  }

  /**
   * Updates the expiration for an existing, unexpired entry.
   *
   * @param key        cache key
   * @param expiration new expiration in seconds
   * @return {@code true} if touched; {@code false} if missing or expired
   */
  @Override
  public synchronized boolean touch (String key, int expiration) {

    Holder<?> holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {

      return false;
    }

    holder.touch(expiration);

    return true;
  }

  /**
   * Retrieves a value and updates its expiration in a single operation.
   *
   * @param key        cache key
   * @param expiration new expiration in seconds
   * @param <T>        value type
   * @return cached value or {@code null} if missing or expired
   */
  @Override
  public synchronized <T> T getAndTouch (String key, int expiration) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    holder.touch(expiration);

    return holder.getValue();
  }

  /**
   * Clears all stored entries.
   */
  @Override
  public void clear () {

    internalMap.clear();
  }

  /**
   * Shuts down the client. No-op for the in-memory implementation.
   */
  @Override
  public void shutdown () {

  }

  private class Holder<T> {

    private final T value;
    private final long cas;
    private long creation;
    private int expiration;

    /**
     * Creates a holder for a value with an expiration policy.
     *
     * @param expiration expiration in seconds (0 for no expiry)
     * @param value      value to store
     */
    public Holder (int expiration, T value) {

      if (expiration < 0) {
        throw new IllegalArgumentException();
      }

      this.expiration = expiration;
      this.value = value;

      cas = counter.incrementAndGet();
      creation = System.currentTimeMillis();
    }

    /**
     * Returns the stored value.
     *
     * @return cached value
     */
    public T getValue () {

      return value;
    }

    /**
     * Returns the CAS token associated with this value.
     *
     * @return CAS token
     */
    public long getCas () {

      return cas;
    }

    /**
     * Updates the expiration and refreshes the creation time.
     *
     * @param expiration new expiration in seconds
     */
    public void touch (int expiration) {

      this.expiration = expiration;

      creation = System.currentTimeMillis();
    }

    /**
     * Determines whether the entry has expired.
     *
     * @return {@code true} if expired; otherwise {@code false}
     */
    public boolean isExpired () {

      return (expiration > 0) && System.currentTimeMillis() >= creation + (expiration * 1000);
    }
  }
}
