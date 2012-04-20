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
package org.smallmind.persistence.sql.pool;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.juggler.Juggler;
import org.smallmind.quorum.juggler.NoAvailableResourceException;
import org.smallmind.quorum.juggler.ResourceException;
import org.smallmind.quorum.pool.connection.ConnectionInstance;
import org.smallmind.quorum.pool.connection.ConnectionInstanceFactory;
import org.smallmind.quorum.pool.connection.ConnectionPool;

public class DataSourceConnectionInstanceFactory implements ConnectionInstanceFactory<Connection, PooledConnection> {

  private Juggler<DataSourceCartridge, PooledConnection> pooledConnectionJuggler;
  private Juggler<DataSourceCartridge, Connection> rawConnectionJuggler;
  private String validationQuery = "select 1";

  public DataSourceConnectionInstanceFactory (ConnectionPoolDataSource connectionPoolDataSource)
    throws ResourceException {

    this(0, new DataSourceCartridge(connectionPoolDataSource));
  }

  public DataSourceConnectionInstanceFactory (ConnectionPoolDataSource connectionPoolDataSource, int recoveryCheckSeconds)
    throws ResourceException {

    this(recoveryCheckSeconds, new DataSourceCartridge(connectionPoolDataSource));
  }

  public DataSourceConnectionInstanceFactory (DataSource dataSource, ConnectionPoolDataSource connectionPoolDataSource)
    throws ResourceException {

    this(0, new DataSourceCartridge(dataSource, connectionPoolDataSource));
  }

  public DataSourceConnectionInstanceFactory (DataSource dataSource, ConnectionPoolDataSource connectionPoolDataSource, int recoveryCheckSeconds)
    throws ResourceException {

    this(recoveryCheckSeconds, new DataSourceCartridge(dataSource, connectionPoolDataSource));
  }

  public DataSourceConnectionInstanceFactory (DataSourceCartridge... cartridges)
    throws ResourceException {

    this(0, cartridges);
  }

  public DataSourceConnectionInstanceFactory (int recoveryCheckSeconds, DataSourceCartridge... cartridges)
    throws ResourceException {

    rawConnectionJuggler = new Juggler<DataSourceCartridge, Connection>(DataSourceCartridge.class, recoveryCheckSeconds, new ConnectionJugglingPinFactory(), cartridges);
    pooledConnectionJuggler = new Juggler<DataSourceCartridge, PooledConnection>(DataSourceCartridge.class, recoveryCheckSeconds, new PooledConnectionJugglingPinFactory(), cartridges);

    rawConnectionJuggler.initialize();
    pooledConnectionJuggler.initialize();

    rawConnectionJuggler.startup();
    pooledConnectionJuggler.startup();
  }

  public String getValidationQuery () {

    return validationQuery;
  }

  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  public Connection rawInstance ()
    throws NoAvailableResourceException, SQLException {

    Connection connection;

    if ((connection = rawConnectionJuggler.pickResource()) == null) {
      throw new UnsupportedOperationException("No standard (non-pooled) data source is available");
    }

    return connection;
  }

  public ConnectionInstance<PooledConnection> createInstance (ConnectionPool<PooledConnection> connectionPool)
    throws NoAvailableResourceException, SQLException {

    return new PooledConnectionInstance(connectionPool, pooledConnectionJuggler.pickResource(), validationQuery);
  }
}
