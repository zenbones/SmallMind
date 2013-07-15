/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import net.rubyeye.xmemcached.MemcachedClient;
import org.smallmind.persistence.cache.memcached.MemcachedCacheDomain;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedCacheDomainFactoryBean implements FactoryBean<MemcachedCacheDomain>, InitializingBean {

  private MemcachedCacheDomain memcachedCacheDomain;
  private MemcachedClient memcachedClient;
  private Map<Class, Integer> timeToLiveOverrideMap;
  private String discriminator;
  private int timeToLiveSeconds;

  public void setMemcachedClient (MemcachedClient memcachedClient) {

    this.memcachedClient = memcachedClient;
  }

  public void setDiscriminator (String discriminator) {

    this.discriminator = discriminator;
  }

  public void setTimeToLiveSeconds (int timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public void setTimeToLiveOverrideMap (Map<Class, Integer> timeToLiveOverrideMap) {

    this.timeToLiveOverrideMap = timeToLiveOverrideMap;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (memcachedClient != null) {
      memcachedCacheDomain = new MemcachedCacheDomain(memcachedClient, discriminator, timeToLiveSeconds, timeToLiveOverrideMap);
    }
  }

  @Override
  public MemcachedCacheDomain getObject () {

    return memcachedCacheDomain;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedCacheDomain.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }
}
