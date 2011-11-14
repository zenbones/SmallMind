/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.praxis.distributed.memcached;

import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.PersistenceCache;

public class MemcachedCache<V> implements PersistenceCache<String, V> {

  private MemcachedClient memcachedClient;
  private Class<V> valueClass;
  private long timeToLiveMilliseconds;

  public MemcachedCache (MemcachedClient memcachedClient, Class<V> valueClass, long timeToLiveMilliseconds) {

    this.valueClass = valueClass;
    this.memcachedClient = memcachedClient;
    this.timeToLiveMilliseconds = timeToLiveMilliseconds;
  }

  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return false;
  }

  @Override
  public long getDefaultTimeToLiveMilliseconds () {

    return timeToLiveMilliseconds;
  }

  @Override
  public V get (String key)
    throws CacheOperationException {

    try {

      return valueClass.cast(memcachedClient.get(key));
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void set (String key, V value, long timeToLiveMilliseconds) {

    try {
      memcachedClient.set(key, (int)(((timeToLiveMilliseconds <= 0) ? getDefaultTimeToLiveMilliseconds() : timeToLiveMilliseconds) / 1000), value);
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public V putIfAbsent (String key, V value, long timeToLiveMilliseconds) {

    try {

      GetsResponse<V> getsResponse;

      if ((getsResponse = memcachedClient.gets(key)).getValue() != null) {

        return getsResponse.getValue();
      }

      while (!memcachedClient.cas(key, (int)(((timeToLiveMilliseconds <= 0) ? getDefaultTimeToLiveMilliseconds() : timeToLiveMilliseconds) / 1000), value, getsResponse.getCas())) {
        if ((getsResponse = memcachedClient.gets(key)).getValue() != null) {

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

      GetsResponse<V> getsResponse = memcachedClient.gets(key);

      return new CASValue<V>(getsResponse.getValue(), getsResponse.getCas());
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public boolean putViaCas (String key, V oldValue, V value, long version, long timeToLiveMilliseconds) {

    try {

      return memcachedClient.cas(key, (int)(((timeToLiveMilliseconds <= 0) ? getDefaultTimeToLiveMilliseconds() : timeToLiveMilliseconds) / 1000), value, version);
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  @Override
  public void remove (String key) {

    try {
      memcachedClient.delete(key);
    }
    catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }
}
