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
package org.smallmind.persistence.sql.pool.spring;

import java.sql.SQLException;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.persistence.sql.pool.DataSourceFactory;
import org.smallmind.persistence.sql.pool.PooledDataSourceFactory;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that builds, starts, and eventually shuts down a pooled data source.
 */
public class PooledDataSourceFactoryBean<D extends CommonDataSource, P extends PooledConnection> implements FactoryBean<CommonDataSource>, InitializingBean, DisposableBean {

  private AbstractPooledDataSource dataSource;
  private DataSourceFactory<D, P> dataSourceFactory;
  private ComplexPoolConfig poolConfig;
  private DatabaseConnection[] connections;
  private String poolName;
  private String validationQuery;
  private int maxStatements;

  /**
   * @return configured pool name
   */
  public String getPoolName () {

    return poolName;
  }

  /**
   * Sets the pool name used for the underlying component pool.
   *
   * @param poolName name assigned to the pool
   */
  public void setPoolName (String poolName) {

    this.poolName = poolName;
  }

  /**
   * @return current data source factory
   */
  public DataSourceFactory<D, P> getDataSourceFactory () {

    return dataSourceFactory;
  }

  /**
   * Sets the data source factory used to create concrete connections.
   *
   * @param dataSourceFactory factory that builds data sources
   */
  public void setDataSourceFactory (DataSourceFactory<D, P> dataSourceFactory) {

    this.dataSourceFactory = dataSourceFactory;
  }

  /**
   * @return configured connection definitions
   */
  public DatabaseConnection[] getConnections () {

    return connections;
  }

  /**
   * Supplies the JDBC connection definitions to populate the pool.
   *
   * @param connections connection definitions
   */
  public void setConnections (DatabaseConnection[] connections) {

    this.connections = connections;
  }

  /**
   * @return validation query configured for pooled connections
   */
  public String getValidationQuery () {

    return validationQuery;
  }

  /**
   * Sets an optional validation query executed when borrowing a connection.
   *
   * @param validationQuery SQL used for validation; null/empty disables validation
   */
  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  /**
   * @return prepared statement cache size per connection
   */
  public int getMaxStatements () {

    return maxStatements;
  }

  /**
   * Sets the maximum number of prepared statements cached per connection.
   *
   * @param maxStatements cache size for prepared statements
   */
  public void setMaxStatements (int maxStatements) {

    this.maxStatements = maxStatements;
  }

  /**
   * @return configuration of the component pool
   */
  public ComplexPoolConfig getPoolConfig () {

    return poolConfig;
  }

  /**
   * Applies configuration for the component pool.
   *
   * @param poolConfig pool configuration
   */
  public void setPoolConfig (ComplexPoolConfig poolConfig) {

    this.poolConfig = poolConfig;
  }

  /**
   * Builds and starts the pooled data source using the configured parameters.
   *
   * @throws SQLException           if creation fails
   * @throws ComponentPoolException if pool startup fails
   */
  @Override
  public void afterPropertiesSet ()
    throws SQLException, ComponentPoolException {

    dataSource = PooledDataSourceFactory.createPooledDataSource(poolName, dataSourceFactory, validationQuery, maxStatements, poolConfig, connections);
    dataSource.startup();
  }

  /**
   * Shuts down the pooled data source, releasing all pooled resources.
   *
   * @throws ComponentPoolException if shutdown fails
   */
  @Override
  public void destroy ()
    throws ComponentPoolException {

    dataSource.shutdown();
  }

  /**
   * Factory produces a singleton data source.
   *
   * @return always {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Declares the produced object type.
   *
   * @return {@link CommonDataSource}.class
   */
  @Override
  public Class<?> getObjectType () {

    return CommonDataSource.class;
  }

  /**
   * Returns the initialized pooled data source.
   *
   * @return pooled data source instance
   */
  @Override
  public CommonDataSource getObject () {

    return dataSource;
  }
}
