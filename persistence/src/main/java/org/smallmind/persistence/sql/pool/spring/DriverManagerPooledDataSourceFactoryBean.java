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
import org.smallmind.persistence.sql.pool.ConnectionEndpoint;
import org.smallmind.persistence.sql.pool.DriverManagerConnectionInstanceFactory;
import org.smallmind.quorum.juggler.JugglerResourceException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class DriverManagerPooledDataSourceFactoryBean implements InitializingBean, FactoryBean<DataSource> {

  /*
  <constructor-arg index="0" value="${jdbc.driver.classname}"/>
  <constructor-arg index="1" value="${jdbc.url.protocol}"/>
  <constructor-arg index="2" value="${jdbc.username.protocol}"/>
  <constructor-arg index="3" value="${jdbc.password.protocol}"/>
  <property name="validationQuery" value="${jdbc.validation.statement}"/>
</bean>

<bean id="protocolDBPool" class="org.smallmind.quorum.pool.connection.ConnectionPool" init-method="startup" destroy-method="shutdown">
  <constructor-arg index="0" value="protocolDBPool"/>
  <constructor-arg index="1" ref="protocolConnectionInstanceFactory"/>
  <property name="connectionPoolConfig">
  */

  private DatabaseConnection[] connections;
  private String driverClassName;
  private int maxStatements;

  @Override
  public void afterPropertiesSet ()
    throws SQLException, JugglerResourceException {

    DriverManagerConnectionInstanceFactory connectionInstanceFactory = new DriverManagerConnectionInstanceFactory(driverClassName, maxStatements, createConnectionEndpoints(connections));

  }

  private ConnectionEndpoint[] createConnectionEndpoints (DatabaseConnection... databaseConnections) {

    if (databaseConnections == null) {

      return new ConnectionEndpoint[0];
    }
    else {

      ConnectionEndpoint[] connectionEndpoints = new ConnectionEndpoint[databaseConnections.length];

      for (int count = 0; count < connectionEndpoints.length; count++) {
        connectionEndpoints[count] = new ConnectionEndpoint(databaseConnections[count].getJdbcUrl(), databaseConnections[count].getUser(), databaseConnections[count].getPassword());
      }

      return connectionEndpoints;
    }
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

    return null;
  }
}
