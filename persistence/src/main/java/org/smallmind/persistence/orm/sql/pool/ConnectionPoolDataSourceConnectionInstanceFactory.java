/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.orm.sql.pool;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool2.ConnectionInstance;
import org.smallmind.quorum.pool2.ConnectionInstanceFactory;
import org.smallmind.quorum.pool2.ConnectionPool;

public class ConnectionPoolDataSourceConnectionInstanceFactory implements ConnectionInstanceFactory<PooledConnection> {

  private DataSource dataSource;
  private ConnectionPoolDataSource pooledDataSource;
  private String validationQuery = "select 1";

  public ConnectionPoolDataSourceConnectionInstanceFactory (ConnectionPoolDataSource pooledDataSource) {

    this(null, pooledDataSource);
  }

  public ConnectionPoolDataSourceConnectionInstanceFactory (DataSource dataSource, ConnectionPoolDataSource pooledDataSource) {

    this.dataSource = dataSource;
    this.pooledDataSource = pooledDataSource;
  }

  public String getValidationQuery () {

    return validationQuery;
  }

  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  public Object rawInstance ()
    throws SQLException {

    if (dataSource == null) {
      throw new UnsupportedOperationException("No standard (unpooled) data source is available");
    }

    return dataSource.getConnection();
  }

  public ConnectionInstance<PooledConnection> createInstance (ConnectionPool<PooledConnection> connectionPool)
    throws SQLException {

    return new PooledConnectionInstance(connectionPool, pooledDataSource.getPooledConnection(), validationQuery);
  }
}
