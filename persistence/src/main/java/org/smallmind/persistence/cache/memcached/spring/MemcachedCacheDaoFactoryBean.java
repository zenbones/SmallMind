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
 * Spring factory bean that produces a {@link ByKeyExtrinsicCacheDao} backed by memcached caches.
 */
public class MemcachedCacheDaoFactoryBean implements FactoryBean<ByKeyExtrinsicCacheDao<?, ?>>, InitializingBean {

  private ByKeyExtrinsicCacheDao<?, ?> memcachedCacheDao;
  private MemcachedCacheDomain<?, ?> memcachedCacheDomain;

  /**
   * Injects the memcached cache domain required to build the DAO.
   *
   * @param memcachedCacheDomain configured cache domain
   */
  public void setMemcachedCacheDomain (MemcachedCacheDomain<?, ?> memcachedCacheDomain) {

    this.memcachedCacheDomain = memcachedCacheDomain;
  }

  /**
   * Initializes the factory by constructing the DAO after dependencies are set.
   *
   * @throws IOException if the DAO cannot be created
   */
  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedCacheDomain != null) {
      memcachedCacheDao = new ByKeyExtrinsicCacheDao<>(memcachedCacheDomain);
    }
  }

  /**
   * @return the created DAO instance
   */
  @Override
  public ByKeyExtrinsicCacheDao<?, ?> getObject () {

    return memcachedCacheDao;
  }

  /**
   * @return type exposed by the factory
   */
  @Override
  public Class<?> getObjectType () {

    return ByKeyExtrinsicCacheDao.class;
  }

  /**
   * @return {@code true} because the factory always returns the same DAO
   */
  @Override
  public boolean isSingleton () {

    return true;
  }
}
