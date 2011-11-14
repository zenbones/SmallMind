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
package org.smallmind.persistence.cache.praxis.concurrent.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.PersistenceCache;

public class EhcacheCache<V> implements PersistenceCache<String, V> {

  private static final CASValue EMPTY_CAS_VALUE = new CASValue<Object>(null, 0);

  private Cache ehCache;
  private Class<V> valueClass;

  public EhcacheCache (Cache ehCache, Class<V> valueClass) {

    this.ehCache = ehCache;
    this.valueClass = valueClass;
  }

  private Element createElement (String key, V value, long timeToLiveMilliseconds) {

    return new Element(key, value, false, (int)ehCache.getCacheConfiguration().getTimeToIdleSeconds(), (int)((timeToLiveMilliseconds <= 0) ? ehCache.getCacheConfiguration().getTimeToLiveSeconds() : timeToLiveMilliseconds / 1000));
  }

  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return true;
  }

  @Override
  public long getDefaultTimeToLiveMilliseconds () {

    return ehCache.getCacheConfiguration().getTimeToLiveSeconds() * 1000;
  }

  @Override
  public V get (String key) throws CacheOperationException {

    return valueClass.cast(ehCache.get(key).getValue());
  }

  @Override
  public void set (String key, V value, long timeToLiveMilliseconds) {

    ehCache.put(createElement(key, value, timeToLiveMilliseconds));
  }

  @Override
  public V putIfAbsent (String key, V value, long timeToLiveMilliseconds) {

    return valueClass.cast(ehCache.putIfAbsent(createElement(key, value, timeToLiveMilliseconds)).getValue());
  }

  @Override
  public CASValue<V> getViaCas (String key) {

    Element element;

    if ((element = ehCache.get(key)) != null) {
      return new CASValue<V>(valueClass.cast(element.getValue()), element.getVersion());
    }

    return EMPTY_CAS_VALUE;
  }

  @Override
  public boolean putViaCas (String key, V oldValue, V value, long version, long timeToLiveMilliseconds) throws CacheOperationException {

    if (oldValue == null) {

      return ehCache.putIfAbsent(createElement(key, value, timeToLiveMilliseconds)) == null;
    }
    else {

      return ehCache.replace(createElement(key, oldValue, timeToLiveMilliseconds), createElement(key, value, timeToLiveMilliseconds));
    }
  }

  @Override
  public void remove (String key) {

    ehCache.remove(key);
  }
}
