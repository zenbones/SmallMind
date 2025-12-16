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

import java.sql.SQLException;
import javax.sql.ConnectionEvent;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

/**
 * {@link XAConnection} wrapper that layers pooled connection behavior and statement caching on top
 * of an {@link XADataSource}-provided connection.
 */
public class XADataSourcePooledConnection extends AbstractPooledConnection<XADataSource> implements XAConnection {

  private final XAConnection xaConnection;

  /**
   * Acquires an XA connection from the data source and wraps it with pooling logic.
   *
   * @param dataSource    XA data source
   * @param maxStatements maximum prepared statements to cache
   * @throws SQLException if obtaining the connection fails
   */
  public XADataSourcePooledConnection (XADataSource dataSource, int maxStatements)
    throws SQLException {

    this(dataSource, dataSource.getXAConnection(), maxStatements);
  }

  /**
   * Same as {@link #XADataSourcePooledConnection(XADataSource, int)} but uses explicit credentials.
   *
   * @param dataSource    XA data source
   * @param user          user name
   * @param password      password
   * @param maxStatements maximum prepared statements to cache
   * @throws SQLException if obtaining the connection fails
   */
  public XADataSourcePooledConnection (XADataSource dataSource, String user, String password, int maxStatements)
    throws SQLException {

    this(dataSource, dataSource.getXAConnection(user, password), maxStatements);
  }

  /**
   * Internal constructor used by public overloads after obtaining an {@link XAConnection}.
   *
   * @param dataSource    XA data source
   * @param xaConnection  underlying XA connection to wrap
   * @param maxStatements maximum prepared statements to cache
   * @throws SQLException if initializing the pooled connection fails
   */
  private XADataSourcePooledConnection (XADataSource dataSource, XAConnection xaConnection, int maxStatements)
    throws SQLException {

    super(dataSource, xaConnection.getConnection(), maxStatements);

    this.xaConnection = xaConnection;
  }

  /**
   * Wraps the XA connection in a connection event when reporting errors or closure to listeners.
   *
   * @param sqlException optional exception that triggered the event
   * @return constructed connection event
   */
  @Override
  public ConnectionEvent getConnectionEvent (SQLException sqlException) {

    return (sqlException == null) ? new ConnectionEvent(this) : new ConnectionEvent(this, sqlException);
  }

  /**
   * Delegates to the underlying XA connection to obtain the {@link XAResource}.
   *
   * @return XA resource for transaction enlistment
   * @throws SQLException if the resource cannot be retrieved
   */
  @Override
  public XAResource getXAResource ()
    throws SQLException {

    return xaConnection.getXAResource();
  }
}
