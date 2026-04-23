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
 * In-process, heap-based implementation of {@link ProxyMemcachedClient} intended for unit and
 * integration testing without a real memcached server.
 *
 * <p>All state is held in a {@link HashMap} whose entries are wrapped in an inner {@link Holder}
 * that records the CAS token and expiry deadline. Expiry is evaluated lazily on each access;
 * expired entries are treated as absent but are not eagerly evicted. All mutating operations are
 * {@code synchronized} to provide thread safety consistent with what a real memcached client
 * would provide.</p>
 *
 * <p>CAS tokens are issued by a monotonically increasing {@link AtomicLong} counter so that each
 * store operation produces a unique token. This mirrors the server-assigned opaque tokens returned
 * by a real memcached deployment.</p>
 */
public class InMemoryMemcachedClient implements ProxyMemcachedClient {

  private final HashMap<String, Holder<?>> internalMap = new HashMap<>();
  private final AtomicLong counter = new AtomicLong(0);

  /**
   * Returns the default operation timeout used by this client.
   *
   * <p>Although in-memory operations complete immediately, the value mirrors the typical
   * default used by real clients so that calling code behaves consistently.</p>
   *
   * @return {@code 5000} milliseconds
   */
  public long getDefaultTimeout () {

    return 5000L;
  }

  /**
   * Creates an in-memory {@link ProxyCASResponse} wrapping the given value and CAS token.
   *
   * @param cas   the compare-and-swap token
   * @param value the cached value
   * @param <T>   the value type
   * @return an {@link InMemoryCASResponse} containing both the value and the token
   */
  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new InMemoryCASResponse<>(cas, value);
  }

  /**
   * Returns the cached value for the given key if the entry exists and has not expired.
   *
   * @param key the cache key
   * @param <T> the expected value type
   * @return the cached value, or {@code null} if absent or expired
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
   * Returns cached values for the given collection of keys, omitting absent or expired entries.
   *
   * @param keys the cache keys to look up
   * @param <T>  the expected value type
   * @return a map of keys to values; keys that are absent or expired are not included
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
   * Returns the cached value and its CAS token for the given key if the entry exists and has
   * not expired.
   *
   * @param key the cache key
   * @param <T> the expected value type
   * @return an {@link InMemoryCASResponse} with the value and token, or {@code null} if absent
   * or expired
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
   * Stores a value unconditionally under the given key, replacing any existing entry.
   *
   * @param key        the cache key
   * @param expiration the time-to-live in seconds; {@code 0} means the entry never expires
   * @param value      the value to store
   * @param <T>        the value type
   * @return {@code true} always
   */
  @Override
  public synchronized <T> boolean set (String key, int expiration, T value) {

    internalMap.put(key, new Holder<>(expiration, value));

    return true;
  }

  /**
   * Stores a value only when the key is absent (or expired) or the supplied CAS token matches
   * the current entry's token.
   *
   * @param key        the cache key
   * @param expiration the time-to-live in seconds; {@code 0} means the entry never expires
   * @param value      the value to store
   * @param cas        the CAS token that must match; pass {@code 0} to treat absent/expired as
   *                   a matching condition
   * @param <T>        the value type
   * @return {@code true} if the value was stored; {@code false} if the CAS token did not match
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
   * Removes the entry for the given key regardless of its current state.
   *
   * @param key the cache key to delete
   * @return {@code true} always
   */
  @Override
  public synchronized boolean delete (String key) {

    internalMap.remove(key);

    return true;
  }

  /**
   * Removes the entry for the given key only if its CAS token matches the supplied value.
   *
   * @param key the cache key to delete
   * @param cas the CAS token that must match the stored entry
   * @return {@code true} if the entry was deleted or was already absent/expired; {@code false}
   * if the CAS token did not match
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
   * Refreshes the expiration of an existing, unexpired entry without returning its value.
   *
   * @param key        the cache key whose TTL should be updated
   * @param expiration the new time-to-live in seconds
   * @return {@code true} if the entry existed and was touched; {@code false} if absent or expired
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
   * Returns the cached value for the given key and atomically refreshes its expiration.
   *
   * @param key        the cache key
   * @param expiration the new time-to-live in seconds
   * @param <T>        the expected value type
   * @return the cached value, or {@code null} if absent or expired
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
   * Removes all entries from the in-memory store.
   */
  @Override
  public void clear () {

    internalMap.clear();
  }

  /**
   * No-op shutdown; the in-memory client holds no external resources that require release.
   */
  @Override
  public void shutdown () {

  }

  /**
   * Internal value container that pairs a stored value with its CAS token and expiry metadata.
   *
   * @param <T> the type of the wrapped value
   */
  private class Holder<T> {

    private final T value;
    private final long cas;
    private long creation;
    private int expiration;

    /**
     * Creates a new holder for a value with the given expiration policy.
     *
     * <p>A new, unique CAS token is issued by incrementing the client-wide counter. The creation
     * timestamp is set to the current system time in milliseconds.</p>
     *
     * @param expiration the time-to-live in seconds; must be non-negative; {@code 0} means no expiry
     * @param value      the value to store
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
     * @return the cached value
     */
    public T getValue () {

      return value;
    }

    /**
     * Returns the CAS token assigned when this holder was created.
     *
     * @return the compare-and-swap token
     */
    public long getCas () {

      return cas;
    }

    /**
     * Resets the expiration of this entry by updating the TTL and refreshing the creation timestamp
     * to the current time.
     *
     * @param expiration the new time-to-live in seconds
     */
    public void touch (int expiration) {

      this.expiration = expiration;

      creation = System.currentTimeMillis();
    }

    /**
     * Determines whether this entry has passed its expiration deadline.
     *
     * <p>An entry with an expiration of {@code 0} never expires.</p>
     *
     * @return {@code true} if the entry's TTL has elapsed; {@code false} otherwise
     */
    public boolean isExpired () {

      return (expiration > 0) && System.currentTimeMillis() >= creation + (expiration * 1000);
    }
  }
}
