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
package org.smallmind.quorum.pool.connection.remote.spring;

import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.remote.RemoteConnectionPoolSurface;
import org.smallmind.quorum.pool.connection.remote.RemoteConnectionPoolSurfaceImpl;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;
import org.springframework.beans.factory.FactoryBean;

public class RemoteConnectionPoolFactoryBean implements FactoryBean<RemoteConnectionPoolSurface> {

  private ConnectionPool connectionPool;
  private String registryName;

  public void setConnectionPool (ConnectionPool connectionPool) {

    this.connectionPool = connectionPool;
  }

  public void setRegistryName (String registryName) {

    this.registryName = registryName;
  }

  public RemoteConnectionPoolSurface getObject ()
    throws Exception {

    RemoteEndpointBinder.bind(new RemoteConnectionPoolSurfaceImpl(connectionPool), registryName);

    return RemoteProxyFactory.generateRemoteProxy(RemoteConnectionPoolSurface.class, registryName);
  }

  public Class getObjectType () {

    return RemoteConnectionPoolSurface.class;
  }

  public boolean isSingleton () {

    return true;
  }
}
