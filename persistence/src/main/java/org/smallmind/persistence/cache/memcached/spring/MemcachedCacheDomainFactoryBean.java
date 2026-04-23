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
 * Spring {@link FactoryBean} that assembles a {@link MemcachedCacheDomain} from injected
 * configuration properties.
 *
 * <p>A {@link MemcachedCacheDomain} is created during {@link #afterPropertiesSet()} only when
 * a non-null {@link ProxyMemcachedClient} has been supplied. This allows optional memcached
 * caching to be declared in a Spring context without causing a startup failure when no client
 * is available.</p>
 *
 * <p>This bean is always singleton-scoped.</p>
 */
public class MemcachedCacheDomainFactoryBean implements FactoryBean<MemcachedCacheDomain<?, ?>>, InitializingBean {

  private MemcachedCacheDomain<?, ?> memcachedCacheDomain;
  private ProxyMemcachedClient memcachedClient;
  private Map<Class<?>, Integer> timeToLiveOverrideMap;
  private String discriminator;
  private int timeToLiveSeconds;

  /**
   * Sets the memcached client that the domain will use to operate on the cache cluster.
   *
   * @param memcachedClient the configured client; may be {@code null} to suppress domain creation
   */
  public void setMemcachedClient (ProxyMemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  /**
   * Sets the discriminator namespace applied to every key managed by the domain.
   *
   * @param discriminator the namespace prefix string
   */
  public void setDiscriminator (String discriminator) {

    this.discriminator = discriminator;
  }

  /**
   * Sets the default time-to-live applied to all cached entries in this domain.
   *
   * @param timeToLiveSeconds the default TTL in seconds
   */
  public void setTimeToLiveSeconds (int timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  /**
   * Sets an optional map of per-class TTL overrides that take precedence over the domain default.
   *
   * @param timeToLiveOverrideMap map from entity class to override TTL in seconds; may be
   *                              {@code null}
   */
  public void setTimeToLiveOverrideMap (Map<Class<?>, Integer> timeToLiveOverrideMap) {

    this.timeToLiveOverrideMap = timeToLiveOverrideMap;
  }

  /**
   * Constructs the {@link MemcachedCacheDomain} from the injected properties, provided that a
   * client has been supplied.
   *
   * @throws IOException if domain initialisation fails
   */
  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedClient != null) {
      memcachedCacheDomain = new MemcachedCacheDomain(memcachedClient, discriminator, timeToLiveSeconds, timeToLiveOverrideMap);
    }
  }

  /**
   * Returns the constructed {@link MemcachedCacheDomain}, or {@code null} if no client was
   * supplied.
   *
   * @return the cache domain, or {@code null}
   */
  @Override
  public MemcachedCacheDomain<?, ?> getObject () {

    return memcachedCacheDomain;
  }

  /**
   * Returns the concrete type produced by this factory.
   *
   * @return {@link MemcachedCacheDomain}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return MemcachedCacheDomain.class;
  }

  /**
   * Reports that this factory bean always returns the same singleton instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
