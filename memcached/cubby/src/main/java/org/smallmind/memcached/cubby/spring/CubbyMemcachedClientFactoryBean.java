/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.memcached.cubby.spring;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyMemcachedClient;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.utility.MemcachedServer;
import org.smallmind.memcached.utility.XMemcachedMemcachedClient;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class CubbyMemcachedClientFactoryBean implements FactoryBean<CubbyMemcachedClient>, InitializingBean, DisposableBean {

  private CubbyMemcachedClient memcachedClient;
  private CubbyConfiguration configuration;
  private Map<String, MemcachedServer> servers;
  private boolean enabled = true;

  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  public void setConfiguration (CubbyConfiguration configuration) {

    this.configuration = configuration;
  }

  public void setServers (Map<String, MemcachedServer> servers) {

    this.servers = servers;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return XMemcachedMemcachedClient.class;
  }

  @Override
  public CubbyMemcachedClient getObject () {

    return memcachedClient;
  }

  @Override
  public void afterPropertiesSet ()
    throws IOException, InterruptedException, CubbyOperationException {

    if (enabled && (servers != null) && (servers.size() > 0)) {

      MemcachedHost[] memcachedHosts = new MemcachedHost[servers.size()];
      int index = 0;

      for (Map.Entry<String, MemcachedServer> serverEntry : servers.entrySet()) {
        memcachedHosts[index++] = new MemcachedHost(serverEntry.getKey(), new InetSocketAddress(serverEntry.getValue().getHost(), serverEntry.getValue().getPort()));
      }

      memcachedClient = new CubbyMemcachedClient(configuration, memcachedHosts);
      LoggerManager.getLogger(org.smallmind.memcached.utility.spring.XMemcachedMemcachedClientFactoryBean.class).info("Memcached servers(%s) initialized...", outputAddresses(memcachedHosts));
      memcachedClient.start();
      LoggerManager.getLogger(org.smallmind.memcached.utility.spring.XMemcachedMemcachedClientFactoryBean.class).info("Memcached client started...");
    }
  }

  private String outputAddresses (MemcachedHost[] memcachedHosts) {

    String[] output = new String[memcachedHosts.length];
    int index = 0;

    for (MemcachedHost memcachedHost : memcachedHosts) {
      output[index++] = memcachedHost.toString();
    }

    return Arrays.toString(output);
  }

  @Override
  public void destroy ()
    throws IOException, InterruptedException {

    if (memcachedClient != null) {
      memcachedClient.stop();
      LoggerManager.getLogger(org.smallmind.memcached.utility.spring.XMemcachedMemcachedClientFactoryBean.class).info("Memcached client stopped...");
    }
  }
}