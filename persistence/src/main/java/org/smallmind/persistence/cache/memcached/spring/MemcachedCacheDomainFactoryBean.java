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
package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import java.util.Map;
import org.smallmind.memcached.utility.ProxyMemcachedClient;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that constructs {@link MemcachedCacheDomain} instances for DAO configuration.
 */
public class MemcachedCacheDomainFactoryBean implements FactoryBean<MemcachedCacheDomain<?, ?>>, InitializingBean {

  private MemcachedCacheDomain<?, ?> memcachedCacheDomain;
  private ProxyMemcachedClient memcachedClient;
  private Map<Class<?>, Integer> timeToLiveOverrideMap;
  private String discriminator;
  private int timeToLiveSeconds;

  /**
   * Sets the memcached client used by the domain.
   *
   * @param memcachedClient configured memcached client
   */
  public void setMemcachedClient (ProxyMemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  /**
   * Sets the discriminator applied to all keys created by the domain.
   *
   * @param discriminator namespace prefix for cache keys
   */
  public void setDiscriminator (String discriminator) {

    this.discriminator = discriminator;
  }

  /**
   * Sets the default TTL applied to cached entries.
   *
   * @param timeToLiveSeconds TTL in seconds
   */
  public void setTimeToLiveSeconds (int timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  /**
   * Sets optional per-class TTL overrides.
   *
   * @param timeToLiveOverrideMap map of managed class to TTL override
   */
  public void setTimeToLiveOverrideMap (Map<Class<?>, Integer> timeToLiveOverrideMap) {

    this.timeToLiveOverrideMap = timeToLiveOverrideMap;
  }

  /**
   * Instantiates the cache domain once required properties are present.
   *
   * @throws IOException if initialization fails
   */
  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedClient != null) {
      memcachedCacheDomain = new MemcachedCacheDomain(memcachedClient, discriminator, timeToLiveSeconds, timeToLiveOverrideMap);
    }
  }

  /**
   * @return the constructed memcached cache domain
   */
  @Override
  public MemcachedCacheDomain<?, ?> getObject () {

    return memcachedCacheDomain;
  }

  /**
   * @return type exposed by this factory bean
   */
  @Override
  public Class<?> getObjectType () {

    return MemcachedCacheDomain.class;
  }

  /**
   * @return {@code true} to indicate the factory is singleton-scoped
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
