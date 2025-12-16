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
package org.smallmind.persistence.sql;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * {@link ConnectionPoolDataSource} implementation that delegates to any {@link CommonDataSource}
 * and returns pooled connections created by {@link PooledConnectionFactory}. Supports an optional
 * prepared statement cache size.
 *
 * @param <D> concrete data source type
 * @param <P> pooled connection implementation
 */
public class OmnivorousConnectionPoolDataSource<D extends CommonDataSource, P extends PooledConnection> implements ConnectionPoolDataSource {

  private final D dataSource;
  private final Class<P> pooledConnectionClass;
  private int maxStatements = 0;

  /**
   * Builds a pooling data source that wraps connections from the given data source.
   *
   * @param dataSource            underlying data source
   * @param pooledConnectionClass expected pooled connection type for casting
   */
  public OmnivorousConnectionPoolDataSource (D dataSource, Class<P> pooledConnectionClass) {

    this.dataSource = dataSource;
    this.pooledConnectionClass = pooledConnectionClass;
  }

  /**
   * Builds a pooling data source with a prepared statement cache size.
   *
   * @param dataSource            underlying data source
   * @param pooledConnectionClass expected pooled connection type for casting
   * @param maxStatements         maximum prepared statements to cache per connection
   */
  public OmnivorousConnectionPoolDataSource (D dataSource, Class<P> pooledConnectionClass, int maxStatements) {

    this(dataSource, pooledConnectionClass);

    this.maxStatements = maxStatements;
  }

  /**
   * Obtains a pooled connection from the underlying data source using configured credentials.
   *
   * @return pooled connection instance
   * @throws SQLException if connection acquisition fails
   */
  public P getPooledConnection ()
    throws SQLException {

    return pooledConnectionClass.cast(PooledConnectionFactory.createPooledConnection(dataSource, maxStatements));
  }

  /**
   * {@inheritDoc}
   */
  public P getPooledConnection (String user, String password)
    throws SQLException {

    return pooledConnectionClass.cast(PooledConnectionFactory.createPooledConnection(dataSource, user, password, maxStatements));
  }

  /**
   * Unsupported operation; parent logger is not provided by this adapter.
   *
   * @return nothing; always throws
   * @throws SQLFeatureNotSupportedException always
   */
  public Logger getParentLogger ()
    throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  /**
   * Delegates to the wrapped data source's log writer.
   *
   * @return current log writer
   * @throws SQLException if the underlying data source raises an error
   */
  public PrintWriter getLogWriter ()
    throws SQLException {

    return dataSource.getLogWriter();
  }

  /**
   * {@inheritDoc}
   */
  public void setLogWriter (PrintWriter out)
    throws SQLException {

    dataSource.setLogWriter(out);
  }

  /**
   * {@inheritDoc}
   */
  public int getLoginTimeout ()
    throws SQLException {

    return dataSource.getLoginTimeout();
  }

  /**
   * {@inheritDoc}
   */
  public void setLoginTimeout (int seconds)
    throws SQLException {

    dataSource.setLoginTimeout(seconds);
  }
}
