/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class DriverManagerDataSource implements DataSource {

  private final String jdbcUrl;
  private final String user;
  private final String password;
  private PrintWriter logWriter;

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

  public Connection getConnection (String user, String password)
    throws SQLException {

    return DriverManager.getConnection(jdbcUrl, user, password);
  }

  public Logger getParentLogger () throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public PrintWriter getLogWriter () {

    return logWriter;
  }

  public void setLogWriter (PrintWriter logWriter) {

    this.logWriter = logWriter;
  }

  public int getLoginTimeout () {

    return 0;
  }

  public void setLoginTimeout (int timeoutSeconds) {

    throw new UnsupportedOperationException();
  }

  public boolean isWrapperFor (Class<?> iface) {

    return false;
  }

  public <T> T unwrap (Class<T> iface) {

    return null;
  }
}
