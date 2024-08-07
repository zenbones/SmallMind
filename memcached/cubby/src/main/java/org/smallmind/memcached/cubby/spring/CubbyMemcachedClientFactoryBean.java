/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.util.Arrays;
import java.util.Map;
import org.smallmind.memcached.cubby.CubbyConfiguration;
import org.smallmind.memcached.cubby.CubbyMemcachedClient;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.MemcachedHost;
import org.smallmind.memcached.utility.MemcachedServer;
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

    return CubbyMemcachedClient.class;
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
      String[] output = new String[servers.size()];
      int index = 0;

      for (Map.Entry<String, MemcachedServer> serverEntry : servers.entrySet()) {
        output[index] = serverEntry.getKey() + '=' + serverEntry.getValue().getHost() + ':' + serverEntry.getValue().getPort();
        memcachedHosts[index++] = new MemcachedHost(serverEntry.getKey(), serverEntry.getValue().getHost(), serverEntry.getValue().getPort());
      }

      memcachedClient = new CubbyMemcachedClient(configuration, memcachedHosts);
      LoggerManager.getLogger(CubbyMemcachedClientFactoryBean.class).info("Memcached servers(%s) initialized...", Arrays.toString(output));
      memcachedClient.start();
      LoggerManager.getLogger(CubbyMemcachedClientFactoryBean.class).info("Memcached client started...");
    }
  }

  @Override
  public void destroy ()
    throws IOException, InterruptedException {

    if (memcachedClient != null) {
      memcachedClient.stop();
      LoggerManager.getLogger(CubbyMemcachedClientFactoryBean.class).info("Memcached client stopped...");
    }
  }
}
