/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class InMemoryMemcachedClient implements ProxyMemcachedClient {

  private final HashMap<String, Holder<?>> internalMap = new HashMap<>();
  private final AtomicLong counter = new AtomicLong(0);

  public long getDefaultTimeout () {

    return 5000L;
  }

  @Override
  public <T> ProxyCASResponse<T> createCASResponse (long cas, T value) {

    return new InMemoryCASResponse<>(cas, value);
  }

  @Override
  public synchronized <T> T get (String key) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return holder.getValue();
  }

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

  @Override
  public synchronized <T> ProxyCASResponse<T> casGet (String key) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    return new InMemoryCASResponse<T>(holder.getCas(), holder.getValue());
  }

  @Override
  public synchronized <T> boolean set (String key, int expiration, T value) {

    internalMap.put(key, new Holder<>(expiration, value));

    return true;
  }

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

  @Override
  public synchronized boolean delete (String key) {

    internalMap.remove(key);

    return true;
  }

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

  @Override
  public synchronized boolean touch (String key, int expiration) {

    Holder<?> holder;

    if (((holder = internalMap.get(key)) == null) || holder.isExpired()) {

      return false;
    }

    holder.touch(expiration);

    return true;
  }

  @Override
  public synchronized <T> T getAndTouch (String key, int expiration) {

    Holder<T> holder;

    if (((holder = (Holder<T>)internalMap.get(key)) == null) || holder.isExpired()) {

      return null;
    }

    holder.touch(expiration);

    return holder.getValue();
  }

  @Override
  public void clear () {

    internalMap.clear();
  }

  @Override
  public void shutdown () {

  }

  private class Holder<T> {

    private final T value;
    private final long cas;
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

    public void touch (int expiration) {

      this.expiration = expiration;

      creation = System.currentTimeMillis();
    }

    public boolean isExpired () {

      return (expiration > 0) && System.currentTimeMillis() >= creation + (expiration * 1000);
    }
  }
}
