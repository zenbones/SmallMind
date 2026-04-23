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
package org.smallmind.nutsnbolts.spring.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Spring {@link FactoryBean} that locates an existing RMI registry or creates a new one,
 * optionally using custom client and server socket factories, and exposes the result as a singleton {@link Registry} bean.
 */
public class RMIRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean {

  private Registry registry;
  private RMIClientSocketFactory clientSocketFactory;
  private RMIServerSocketFactory serverSocketFactory;
  private String host;
  private int port;

  /**
   * Sets the host where the registry should be located or created; when {@code null} the local host is assumed.
   *
   * @param host the host name or IP address, or {@code null} for localhost
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Sets the port number on which the RMI registry listens.
   *
   * @param port the registry port
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Supplies an optional custom client socket factory for registry connections; must be paired with a server factory when set.
   *
   * @param clientSocketFactory the client socket factory, or {@code null} to use the default
   */
  public void setClientSocketFactory (RMIClientSocketFactory clientSocketFactory) {

    this.clientSocketFactory = clientSocketFactory;
  }

  /**
   * Supplies an optional custom server socket factory for registry creation; must be paired with a client factory when set.
   *
   * @param serverSocketFactory the server socket factory, or {@code null} to use the default
   */
  public void setServerSocketFactory (RMIServerSocketFactory serverSocketFactory) {

    this.serverSocketFactory = serverSocketFactory;
  }

  /**
   * Returns the {@link Registry} instance obtained or created during initialization.
   *
   * @return the registry singleton
   * @throws Exception if the object cannot be returned
   */
  @Override
  public Registry getObject ()
    throws Exception {

    return registry;
  }

  /**
   * Returns {@link Registry} as the type of object produced by this factory.
   *
   * @return the {@link Registry} class
   */
  @Override
  public Class<?> getObjectType () {

    return Registry.class;
  }

  /**
   * Confirms that this factory always produces a single shared registry instance.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Attempts to locate an existing registry at the configured host and port; if none is reachable and the host is local,
   * creates a new registry using the configured socket factories (both must be either set or null together).
   *
   * @throws UnknownHostException if the configured host name cannot be resolved
   * @throws RemoteException      if the registry cannot be reached on a remote host or cannot be created
   */
  @Override
  public void afterPropertiesSet ()
    throws UnknownHostException, RemoteException {

    InetAddress hostInetAddress = null;

    if (host != null) {
      hostInetAddress = InetAddress.getByName(host);
    }

    synchronized (LocateRegistry.class) {
      try {
        registry = LocateRegistry.getRegistry(host, port, clientSocketFactory);
        registry.list();
      } catch (RemoteException remoteException) {
        if ((hostInetAddress == null) || InetAddress.getLocalHost().equals(hostInetAddress)) {
          if ((clientSocketFactory != null) && (serverSocketFactory != null)) {
            registry = LocateRegistry.createRegistry(port, clientSocketFactory, serverSocketFactory);
            registry.list();
          } else if ((clientSocketFactory == null) && (serverSocketFactory == null)) {
            registry = LocateRegistry.createRegistry(port);
            registry.list();
          } else {
            throw new IllegalStateException("Either both client and server socket factories must be left null, or both must be set, in order to create a registry");
          }
        } else {
          throw remoteException;
        }
      }
    }
  }
}
