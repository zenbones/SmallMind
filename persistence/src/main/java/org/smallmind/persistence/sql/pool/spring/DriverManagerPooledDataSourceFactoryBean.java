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
import javax.sql.DataSource;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DriverManagerPooledDataSourceFactoryBean implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

  private DriverManagerPooledDataSourceProvider dataSourceProvider;
  private ComplexPoolConfig poolConfig;
  private DatabaseConnection[] connections;
  private String driverClassName;
  private String poolName;
  private String validationQuery;
  private int maxStatements;

  public void setPoolName (String poolName) {

    this.poolName = poolName;
  }

  public void setDriverClassName (String driverClassName) {

    this.driverClassName = driverClassName;
  }

  public void setConnections (DatabaseConnection[] connections) {

    this.connections = connections;
  }

  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  public void setMaxStatements (int maxStatements) {

    this.maxStatements = maxStatements;
  }

  public void setPoolConfig (ComplexPoolConfig poolConfig) {

    this.poolConfig = poolConfig;
  }

  @Override
  public void afterPropertiesSet ()
    throws SQLException, ComponentPoolException {

    dataSourceProvider = new DriverManagerPooledDataSourceProvider(poolName, driverClassName, validationQuery, maxStatements, poolConfig, connections);
    dataSourceProvider.startup();
  }

  @Override
  public void destroy ()
    throws ComponentPoolException {

    dataSourceProvider.shutdown();
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return DataSource.class;
  }

  @Override
  public DataSource getObject () {

    return dataSourceProvider.getPooledDataSource();
  }
}
