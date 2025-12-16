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

import java.lang.reflect.Array;
import java.sql.SQLException;
import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.OmnivorousConnectionPoolDataSource;

/**
 * Convenience factory that builds {@link ConnectionPoolDataSource}s from JDBC endpoints using a
 * {@link DataSourceFactory}, then delegates to {@link PooledConnectionComponentInstanceFactory} to
 * create pooled connection instances.
 */
public class DataSourceComponentInstanceFactory<D extends CommonDataSource, P extends PooledConnection> extends PooledConnectionComponentInstanceFactory<P> {

  /**
   * Creates a factory for a single endpoint with default validation and no statement caching.
   *
   * @param dataSourceFactory factory used to construct concrete data sources
   * @param jdbcUrl           JDBC URL
   * @param user              user name
   * @param password          password
   * @throws SQLException if data source construction fails
   */
  public DataSourceComponentInstanceFactory (DataSourceFactory<D, P> dataSourceFactory, String jdbcUrl, String user, String password)
    throws SQLException {

    this(dataSourceFactory, jdbcUrl, user, password, 0);
  }

  /**
   * Creates a factory for a single endpoint with a prepared statement cache size.
   *
   * @param dataSourceFactory factory used to construct concrete data sources
   * @param jdbcUrl           JDBC URL
   * @param user              user name
   * @param password          password
   * @param maxStatements     maximum statements to cache per connection
   * @throws SQLException if data source construction fails
   */
  public DataSourceComponentInstanceFactory (DataSourceFactory<D, P> dataSourceFactory, String jdbcUrl, String user, String password, int maxStatements)
    throws SQLException {

    this(dataSourceFactory, maxStatements, new ConnectionEndpoint(jdbcUrl, user, password));
  }

  /**
   * Creates a factory over multiple endpoints without statement caching.
   *
   * @param dataSourceFactory factory used to construct concrete data sources
   * @param endpoints         one or more JDBC endpoints
   * @throws SQLException if data source construction fails
   */
  public DataSourceComponentInstanceFactory (DataSourceFactory<D, P> dataSourceFactory, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(dataSourceFactory, 0, endpoints);
  }

  /**
   * Creates a factory over multiple endpoints with prepared statement cache size.
   *
   * @param dataSourceFactory factory used to construct concrete data sources
   * @param maxStatements     maximum statements to cache per connection
   * @param endpoints         one or more JDBC endpoints
   * @throws SQLException if data source construction fails
   */
  public DataSourceComponentInstanceFactory (DataSourceFactory<D, P> dataSourceFactory, int maxStatements, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(maxStatements, dataSourceFactory.getPooledConnectionClass(), constructDataSources(dataSourceFactory, endpoints));
  }

  /**
   * Allows direct construction from already-built data sources.
   *
   * @param maxStatements         maximum statements to cache per connection
   * @param pooledConnectionClass pooled connection class
   * @param dataSources           data sources to wrap
   */
  public DataSourceComponentInstanceFactory (int maxStatements, Class<P> pooledConnectionClass, D... dataSources) {

    super(60, pooledConnectionClass, constructConnectionPoolDataSources(maxStatements, pooledConnectionClass, dataSources));
  }

  /**
   * Builds data sources for each provided endpoint.
   *
   * @param dataSourceFactory factory used to construct concrete data sources
   * @param endpoints         JDBC endpoints
   * @param <D>               data source type
   * @param <P>               pooled connection type
   * @return array of constructed data sources
   * @throws SQLException if construction fails
   */
  private static <D extends CommonDataSource, P extends PooledConnection> D[] constructDataSources (DataSourceFactory<D, P> dataSourceFactory, ConnectionEndpoint... endpoints)
    throws SQLException {

    D[] dataSources = (D[])Array.newInstance(dataSourceFactory.getDataSourceClass(), endpoints.length);

    for (int index = 0; index < endpoints.length; index++) {
      dataSources[index] = dataSourceFactory.constructDataSource(endpoints[index].getJdbcUrl(), endpoints[index].getUser(), endpoints[index].getPassword());
    }

    return dataSources;
  }

  /**
   * Wraps each data source in an {@link OmnivorousConnectionPoolDataSource} with the given cache
   * size.
   *
   * @param maxStatements       maximum statements to cache
   * @param connectionPoolClass pooled connection class
   * @param dataSources         data sources to wrap
   * @param <D>                 data source type
   * @param <P>                 pooled connection type
   * @return array of connection pool data sources
   */
  private static <D extends CommonDataSource, P extends PooledConnection> ConnectionPoolDataSource[] constructConnectionPoolDataSources (int maxStatements, Class<P> connectionPoolClass, D... dataSources) {

    ConnectionPoolDataSource[] connectionPoolDataSources = new ConnectionPoolDataSource[dataSources.length];

    for (int index = 0; index < dataSources.length; index++) {
      connectionPoolDataSources[index] = new OmnivorousConnectionPoolDataSource<D, P>(dataSources[index], connectionPoolClass, maxStatements);
    }

    return connectionPoolDataSources;
  }
}
