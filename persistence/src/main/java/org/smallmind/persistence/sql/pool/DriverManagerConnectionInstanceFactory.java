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
package org.smallmind.persistence.sql.pool;

import java.sql.SQLException;
import org.smallmind.persistence.sql.DriverManagerConnectionPoolDataSource;
import org.smallmind.persistence.sql.DriverManagerDataSource;

public class DriverManagerConnectionInstanceFactory extends DataSourceConnectionInstanceFactory {

  public DriverManagerConnectionInstanceFactory (String driverClassName, String jdbcUrl, String user, String password)
    throws SQLException {

    this(driverClassName, jdbcUrl, user, password, 0);
  }

  public DriverManagerConnectionInstanceFactory (String driverClassName, String jdbcUrl, String user, String password, int maxStatements)
    throws SQLException {

    this(driverClassName, maxStatements, new ConnectionEndpoint(jdbcUrl, user, password));
  }

  public DriverManagerConnectionInstanceFactory (String driverClassName, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(driverClassName, 0, endpoints);
  }

  public DriverManagerConnectionInstanceFactory (String driverClassName, int maxStatements, ConnectionEndpoint... endpoints)
    throws SQLException {

    this(maxStatements, constructDataSources(driverClassName, endpoints));
  }

  public DriverManagerConnectionInstanceFactory (int maxStatements, DriverManagerDataSource... dataSources)
    throws SQLException {

    super(60, constructCartridges(maxStatements, dataSources));
  }

  private static DriverManagerDataSource[] constructDataSources (String driverClassName, ConnectionEndpoint... endpoints)
    throws SQLException {

    DriverManagerDataSource[] dataSources = new DriverManagerDataSource[endpoints.length];

    for (int index = 0; index < endpoints.length; index++) {
      dataSources[index] = new DriverManagerDataSource(driverClassName, endpoints[index].getJdbcUrl(), endpoints[index].getUser(), endpoints[index].getPassword());
    }

    return dataSources;
  }

  private static DataSourceCartridge[] constructCartridges (int maxStatements, DriverManagerDataSource... dataSources)
    throws SQLException {

    DataSourceCartridge[] cartridges = new DataSourceCartridge[dataSources.length];

    for (int index = 0; index < dataSources.length; index++) {
      cartridges[index] = new DataSourceCartridge(dataSources[index], new DriverManagerConnectionPoolDataSource(maxStatements, dataSources[index]));
    }

    return cartridges;
  }
}