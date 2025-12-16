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
package org.smallmind.persistence.sql.pool;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.DriverManagerDataSource;
import org.smallmind.persistence.sql.OmnivorousConnectionPoolDataSource;

/**
 * Factory for pooled connections sourced from {@link DriverManagerDataSource} instances. Builds the
 * data sources from JDBC endpoints and wraps them in {@link OmnivorousConnectionPoolDataSource}s.
 */
public class DriverManagerComponentInstanceFactory extends PooledConnectionComponentInstanceFactory {

  /**
   * Creates a factory for a single endpoint with no statement cache.
   *
   * @param driverClassName JDBC driver class to load
   * @param jdbcUrl         JDBC URL
   * @param user            user name
   * @param password        password
   * @throws SQLException if a data source cannot be constructed
   */
  public DriverManagerComponentInstanceFactory (String driverClassName, String jdbcUrl, String user, String password)
    throws SQLException {

    this(driverClassName, jdbcUrl, user, password, 0);
  }

  /**
   * Creates a factory for a single endpoint with statement cache size.
   *
   * @param driverClassName JDBC driver class to load
   * @param jdbcUrl         JDBC URL
   * @param user            user name
   * @param password        password
   * @param maxStatements   maximum statements to cache
   * @throws SQLException if a data source cannot be constructed
   */
  public DriverManagerComponentInstanceFactory (String driverClassName, String jdbcUrl, String user, String password, int maxStatements)
    throws SQLException {

    this(driverClassName, maxStatements, new ConnectionEndpoint(jdbcUrl, user, password));
  }

  /**
   * Creates a factory spanning multiple endpoints with no statement cache.
   *
   * @param driverClassName JDBC driver class to load
   * @param endpoints       endpoints to include
   * @throws SQLException if a data source cannot be constructed
   */
  public DriverManagerComponentInstanceFactory (String driverClassName, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(driverClassName, 0, endpoints);
  }

  /**
   * Creates a factory spanning multiple endpoints with statement cache size.
   *
   * @param driverClassName JDBC driver class to load
   * @param maxStatements   maximum statements to cache
   * @param endpoints       endpoints to include
   * @throws SQLException if a data source cannot be constructed
   */
  public DriverManagerComponentInstanceFactory (String driverClassName, int maxStatements, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(maxStatements, constructDataSources(driverClassName, endpoints));
  }

  /**
   * Allows direct construction from existing data sources.
   *
   * @param maxStatements statement cache size
   * @param dataSources   concrete driver manager data sources
   */
  public DriverManagerComponentInstanceFactory (int maxStatements, DriverManagerDataSource... dataSources) {

    super(60, PooledConnection.class, constructConnectionPoolDataSources(maxStatements, dataSources));
  }

  /**
   * Builds driver-manager data sources for each endpoint.
   *
   * @param driverClassName driver class to load
   * @param endpoints       JDBC endpoints
   * @return array of constructed data sources
   * @throws SQLException if construction fails
   */
  private static DriverManagerDataSource[] constructDataSources (String driverClassName, ConnectionEndpoint... endpoints)
    throws SQLException {

    DriverManagerDataSource[] dataSources = new DriverManagerDataSource[endpoints.length];

    for (int index = 0; index < endpoints.length; index++) {
      dataSources[index] = new DriverManagerDataSource(driverClassName, endpoints[index].getJdbcUrl(), endpoints[index].getUser(), endpoints[index].getPassword());
    }

    return dataSources;
  }

  /**
   * Wraps data sources in {@link OmnivorousConnectionPoolDataSource}s with the given cache size.
   *
   * @param maxStatements cache size
   * @param dataSources   data sources to wrap
   * @return array of pool data sources
   */
  private static ConnectionPoolDataSource[] constructConnectionPoolDataSources (int maxStatements, DriverManagerDataSource... dataSources) {

    ConnectionPoolDataSource[] connectionPoolDataSources = new ConnectionPoolDataSource[dataSources.length];

    for (int index = 0; index < dataSources.length; index++) {
      connectionPoolDataSources[index] = new OmnivorousConnectionPoolDataSource<>(dataSources[index], PooledConnection.class, maxStatements);
    }

    return connectionPoolDataSources;
  }
}
