/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
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
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.PersistenceCache;

public class MemcachedCache<V> implements PersistenceCache<String, V> {

  private MemcachedClient memcachedClient;
  private Class<V> valueClass;
  private String discriminator;
  private int timeToLiveSeconds;

  public MemcachedCache (MemcachedClient memcachedClient, String discriminator, Class<V> valueClass, int timeToLiveSeconds) {

    this.valueClass = valueClass;
    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public MemcachedClient getMemcachedClient () {

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
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public Map<String, V> get (String[] keys)
    throws CacheOperationException {

    try {

      return memcachedClient.get(Arrays.asList(keys));
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void set (String key, V value, int timeToLiveSeconds) {

    try {
      memcachedClient.set(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value);
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public V putIfAbsent (String key, V value, int timeToLiveSeconds) {

    try {

      GetsResponse<V> getsResponse;
      String discriminatedKey = getDiscriminatedKey(key);

      if (((getsResponse = memcachedClient.gets(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

        return getsResponse.getValue();
      }

      while (!memcachedClient.cas(discriminatedKey, (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, 0)) {
        if (((getsResponse = memcachedClient.gets(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

          return getsResponse.getValue();
        }
      }

      return null;
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public CASValue<V> getViaCas (String key) {

    try {
      GetsResponse<V> getsResponse;

      if ((getsResponse = memcachedClient.gets(getDiscriminatedKey(key))) == null) {

        return CASValue.nullInstance();
      }

      return new CASValue<V>(getsResponse.getValue(), getsResponse.getCas());
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public boolean putViaCas (String key, V oldValue, V value, long version, int timeToLiveSeconds) {

    try {

      return memcachedClient.cas(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, version);
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void remove (String key) {

    try {
      memcachedClient.delete(getDiscriminatedKey(key));
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  private String getDiscriminatedKey (String key) {

    return new StringBuilder(discriminator).append('[').append(key).append(']').toString();
  }
}
