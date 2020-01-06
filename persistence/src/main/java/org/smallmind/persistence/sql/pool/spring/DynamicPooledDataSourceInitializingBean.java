/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.persistence.sql.pool.DataSourceFactory;
import org.smallmind.persistence.sql.pool.PooledDataSourceFactory;
import org.smallmind.persistence.sql.pool.context.ContextualPooledDataSource;
import org.smallmind.persistence.sql.pool.context.DefaultContextualPoolNameTranslator;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class DynamicPooledDataSourceInitializingBean implements InitializingBean, DisposableBean, DataSourceLocator {

  /*
  <prefix>.jdbc.url.<pool name>.<context>.<#> (required, for at least connection '0')
  <prefix>.jdbc.user.<pool name>.<context>.<#> (required, for at least connection '0')
  <prefix>.jdbc.password.<pool name>.<context>.<#> (required, for at least connection '0')
  <prefix>.jdbc.max_statements.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.validation_query.<pool name> (optional - defaults to 'select 1')
  <prefix>.jdbc.pool.test_on_create.<pool name> (optional - defaults to 'false')
  <prefix>.jdbc.pool.test_on_acquire.<pool name> (optional - defaults to 'false')
  <prefix>.jdbc.pool.initial_size.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.pool.min_size.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.pool.max_size.<pool name> (optional - defaults to '10')
  <prefix>.jdbc.pool.acquire_wait_time_millis.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.pool.connection_timeout_millis.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.pool.max_idle_seconds.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.pool.max_lease_time_seconds.<pool name> (optional - defaults to '0')
  <prefix>.jdbc.mapping.<data source name> (required for each data source binding)
  */

  private final HashMap<String, AbstractPooledDataSource> dataSourceMap = new HashMap<String, AbstractPooledDataSource>();
  private final HashMap<String, String> poolNameMap = new HashMap<String, String>();

  private Map<String, DataSourceFactory<?, ?>> factoryMap;
  private String prefix;

  public void setFactoryMap (Map<String, DataSourceFactory<?, ?>> factoryMap) {

    this.factoryMap = factoryMap;
  }

  public void setPrefix (String prefix) {

    this.prefix = (prefix == null) ? null : prefix.trim();
  }

  public CommonDataSource getDataSource (String dataSourceKey) {

    CommonDataSource dataSource;
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

    SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();

    prefix = ((prefix == null) || prefix.isEmpty()) ? "" : prefix + ".";

    for (String poolName : factoryMap.keySet()) {

      dataSourceMap.put(poolName, parsePoolDefinition(springPropertyAccessor, poolName));
    }

    for (String key : springPropertyAccessor.getKeySet()) {

      String dataSourceKey;
      String poolName;
      String mappingKey = prefix + "jdbc.mapping.";

      if (key.startsWith(mappingKey) && (!(dataSourceKey = key.substring(mappingKey.length())).contains("."))) {
        poolNameMap.put(dataSourceKey, poolName = springPropertyAccessor.asString(mappingKey + dataSourceKey));
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
    HashMap<String, HashMap<Integer, DatabaseConnection>> preContextMap = new HashMap<>();
    HashMap<String, DatabaseConnection[]> postContextMap = new HashMap<>();
    Option<Boolean> testOnCreateOption;
    Option<Boolean> testOnAcquireOption;
    Option<Long> acquireWaitTimeMillisOption;
    Option<Long> connectionTimeoutMillisOption;
    Option<Integer> maxStatementsOption;
    Option<Integer> initialSizeOption;
    Option<Integer> minSizeOption;
    Option<Integer> maxSizeOption;
    Option<Integer> maxIdleSecondsOption;
    Option<Integer> maxLeaseTimeSecondsOption;
    String validationQuery;
    String urlPrefix = prefix + "jdbc.url." + poolName + ".";
    String userPrefix = prefix + "jdbc.user." + poolName + ".";
    String passwordPrefix = prefix + "jdbc.password." + poolName + ".";

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
      LinkedList<DatabaseConnection> connectionList = new LinkedList<>();
      DatabaseConnection[] connections;
      int index = 0;

      while (!connectionMap.isEmpty()) {

        DatabaseConnection connection;

        if ((connection = connectionMap.remove(index++)) == null) {
          if (contextEntry.getKey() == null) {
            throw new RuntimeBeansException("Database connection pool(%s) is missing a connection definition at index(%d)", poolName, index - 1);
          } else {
            throw new RuntimeBeansException("Database connection pool(%s) at context(%s) is missing a connection definition at index(%d)", poolName, contextEntry.getKey(), index - 1);
          }
        }
        if (!connection.isComplete()) {
          if (contextEntry.getKey() == null) {
            throw new RuntimeBeansException("Database connection pool(%s) has an incomplete connection definition at index(%d)", poolName, index - 1);
          } else {
            throw new RuntimeBeansException("Database connection pool(%s) at context(%s) has an incomplete connection definition at index(%d)", poolName, contextEntry.getKey(), index - 1);
          }
        }

        connectionList.add(connection);
      }

      connections = new DatabaseConnection[connectionList.size()];
      connectionList.toArray(connections);
      postContextMap.put(contextEntry.getKey(), connections);
    }

    maxStatementsOption = springPropertyAccessor.asInt(prefix + "jdbc.max_statements." + poolName);
    validationQuery = springPropertyAccessor.asString(prefix + "jdbc.validation_query." + poolName);

    if (!(testOnCreateOption = springPropertyAccessor.asBoolean(prefix + "jdbc.pool.test_on_create." + poolName)).isNone()) {
      complexPoolConfig.setTestOnCreate(testOnCreateOption.get());
    }
    if (!(testOnAcquireOption = springPropertyAccessor.asBoolean(prefix + "jdbc.pool.test_on_acquire." + poolName)).isNone()) {
      complexPoolConfig.setTestOnAcquire(testOnAcquireOption.get());
    }
    if (!(initialSizeOption = springPropertyAccessor.asInt(prefix + "jdbc.pool.initial_size." + poolName)).isNone()) {
      complexPoolConfig.setInitialPoolSize(initialSizeOption.get());
    }
    if (!(minSizeOption = springPropertyAccessor.asInt(prefix + "jdbc.pool.min_size." + poolName)).isNone()) {
      complexPoolConfig.setMinPoolSize(minSizeOption.get());
    }
    if (!(maxSizeOption = springPropertyAccessor.asInt(prefix + "jdbc.pool.max_size." + poolName)).isNone()) {
      complexPoolConfig.setMaxPoolSize(maxSizeOption.get());
    }
    if (!(acquireWaitTimeMillisOption = springPropertyAccessor.asLong(prefix + "jdbc.pool.acquire_wait_time_millis." + poolName)).isNone()) {
      complexPoolConfig.setAcquireWaitTimeMillis(acquireWaitTimeMillisOption.get());
    }
    if (!(connectionTimeoutMillisOption = springPropertyAccessor.asLong(prefix + "jdbc.pool.connection_timeout_millis." + poolName)).isNone()) {
      complexPoolConfig.setCreationTimeoutMillis(connectionTimeoutMillisOption.get());
    }
    if (!(maxIdleSecondsOption = springPropertyAccessor.asInt(prefix + "jdbc.pool.max_idle_seconds." + poolName)).isNone()) {
      complexPoolConfig.setMaxIdleTimeSeconds(maxIdleSecondsOption.get());
    }
    if (!(maxLeaseTimeSecondsOption = springPropertyAccessor.asInt(prefix + "jdbc.pool.max_lease_time_seconds." + poolName)).isNone()) {
      complexPoolConfig.setMaxLeaseTimeSeconds(maxLeaseTimeSecondsOption.get());
    }

    if (postContextMap.size() == 1) {

      return PooledDataSourceFactory.createPooledDataSource(poolName, factoryMap.get(poolName), validationQuery, maxStatementsOption.isNone() ? 0 : maxStatementsOption.get(), complexPoolConfig, postContextMap.get(null));
    } else {

      ComponentPool[] componentPools = new ComponentPool[postContextMap.size()];
      DefaultContextualPoolNameTranslator poolNameTranslator = new DefaultContextualPoolNameTranslator(poolName, ':');
      int index = 0;

      for (Map.Entry<String, DatabaseConnection[]> contextEntry : postContextMap.entrySet()) {
        componentPools[index++] = PooledConnectionComponentPoolFactory.constructComponentPool(poolNameTranslator.getPoolName(contextEntry.getKey()), factoryMap.get(poolName), validationQuery, maxStatementsOption.isNone() ? 0 : maxStatementsOption.get(), complexPoolConfig, contextEntry.getValue());
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
      contextMap.put(contextIndex.getContext(), connectionMap = new HashMap<>());
    }

    if ((databaseConnection = connectionMap.get(contextIndex.getIndex())) == null) {
      connectionMap.put(contextIndex.getIndex(), databaseConnection = new DatabaseConnection());
    }

    return databaseConnection;
  }
}
