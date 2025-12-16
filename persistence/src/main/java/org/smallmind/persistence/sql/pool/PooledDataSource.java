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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.persistence.sql.DataSourceManager;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * {@link DataSource} implementation backed by a {@link ComponentPool} of {@link PooledConnection}s.
 * Provides integration with {@link DataSourceManager} for keyed lookup.
 */
public class PooledDataSource extends AbstractPooledDataSource<DataSource, PooledConnection> implements DataSource {

  private final ComponentPool<PooledConnection> componentPool;
  private final String key;

  /**
   * Constructs a pooled data source using the pool name as the registration key.
   *
   * @param componentPool pool that manages pooled connections
   */
  public PooledDataSource (ComponentPool<PooledConnection> componentPool) {

    this(componentPool.getPoolName(), componentPool);
  }

  /**
   * Constructs a pooled data source with an explicit registry key.
   *
   * @param key           name used when registering with {@link DataSourceManager}
   * @param componentPool pool that manages pooled connections
   */
  public PooledDataSource (String key, ComponentPool<PooledConnection> componentPool) {

    super(DataSource.class, PooledConnection.class);

    this.key = key;
    this.componentPool = componentPool;
  }

  /**
   * Registers this data source in the {@link DataSourceManager} under its key.
   */
  public void register () {

    DataSourceManager.register(key, this);
  }

  /**
   * @return registry key for this data source
   */
  public String getKey () {

    return key;
  }

  /**
   * Borrows a connection from the pool and returns its logical {@link Connection}.
   *
   * @return a pooled connection wrapper
   * @throws SQLException if the pool cannot provide a connection
   */
  public Connection getConnection ()
    throws SQLException {

    try {
      return componentPool.getComponent().getConnection();
    } catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  /**
   * Unsupported variant; the pool is configured with credentials externally.
   *
   * @param username unused
   * @param password unused
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  public Connection getConnection (String username, String password) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  /**
   * Exposes stack traces for components currently checked out of the pool (diagnostics).
   *
   * @return array of stack traces for active components
   */
  public StackTrace[] getExistentialStackTraces () {

    return componentPool.getExistentialStackTraces();
  }

  /**
   * Forcibly terminates all in-flight processing in the connection pool.
   */
  public void killAllProcessing () {

    componentPool.killAllProcessing();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }
}
