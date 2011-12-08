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
package org.smallmind.persistence.cache.memcached.spring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import org.smallmind.persistence.cache.memcached.MemcachedServer;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MemcachedClientFactoryBean implements FactoryBean<MemcachedClient>, InitializingBean {

  private MemcachedClient memcachedClient;
  private MemcachedServer[] servers;
  private boolean enabled = true;
  private int poolSize;

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  public void setServers (MemcachedServer[] servers) {

    this.servers = servers;
  }

  public void setPoolSize (int poolSize) {

    this.poolSize = poolSize;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException {

    if (enabled && (servers != null) && (servers.length > 0)) {

      MemcachedClientBuilder builder;
      LinkedList<InetSocketAddress> addressList;

      addressList = new LinkedList<InetSocketAddress>();
      for (MemcachedServer server : servers) {
        addressList.add(new InetSocketAddress(server.getHost(), server.getPort()));
      }

      builder = new XMemcachedClientBuilder(addressList);
      builder.setFailureMode(true);
      builder.setConnectionPoolSize(poolSize);
      builder.setCommandFactory(new BinaryCommandFactory());
      builder.setSessionLocator(new KetamaMemcachedSessionLocator());
      memcachedClient = builder.build();
    }
  }

  @Override
  public MemcachedClient getObject () {

    return memcachedClient;
  }

  @Override
  public Class<?> getObjectType () {

    return MemcachedClient.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  public void shutdown () {

    if (memcachedClient != null)
      try {
        memcachedClient.shutdown();
      }
      catch (IOException e) {
        LoggerManager.getLogger(MemcachedClientFactoryBean.class).error(e);
      }
  }
}