/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.sql.pool.spring;

import java.sql.SQLException;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.pool.ConnectionEndpoint;
import org.smallmind.persistence.sql.pool.DriverManagerComponentInstanceFactory;
import org.smallmind.persistence.sql.pool.PooledDataSource;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.quorum.pool.complex.ComponentPoolException;

public class DriverManagerPooledDataSourceProvider {

  private final PooledDataSource dataSource;
  private final ComponentPool<PooledConnection> componentPool;

  public DriverManagerPooledDataSourceProvider (String poolName, String driverClassName, String validationQuery, int maxStatements, ComplexPoolConfig poolConfig, DatabaseConnection... connections)
    throws ComponentPoolException, SQLException {

    DriverManagerComponentInstanceFactory connectionInstanceFactory = new DriverManagerComponentInstanceFactory(driverClassName, maxStatements, createConnectionEndpoints(connections));

    if (validationQuery != null) {
      connectionInstanceFactory.setValidationQuery(validationQuery);
    }

    dataSource = new PooledDataSource(componentPool = new ComponentPool<PooledConnection>(poolName, connectionInstanceFactory).setComplexPoolConfig(poolConfig));
  }

  public PooledDataSource getPooledDataSource () {

    return dataSource;
  }

  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }

  private ConnectionEndpoint[] createConnectionEndpoints (DatabaseConnection... databaseConnections) {

    if (databaseConnections == null) {

      return new ConnectionEndpoint[0];
    }
    else {

      ConnectionEndpoint[] connectionEndpoints = new ConnectionEndpoint[databaseConnections.length];

      for (int count = 0; count < connectionEndpoints.length; count++) {
        connectionEndpoints[count] = new ConnectionEndpoint(databaseConnections[count].getJdbcUrl(), databaseConnections[count].getUser(), databaseConnections[count].getPassword());
      }

      return connectionEndpoints;
    }
  }
}
