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
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.pool.DataSourceFactory;

/**
 * {@link DataSourceFactory} that builds {@link DriverManagerDataSource} instances.
 */
public class DriverManagerDataSourceFactory implements DataSourceFactory<DataSource, PooledConnection> {

  private String driverClassName;

  /**
   * Configures the JDBC driver class to load before creating connections.
   *
   * @param driverClassName fully qualified driver class name
   */
  public void setDriverClassName (String driverClassName) {

    this.driverClassName = driverClassName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<PooledConnection> getPooledConnectionClass () {

    return PooledConnection.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<DataSource> getDataSourceClass () {

    return DataSource.class;
  }

  /**
   * Creates a new {@link DriverManagerDataSource} using the configured driver class.
   *
   * @param jdbcUrl  JDBC URL
   * @param user     user name
   * @param password password
   * @return fresh data source instance
   * @throws SQLException if driver loading or validation fails
   */
  @Override
  public DataSource constructDataSource (String jdbcUrl, String user, String password)
    throws SQLException {

    return new DriverManagerDataSource(driverClassName, jdbcUrl, user, password);
  }
}
