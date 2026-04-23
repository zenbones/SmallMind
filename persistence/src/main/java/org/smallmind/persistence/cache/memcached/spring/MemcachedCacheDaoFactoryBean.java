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
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.smallmind.persistence.cache.praxis.extrinsic.ByKeyExtrinsicCacheDao;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that constructs a {@link ByKeyExtrinsicCacheDao} backed by a
 * memcached {@link MemcachedCacheDomain}.
 *
 * <p>The DAO is created during {@link #afterPropertiesSet()} if a non-null
 * {@link MemcachedCacheDomain} has been injected. If no domain is supplied the factory produces
 * a {@code null} object, allowing optional memcached caching to be configured in Spring without
 * causing context startup failures when the dependency is absent.</p>
 *
 * <p>This bean is always singleton-scoped.</p>
 */
public class MemcachedCacheDaoFactoryBean implements FactoryBean<ByKeyExtrinsicCacheDao<?, ?>>, InitializingBean {

  private ByKeyExtrinsicCacheDao<?, ?> memcachedCacheDao;
  private MemcachedCacheDomain<?, ?> memcachedCacheDomain;

  /**
   * Injects the {@link MemcachedCacheDomain} used to back the DAO.
   *
   * @param memcachedCacheDomain the configured memcached cache domain; may be {@code null}
   */
  public void setMemcachedCacheDomain (MemcachedCacheDomain<?, ?> memcachedCacheDomain) {

    this.memcachedCacheDomain = memcachedCacheDomain;
  }

  /**
   * Constructs the {@link ByKeyExtrinsicCacheDao} from the injected domain, if present.
   *
   * @throws IOException if the DAO cannot be initialised
   */
  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedCacheDomain != null) {
      memcachedCacheDao = new ByKeyExtrinsicCacheDao<>(memcachedCacheDomain);
    }
  }

  /**
   * Returns the constructed {@link ByKeyExtrinsicCacheDao}, or {@code null} if no domain was
   * supplied.
   *
   * @return the cache DAO, or {@code null}
   */
  @Override
  public ByKeyExtrinsicCacheDao<?, ?> getObject () {

    return memcachedCacheDao;
  }

  /**
   * Returns the concrete type produced by this factory.
   *
   * @return {@link ByKeyExtrinsicCacheDao}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return ByKeyExtrinsicCacheDao.class;
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
