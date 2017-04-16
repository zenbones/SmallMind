/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.cache.ehcache;

import java.util.HashMap;
import java.util.Map;
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

  private Element createElement (String key, V value, int timeToLiveSeconds) {

    return new Element(key, value, false, (int)ehCache.getCacheConfiguration().getTimeToIdleSeconds(), (timeToLiveSeconds <= 0) ? (int)ehCache.getCacheConfiguration().getTimeToLiveSeconds() : timeToLiveSeconds);
  }

  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return true;
  }

  @Override
  public int getDefaultTimeToLiveSeconds () {

    return (int)ehCache.getCacheConfiguration().getTimeToLiveSeconds();
  }

  @Override
  public V get (String key) {

    return valueClass.cast(ehCache.get(key).getValue());
  }

  @Override
  public Map<String, V> get (String[] keys)
    throws CacheOperationException {

    HashMap<String, V> resultMap = new HashMap<String, V>();

    for (String key : keys) {

      V value;

      if ((value = valueClass.cast(ehCache.get(key).getValue())) != null) {
        resultMap.put(key, value);
      }
    }

    return resultMap;
  }

  @Override
  public void set (String key, V value, int timeToLiveSeconds) {

    ehCache.put(createElement(key, value, timeToLiveSeconds));
  }

  @Override
  public V putIfAbsent (String key, V value, int timeToLiveSeconds) {

    return valueClass.cast(ehCache.putIfAbsent(createElement(key, value, timeToLiveSeconds)).getValue());
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
  public boolean putViaCas (String key, V oldValue, V value, long version, int timeToLiveSeconds) {

    if (oldValue == null) {

      return ehCache.putIfAbsent(createElement(key, value, timeToLiveSeconds)) == null;
    }
    else {

      return ehCache.replace(createElement(key, oldValue, timeToLiveSeconds), createElement(key, value, timeToLiveSeconds));
    }
  }

  @Override
  public void remove (String key) {

    ehCache.remove(key);
  }
}
