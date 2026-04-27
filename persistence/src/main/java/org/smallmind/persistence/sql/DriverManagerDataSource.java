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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Minimal {@link DataSource} backed by {@link DriverManager}, useful for simple configurations
 * without connection pooling.
 */
public class DriverManagerDataSource implements DataSource {

  private final String jdbcUrl;
  private final String user;
  private final String password;
  private PrintWriter logWriter;

  /**
   * Creates a data source using the supplied JDBC driver, url, and credentials.
   *
   * @param driverClassName fully-qualified driver class to load
   * @param jdbcUrl         JDBC connection URL
   * @param user            username
   * @param password        password
   * @throws SQLException if the driver class cannot be loaded
   */
  public DriverManagerDataSource (String driverClassName, String jdbcUrl, String user, String password)
    throws SQLException {

    this.jdbcUrl = jdbcUrl;
    this.user = user;
    this.password = password;

    try {
      Class.forName(driverClassName);
    } catch (ClassNotFoundException classNotFoundException) {
      throw new SQLException(classNotFoundException);
    }
  }

  public Connection getConnection ()
    throws SQLException {

    return DriverManager.getConnection(jdbcUrl, user, password);
  }

  /**
   * Attempts to establish a connection with the data source that this DataSource object
   * represents.
   *
   * @param user     the database user on whose behalf the connection is being made
   * @param password the user's password
   * @return a connection to the data source
   * @throws SQLException if a database access error occurs
   */
  public Connection getConnection (String user, String password)
    throws SQLException {

    return DriverManager.getConnection(jdbcUrl, user, password);
  }

  public Logger getParentLogger ()
    throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  /**
   * Retrieves the log writer for this DataSource object.
   *
   * @return the log writer for this data source or null if logging is disabled
   */
  @Override
  public PrintWriter getLogWriter () {

    return logWriter;
  }

  /**
   * Sets the log writer for this DataSource object to the given java.io.PrintWriter object.
   *
   * @param logWriter the new log writer; to disable logging, set to null
   */
  public void setLogWriter (PrintWriter logWriter) {

    this.logWriter = logWriter;
  }

  /**
   * Gets the maximum time in seconds that this data source can wait while attempting to connect
   * to a database.
   *
   * @return the data source login time limit
   */
  @Override
  public int getLoginTimeout () {

    return 0;
  }

  /**
   * Login timeout configuration is not supported.
   *
   * @param timeoutSeconds ignored
   * @throws UnsupportedOperationException always
   */
  public void setLoginTimeout (int timeoutSeconds) {

    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if this either implements the interface argument or is directly or indirectly
   * a wrapper for an object that does. Returns false otherwise.
   *
   * @param iface a Class defining an interface
   * @return true if this implements the interface or directly or indirectly wraps an object that does
   */
  public boolean isWrapperFor (Class<?> iface) {

    return false;
  }

  /**
   * Returns an object that implements the given interface to allow access to non-standard methods,
   * or standard methods not exposed by the proxy. If the receiver implements the interface then the
   * result is the receiver or a proxy for the receiver. If the receiver is a wrapper and the
   * wrapped object implements the interface then the result is the wrapped object or a proxy for
   * the wrapped object.
   *
   * @param iface a Class defining an interface that the result must implement
   * @return an object that implements the interface
   */
  public <T> T unwrap (Class<T> iface) {

    return null;
  }
}
