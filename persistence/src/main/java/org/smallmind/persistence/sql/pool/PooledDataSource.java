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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.DataSourceManager;
import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;

public class PooledDataSource implements DataSource {

  private ConnectionPool connectionPool;
  private PrintWriter logWriter;
  private String key;

  public PooledDataSource (ConnectionPool connectionPool) {

    this(connectionPool.getPoolName(), connectionPool);
  }

  public PooledDataSource (String key, ConnectionPool connectionPool) {

    this.key = key;
    this.connectionPool = connectionPool;

    logWriter = new PrintWriter(new PooledLogWriter());
  }

  public void register () {

    DataSourceManager.register(key, this);
  }

  public String getKey () {

    return key;
  }

  public Connection getConnection ()
    throws SQLException {

    try {
      return ((PooledConnection)connectionPool.getConnection()).getConnection();
    }
    catch (ConnectionPoolException connectionPoolException) {
      throw new SQLException(connectionPoolException);
    }
  }

  public Connection getConnection (String username, String password) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public Logger getParentLogger () throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public PrintWriter getLogWriter () {

    return logWriter;
  }

  public void setLogWriter (PrintWriter out) {

    throw new UnsupportedOperationException("Please properly configure the underlying pool which is represented by this DataSource");
  }

  public void setLoginTimeout (int seconds) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public int getLoginTimeout () {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public boolean isWrapperFor (Class<?> iface) {

    return false;
  }

  public <T> T unwrap (Class<T> iface) {

    throw new UnsupportedOperationException("This DataSource represents a connection pool");
  }
}
