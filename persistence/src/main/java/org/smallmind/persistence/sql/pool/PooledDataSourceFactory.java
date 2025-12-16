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
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import org.smallmind.persistence.sql.pool.spring.DatabaseConnection;
import org.smallmind.persistence.sql.pool.spring.PooledConnectionComponentPoolFactory;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * Factory helper that builds either {@link PooledDataSource} or {@link PooledXADataSource} based on
 * the provided {@link DataSourceFactory} type.
 */
public class PooledDataSourceFactory {

  /**
   * Creates a pooled data source configured with the given pool parameters and database connections.
   *
   * @param poolName          name for the underlying component pool
   * @param dataSourceFactory factory for concrete data sources
   * @param validationQuery   optional validation SQL to run on checkout
   * @param maxStatements     maximum prepared statements to cache per connection
   * @param poolConfig        configuration for the component pool
   * @param connections       one or more database connection definitions
   * @param <D>               data source type
   * @return pooled data source matching XA or non-XA capabilities
   * @throws SQLException if pool construction fails
   */
  public static <D extends CommonDataSource> AbstractPooledDataSource createPooledDataSource (String poolName, DataSourceFactory<D, ? extends PooledConnection> dataSourceFactory, String validationQuery, int maxStatements, ComplexPoolConfig poolConfig, DatabaseConnection[] connections)
    throws SQLException {

    if (XADataSource.class.isAssignableFrom(dataSourceFactory.getDataSourceClass())) {
      return new PooledXADataSource((ComponentPool<XAConnection>)PooledConnectionComponentPoolFactory.constructComponentPool(poolName, dataSourceFactory, validationQuery, maxStatements, poolConfig, connections));
    }

    return new PooledDataSource((ComponentPool<PooledConnection>)PooledConnectionComponentPoolFactory.constructComponentPool(poolName, dataSourceFactory, validationQuery, maxStatements, poolConfig, connections));
  }
}
