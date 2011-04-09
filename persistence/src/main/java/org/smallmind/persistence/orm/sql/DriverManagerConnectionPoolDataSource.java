/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class DriverManagerConnectionPoolDataSource implements ConnectionPoolDataSource {

  private DriverManagerDataSource dataSource;
  private boolean existential;
  private int maxStatements = 0;

  public DriverManagerConnectionPoolDataSource (String driverClassName, String jdbcUrl, String user, String password)
    throws SQLException {

    this(driverClassName, jdbcUrl, user, password, false);
  }

  public DriverManagerConnectionPoolDataSource (String driverClassName, String jdbcUrl, String user, String password, boolean existential)
    throws SQLException {

    this(new DriverManagerDataSource(driverClassName, jdbcUrl, user, password), existential);
  }

  public DriverManagerConnectionPoolDataSource (DriverManagerDataSource dataSource) {

    this(dataSource, false);
  }

  public DriverManagerConnectionPoolDataSource (DriverManagerDataSource dataSource, boolean existential) {

    this.dataSource = dataSource;
    this.existential = existential;
  }

  public PooledConnection getPooledConnection ()
    throws SQLException {

    return new DriverManagerPooledConnection(dataSource, maxStatements, existential);
  }

  public PooledConnection getPooledConnection (String user, String password)
    throws SQLException {

    return new DriverManagerPooledConnection(dataSource, user, password, maxStatements, existential);
  }

  public int getMaxStatements () {

    return maxStatements;
  }

  public void setMaxStatements (int maxStatements) {

    this.maxStatements = maxStatements;
  }

  public PrintWriter getLogWriter ()
    throws SQLException {

    return dataSource.getLogWriter();
  }

  public void setLogWriter (PrintWriter out)
    throws SQLException {

    dataSource.setLogWriter(out);
  }

  public void setLoginTimeout (int seconds)
    throws SQLException {

    dataSource.setLoginTimeout(seconds);
  }

  public int getLoginTimeout ()
    throws SQLException {

    return dataSource.getLoginTimeout();
  }
}
