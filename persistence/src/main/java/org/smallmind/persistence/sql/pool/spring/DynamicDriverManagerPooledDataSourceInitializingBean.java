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
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPoolException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class DynamicDriverManagerPooledDataSourceInitializingBean implements InitializingBean, DisposableBean, DriverManagerPooledDataSourceProviderFactory {

  /*
  jdbc.driver.class_name.<pool name> (required)
  jdbc.url.<pool name>.<#> (required, for at least connection '0')
  jdbc.user.<pool name>.<#> (required, for at least connection '0')
  jdbc.password.<pool name>.<#> (required, for at least connection '0')
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

  private final HashMap<String, DriverManagerPooledDataSourceProvider> dataSourceProviderMap = new HashMap<String, DriverManagerPooledDataSourceProvider>();
  private final HashMap<String, String> poolNameMap = new HashMap<String, String>();

  private String[] poolNames;

  public void setPoolNames (String[] poolNames) {

    this.poolNames = poolNames;
  }

  @Override
  public DriverManagerPooledDataSourceProvider getDataSourceProvider (String dataSourceKey) {

    DriverManagerPooledDataSourceProvider dataSourceProvider;
    String poolName;

    if ((poolName = poolNameMap.get(dataSourceKey)) == null) {
      throw new RuntimeBeansException("No mapping definition was provided for data source key(%s)", dataSourceKey);
    }
    if ((dataSourceProvider = dataSourceProviderMap.get(poolName)) == null) {
      throw new RuntimeBeansException("No connection pool(%s) definition exists for data source key(%s)", poolName, dataSourceKey);
    }

    return dataSourceProvider;
  }

  @Override
  public void afterPropertiesSet ()
    throws SQLException, ComponentPoolException {

    SpringPropertyAccessor springPropertyAccessor = new SpringPropertyAccessor();

    for (String poolName : poolNames) {

      ComplexPoolConfig complexPoolConfig = new ComplexPoolConfig();
      LinkedList<DatabaseConnection> connectionList = new LinkedList<DatabaseConnection>();
      DatabaseConnection[] connections;
      DatabaseConnection connection;
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
      int index = 0;

      if ((driverClassName = springPropertyAccessor.asString("jdbc.driver.class_name." + poolName)) == null) {
        throw new RuntimeBeansException("Database connection pool(%s) must have a defined driver class name", poolName);
      }

      maxStatementsOption = springPropertyAccessor.asInt("jdbc.max_statements." + poolName);
      validationQuery = springPropertyAccessor.asString("jdbc.validation_query." + poolName);

      while ((connection = containsConnection(springPropertyAccessor, poolName, index++)) != null) {
        connectionList.add(connection);
      }
      if (connectionList.isEmpty()) {
        throw new RuntimeBeansException("Database connection pool(%s) must have at least one complete connection defined at index(0)", poolName);
      }

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

      connections = new DatabaseConnection[connectionList.size()];
      connectionList.toArray(connections);

      dataSourceProviderMap.put(poolName, new DriverManagerPooledDataSourceProvider(poolName, driverClassName, validationQuery, maxStatementsOption.isNone() ? 0 : maxStatementsOption.get(), complexPoolConfig, connections));
    }

    for (String key : springPropertyAccessor.getKeySet()) {

      String dataSourceKey;
      String poolName;

      if (key.startsWith("jdbc.mapping.") && (!(dataSourceKey = key.substring("jdbc.mapping.".length())).contains("."))) {
        poolNameMap.put(dataSourceKey, poolName = springPropertyAccessor.asString("jdbc.mapping." + dataSourceKey));
        if (!dataSourceProviderMap.containsKey(poolName)) {
          throw new RuntimeBeansException("No connection pool(%s) definition exists for data source key(%s)", poolName, dataSourceKey);
        }
      }
    }

    for (DriverManagerPooledDataSourceProvider dataSourceProvider : dataSourceProviderMap.values()) {
      dataSourceProvider.startup();
    }
  }

  @Override
  public void destroy ()
    throws ComponentPoolException {

    for (DriverManagerPooledDataSourceProvider dataSourceProvider : dataSourceProviderMap.values()) {
      dataSourceProvider.shutdown();
    }
  }

  private DatabaseConnection containsConnection (SpringPropertyAccessor springPropertyAccessor, String poolName, int index) {

    String url;
    String user;
    String password;

    if ((url = springPropertyAccessor.asString("jdbc.url." + poolName + "." + String.valueOf(index))) != null) {
      if ((user = springPropertyAccessor.asString("jdbc.user." + poolName + "." + String.valueOf(index))) != null) {
        if ((password = springPropertyAccessor.asString("jdbc.password." + poolName + "." + String.valueOf(index))) != null) {

          return new DatabaseConnection(url, user, password);
        }
      }
    }

    return null;
  }
}
