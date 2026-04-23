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

/**
 * Spring {@link FactoryBean} that constructs, starts, and manages the lifecycle of a
 * {@link CubbyMemcachedClient}.
 *
 * <p>When the Spring context is refreshed ({@link #afterPropertiesSet()}) the factory converts
 * the injected server map into an array of {@link MemcachedHost} descriptors, creates a
 * {@link CubbyMemcachedClient}, and calls {@link CubbyMemcachedClient#start()} to open
 * connections. When the context is closed ({@link #destroy()}) the factory calls
 * {@link CubbyMemcachedClient#stop()} to cleanly shut down all connections.</p>
 *
 * <p>Client creation can be suppressed entirely by setting {@code enabled} to {@code false},
 * in which case {@link #getObject()} returns {@code null}. This is useful for environments
 * where no memcached cluster is available (e.g. local development).</p>
 *
 * <p>This bean is always singleton-scoped.</p>
 */
public class CubbyMemcachedClientFactoryBean implements FactoryBean<CubbyMemcachedClient>, InitializingBean, DisposableBean {

  private CubbyMemcachedClient memcachedClient;
  private CubbyConfiguration configuration;
  private Map<String, MemcachedServer> servers;
  private boolean enabled = true;

  /**
   * Controls whether the client is actually created during context initialisation.
   *
   * <p>When set to {@code false} no connections are opened and {@link #getObject()} returns
   * {@code null}.</p>
   *
   * @param enabled {@code true} to create the client (default); {@code false} to suppress it
   */
  public void setEnabled (boolean enabled) {

    this.enabled = enabled;
  }

  /**
   * Sets the {@link CubbyConfiguration} used when constructing the client.
   *
   * @param configuration the client configuration
   */
  public void setConfiguration (CubbyConfiguration configuration) {

    this.configuration = configuration;
  }

  /**
   * Sets the map of named memcached servers to connect to.
   *
   * <p>The map key is a logical server name (used for logging and consistent-hash routing)
   * and the value describes the host and port.</p>
   *
   * @param servers map of logical server name to {@link MemcachedServer} descriptor
   */
  public void setServers (Map<String, MemcachedServer> servers) {

    this.servers = servers;
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

  /**
   * Returns the concrete type produced by this factory.
   *
   * @return {@link CubbyMemcachedClient}{@code .class}
   */
  @Override
  public Class<?> getObjectType () {

    return CubbyMemcachedClient.class;
  }

  /**
   * Returns the running {@link CubbyMemcachedClient}, or {@code null} if the client was not
   * created (disabled or no servers configured).
   *
   * @return the memcached client, or {@code null}
   */
  @Override
  public CubbyMemcachedClient getObject () {

    return memcachedClient;
  }

  /**
   * Builds and starts the {@link CubbyMemcachedClient} if the factory is enabled and at least
   * one server has been configured.
   *
   * @throws IOException             if a network error occurs while establishing connections
   * @throws InterruptedException    if the calling thread is interrupted during startup
   * @throws CubbyOperationException if the client encounters a protocol-level error during startup
   */
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

  /**
   * Stops the {@link CubbyMemcachedClient} and releases all associated resources when the
   * Spring context is closed.
   *
   * @throws IOException          if a network error occurs while closing connections
   * @throws InterruptedException if the calling thread is interrupted during shutdown
   */
  @Override
  public void destroy ()
    throws IOException, InterruptedException {

    if (memcachedClient != null) {
      memcachedClient.stop();
      LoggerManager.getLogger(CubbyMemcachedClientFactoryBean.class).info("Memcached client stopped...");
    }
  }
}
