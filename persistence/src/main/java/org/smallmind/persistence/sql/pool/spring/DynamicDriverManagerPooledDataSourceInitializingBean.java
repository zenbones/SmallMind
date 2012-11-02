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
package org.smallmind.persistence.sql.pool.spring;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.persistence.sql.pool.PooledDataSource;
import org.smallmind.persistence.sql.pool.context.ContextualPooledDataSource;
import org.smallmind.persistence.sql.pool.context.DefaultContextualPoolNameTranslator;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class DynamicDriverManagerPooledDataSourceInitializingBean implements InitializingBean, DisposableBean {

  /*
  jdbc.driver.class_name.<pool name> (required)
  jdbc.url.<pool name>.<context>.<#> (required, for at least connection '0')
  jdbc.user.<pool name>.<context>.<#> (required, for at least connection '0')
  jdbc.password.<pool name>.<context>.<#> (required, for at least connection '0')
  jdbc.max_statements.<pool name> (optional - defaults to '0')
  jdbc.validation_query.<pool name> (optional - defaults to 'select 1')
  jdbc.pool.test_on_acquire.<pool name> (optional - defaults to 'false')
  jdbc.pool.initial_size.<pool name> (optional - defaults to '0')
  jdbc.pool.min_size.<pool name> (optional - defaults to '0')
  jdbc.pool.max_size.<pool name> (optional - defaults to '10')
  jdbc.pool.acquire_wait_time_millis.<pool name> (optional - defaults to '0')
  jdbc.pool.connection_timeout_millis.<pool name> (optional - defaults to '0')
  jdbc.pool.max_idle_seconds.<pool name> (optional - defaults to '0')
  jdbc.pool.max_lease_time_seconds.<pool name> (optional - defaults to '0')
  jdbc.mapping.<data source name> (required for each data source binding)
   */

  private final HashMap<String, AbstractPooledDataSource> dataSourceMap = new HashMap<String, AbstractPooledDataSource>();
  private final HashMap<String, String> poolNameMap = new HashMap<String, String>();

  private String[] poolNames;

  public void setPoolNames (String[] poolNames) {

    this.poolNames = poolNames;
  }

  public DataSource getDataSource (String dataSourceKey) {

    DataSource dataSource;
    String poolName;

    if ((poolName = poolNameMap.get(dataSourceKey)) == null) {
      throw new RuntimeBeansException("No mapping definition was provided for data source key(%s)", dataSourceKey);
    }
    if ((dataSource = dataSourceMap.get(poolName)) == null) {
      throw new RuntimeBeansException("No connection pool(%s) definition exists for data source key(%s)", poolName, dataSourceKey);
    }

    return dataSource;
  }

  @Override
  public void afterPropertiesSet ()
    throws SQLException, ComponentPoolException {

    SpringPropertyAccessor springPropertyAccessor = new SpringPropertyAccessor();

    for (String poolName : poolNames) {

      dataSourceMap.put(poolName, parsePoolDefinition(springPropertyAccessor, poolName));
    }

    for (String key : springPropertyAccessor.getKeySet()) {

      String dataSourceKey;
      String poolName;

      if (key.startsWith("jdbc.mapping.") && (!(dataSourceKey = key.substring("jdbc.mapping.".length())).contains("."))) {
        poolNameMap.put(dataSourceKey, poolName = springPropertyAccessor.asString("jdbc.mapping." + dataSourceKey));
        if (!dataSourceMap.containsKey(poolName)) {
          throw new RuntimeBeansException("No connection pool(%s) definition exists for data source key(%s)", poolName, dataSourceKey);
        }
      }
    }

    for (AbstractPooledDataSource dataSource : dataSourceMap.values()) {
      dataSource.startup();
    }
  }

  @Override
  public void destroy ()
    throws ComponentPoolException {

    for (AbstractPooledDataSource dataSource : dataSourceMap.values()) {
      dataSource.shutdown();
    }
  }

  private AbstractPooledDataSource parsePoolDefinition (SpringPropertyAccessor springPropertyAccessor, String poolName)
    throws SQLException, ComponentPoolException {

    ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();
    HashMap<String, HashMap<Integer, DatabaseConnection>> preContextMap = new HashMap<String, HashMap<Integer, DatabaseConnection>>();
    HashMap<String, DatabaseConnection[]> postContextMap = new HashMap<String, DatabaseConnection[]>();
    Option<Boolean> testOnAcquireOption;
    Option<Long> acquireWaitTimeMillisOption;
    Option<Long> connectionTimeoutMillisOption;
    Option<Integer> maxStatementsOption;
    Option<Integer> initialSizeOption;
    Option<Integer> minSizeOption;
    Option<Integer> maxSizeOption;
    Option<Integer> maxIdleSecondsOption;
    Option<Integer> maxLeaseTimeSecondsOption;
    String driverClassName;
    String validationQuery;
    String urlPrefix = "jdbc.url." + poolName + ".";
    String userPrefix = "jdbc.user." + poolName + ".";
    String passwordPrefix = "jdbc.password." + poolName + ".";

    for (String key : springPropertyAccessor.getKeySet()) {
      if (key.startsWith(urlPrefix)) {
        getDatabaseConnection(preContextMap, getContextIndex(key.substring(urlPrefix.length()))).setJdbcUrl(springPropertyAccessor.asString(key));
      }
      if (key.startsWith(userPrefix)) {
        getDatabaseConnection(preContextMap, getContextIndex(key.substring(userPrefix.length()))).setUser(springPropertyAccessor.asString(key));
      }
      if (key.startsWith(passwordPrefix)) {
        getDatabaseConnection(preContextMap, getContextIndex(key.substring(passwordPrefix.length()))).setPassword(springPropertyAccessor.asString(key));
      }
    }

    if (preContextMap.isEmpty()) {
      throw new RuntimeBeansException("Database connection pool(%s) has no defined connections", poolName);
    }
    if ((preContextMap.size() == 1) && (!preContextMap.containsKey(null))) {
      throw new RuntimeBeansException("Database connection pool(%s) has only a single defined context, so should be defined without any context at all", poolName);
    }

    for (Map.Entry<String, HashMap<Integer, DatabaseConnection>> contextEntry : preContextMap.entrySet()) {

      HashMap<Integer, DatabaseConnection> connectionMap = contextEntry.getValue();
      LinkedList<DatabaseConnection> connectionList = new LinkedList<DatabaseConnection>();
      DatabaseConnection[] connections;
      int index = 0;

      while (!connectionMap.isEmpty()) {

        DatabaseConnection connection;

        if ((connection = connectionMap.remove(index++)) == null) {
          if (contextEntry.getKey() == null) {
            throw new RuntimeBeansException("Database connection pool(%s) is missing a connection definition at index(%d)", poolName, index - 1);
          }
          else {
            throw new RuntimeBeansException("Database connection pool(%s) at context(%s) is missing a connection definition at index(%d)", poolName, contextEntry.getKey(), index - 1);
          }
        }
        if (!connection.isComplete()) {
          if (contextEntry.getKey() == null) {
            throw new RuntimeBeansException("Database connection pool(%s) has an incomplete connection definition at index(%d)", poolName, index - 1);
          }
          else {
            throw new RuntimeBeansException("Database connection pool(%s) at context(%s) has an incomplete connection definition at index(%d)", poolName, contextEntry.getKey(), index - 1);
          }
        }

        connectionList.add(connection);
      }

      connections = new DatabaseConnection[connectionList.size()];
      connectionList.toArray(connections);
      postContextMap.put(contextEntry.getKey(), connections);
    }

    if ((driverClassName = springPropertyAccessor.asString("jdbc.driver.class_name." + poolName)) == null) {
      throw new RuntimeBeansException("Database connection pool(%s) must have a defined driver class name", poolName);
    }

    maxStatementsOption = springPropertyAccessor.asInt("jdbc.max_statements." + poolName);
    validationQuery = springPropertyAccessor.asString("jdbc.validation_query." + poolName);

    if (!(testOnAcquireOption = springPropertyAccessor.asBoolean("jdbc.pool.test_on_acquire." + poolName)).isNone()) {
      complexPoolConfig.setTestOnAcquire(testOnAcquireOption.get());
    }
    if (!(initialSizeOption = springPropertyAccessor.asInt("jdbc.pool.initial_size." + poolName)).isNone()) {
      complexPoolConfig.setInitialPoolSize(initialSizeOption.get());
    }
    if (!(minSizeOption = springPropertyAccessor.asInt("jdbc.pool.min_size." + poolName)).isNone()) {
      complexPoolConfig.setMinPoolSize(minSizeOption.get());
    }
    if (!(maxSizeOption = springPropertyAccessor.asInt("jdbc.pool.max_size." + poolName)).isNone()) {
      complexPoolConfig.setMaxPoolSize(maxSizeOption.get());
    }
    if (!(acquireWaitTimeMillisOption = springPropertyAccessor.asLong("jdbc.pool.acquire_wait_time_millis." + poolName)).isNone()) {
      complexPoolConfig.setAcquireWaitTimeMillis(acquireWaitTimeMillisOption.get());
    }
    if (!(connectionTimeoutMillisOption = springPropertyAccessor.asLong("jdbc.pool.connection_timeout_millis." + poolName)).isNone()) {
      complexPoolConfig.setCreationTimeoutMillis(connectionTimeoutMillisOption.get());
    }
    if (!(maxIdleSecondsOption = springPropertyAccessor.asInt("jdbc.pool.max_idle_seconds." + poolName)).isNone()) {
      complexPoolConfig.setMaxIdleTimeSeconds(maxIdleSecondsOption.get());
    }
    if (!(maxLeaseTimeSecondsOption = springPropertyAccessor.asInt("jdbc.pool.max_lease_time_seconds." + poolName)).isNone()) {
      complexPoolConfig.setMaxLeaseTimeSeconds(maxLeaseTimeSecondsOption.get());
    }

    if (postContextMap.size() == 1) {

      return new PooledDataSource(PooledConnectionComponentPoolFactory.constructComponentPool(poolName, driverClassName, validationQuery, maxStatementsOption.isNone() ? 0 : maxStatementsOption.get(), complexPoolConfig, postContextMap.get(null)));
    }
    else {

      ComponentPool<PooledConnection>[] componentPools = new ComponentPool[postContextMap.size()];
      DefaultContextualPoolNameTranslator poolNameTranslator = new DefaultContextualPoolNameTranslator(poolName, ':');
      int index = 0;

      for (Map.Entry<String, DatabaseConnection[]> contextEntry : postContextMap.entrySet()) {
        componentPools[index++] = PooledConnectionComponentPoolFactory.constructComponentPool(poolNameTranslator.getPoolName(contextEntry.getKey()), driverClassName, validationQuery, maxStatementsOption.isNone() ? 0 : maxStatementsOption.get(), complexPoolConfig, contextEntry.getValue());
      }

      return new ContextualPooledDataSource(poolNameTranslator, componentPools);
    }
  }

  private ContextIndex getContextIndex (String subKey) {

    int periodPos;

    if ((periodPos = subKey.indexOf('.')) < 0) {

      return new ContextIndex(null, Integer.parseInt(subKey));
    }

    return new ContextIndex(subKey.substring(0, periodPos), Integer.parseInt(subKey.substring(periodPos + 1)));
  }

  private DatabaseConnection getDatabaseConnection (HashMap<String, HashMap<Integer, DatabaseConnection>> contextMap, ContextIndex contextIndex) {

    HashMap<Integer, DatabaseConnection> connectionMap;
    DatabaseConnection databaseConnection;

    if ((connectionMap = contextMap.get(contextIndex.getContext())) == null) {
      contextMap.put(contextIndex.getContext(), connectionMap = new HashMap<Integer, DatabaseConnection>());
    }

    if ((databaseConnection = connectionMap.get(contextIndex.getIndex())) == null) {
      connectionMap.put(contextIndex.getIndex(), databaseConnection = new DatabaseConnection());
    }

    return databaseConnection;
  }
}
