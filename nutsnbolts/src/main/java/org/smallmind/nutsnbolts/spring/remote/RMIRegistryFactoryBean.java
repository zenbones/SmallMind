/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

public class RMIRegistryFactoryBean implements FactoryBean<Registry>, InitializingBean {

  private SelfAwareRegistry registry;
  private RMIClientSocketFactory clientSocketFactory;
  private RMIServerSocketFactory serverSocketFactory;
  private String host;
  private int port;

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setClientSocketFactory (RMIClientSocketFactory clientSocketFactory) {

    this.clientSocketFactory = clientSocketFactory;
  }

  public void setServerSocketFactory (RMIServerSocketFactory serverSocketFactory) {

    this.serverSocketFactory = serverSocketFactory;
  }

  @Override
  public SelfAwareRegistry getObject () throws Exception {

    return registry;
  }

  @Override
  public Class<?> getObjectType () {

    return SelfAwareRegistry.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public void afterPropertiesSet ()
    throws UnknownHostException, RemoteException {

    InetAddress hostInetAddress = null;

    if (host != null) {
      hostInetAddress = InetAddress.getByName(host);
    }

    synchronized (LocateRegistry.class) {
      try {
        registry = new SelfAwareRegistry(host, port, LocateRegistry.getRegistry(host, port, clientSocketFactory));
        registry.list();
      }
      catch (RemoteException remoteException) {
        if ((hostInetAddress == null) || InetAddress.getLocalHost().equals(hostInetAddress)) {
          if ((clientSocketFactory != null) && (serverSocketFactory != null)) {
            registry = new SelfAwareRegistry(InetAddress.getLocalHost().getHostAddress(), port, LocateRegistry.createRegistry(port, clientSocketFactory, serverSocketFactory));
            registry.list();
          }
          else if ((clientSocketFactory == null) && (serverSocketFactory == null)) {
            registry = new SelfAwareRegistry(InetAddress.getLocalHost().getHostAddress(), port, LocateRegistry.createRegistry(port));
            registry.list();
          }
          else {
            throw new IllegalStateException("Either both client and server socket factories must be left null, or both must be set, in order to create a registry");
          }
        }
        else {
          throw remoteException;
        }
      }
    }
  }
}
