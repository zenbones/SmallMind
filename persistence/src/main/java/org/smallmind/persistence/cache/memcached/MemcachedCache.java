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
package org.smallmind.persistence.cache.memcached;

import java.util.Arrays;
import java.util.Map;
import org.smallmind.memcached.ProxyCASResponse;
import org.smallmind.memcached.ProxyMemcachedClient;
import org.smallmind.persistence.cache.CASSupportingPersistenceCache;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheOperationException;

public class MemcachedCache<V> implements CASSupportingPersistenceCache<String, V> {

  private ProxyMemcachedClient memcachedClient;
  private Class<V> valueClass;
  private String discriminator;
  private int timeToLiveSeconds;

  public MemcachedCache (ProxyMemcachedClient memcachedClient, String discriminator, Class<V> valueClass, int timeToLiveSeconds) {

    this.valueClass = valueClass;
    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public ProxyMemcachedClient getMemcachedClient () {

    return memcachedClient;
  }

  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return false;
  }

  @Override
  public int getDefaultTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  @Override
  public V get (String key)
    throws CacheOperationException {

    try {

      return valueClass.cast(memcachedClient.get(getDiscriminatedKey(key)));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public Map<String, V> get (String[] keys)
    throws CacheOperationException {

    String[] discriminatedKeys = new String[keys.length];

    for (int index = 0; index < keys.length; index++) {
      discriminatedKeys[index] = getDiscriminatedKey(keys[index]);
    }

    try {

      return memcachedClient.get(Arrays.asList(discriminatedKeys));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void set (String key, V value, int timeToLiveSeconds) {

    try {
      memcachedClient.set(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value);
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public V putIfAbsent (String key, V value, int timeToLiveSeconds) {

    try {

      ProxyCASResponse<V> getsResponse;
      String discriminatedKey = getDiscriminatedKey(key);

      if (((getsResponse = memcachedClient.casGet(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

        return getsResponse.getValue();
      }

      while (!memcachedClient.casSet(discriminatedKey, (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, 0)) {
        if (((getsResponse = memcachedClient.casGet(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

          return getsResponse.getValue();
        }
      }

      return null;
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public CASValue<V> getViaCas (String key) {

    try {
      ProxyCASResponse<V> getsResponse;

      if ((getsResponse = memcachedClient.casGet(getDiscriminatedKey(key))) == null) {

        return CASValue.nullInstance();
      }

      return new CASValue<V>(getsResponse.getValue(), getsResponse.getCas());
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public boolean putViaCas (String key, V oldValue, V value, long version, int timeToLiveSeconds) {

    try {

      return memcachedClient.casSet(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, version);
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void remove (String key) {

    try {
      memcachedClient.delete(getDiscriminatedKey(key));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  private String getDiscriminatedKey (String key) {

    return new StringBuilder(discriminator).append('[').append(key).append(']').toString();
  }
}
