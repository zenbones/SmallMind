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
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import org.smallmind.persistence.sql.DataSourceManager;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * XA-capable data source backed by a {@link ComponentPool} of {@link XAConnection}s. Supports
 * registration with {@link DataSourceManager} for keyed lookup.
 */
public class PooledXADataSource extends AbstractPooledDataSource<XADataSource, XAConnection> implements XADataSource {

  private final ComponentPool<XAConnection> componentPool;
  private final String key;

  /**
   * Builds an XA pooled data source using the pool name as its registry key.
   *
   * @param componentPool pool of XA connections
   */
  public PooledXADataSource (ComponentPool<XAConnection> componentPool) {

    this(componentPool.getPoolName(), componentPool);
  }

  /**
   * Builds an XA pooled data source with an explicit registry key.
   *
   * @param key           name used when registering with {@link DataSourceManager}
   * @param componentPool pool of XA connections
   */
  public PooledXADataSource (String key, ComponentPool<XAConnection> componentPool) {

    super(XADataSource.class, XAConnection.class);

    this.key = key;
    this.componentPool = componentPool;
  }

  /**
   * Registers this data source for lookup via {@link DataSourceManager}.
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
   * Borrows an XA connection from the pool.
   *
   * @return pooled XA connection
   * @throws SQLException if the pool cannot provide a connection
   */
  @Override
  public XAConnection getXAConnection ()
    throws SQLException {

    try {
      return componentPool.getComponent();
    } catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  /**
   * Unsupported because credentials are configured at the pool level.
   *
   * @param user     ignored user
   * @param password ignored password
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  @Override
  public XAConnection getXAConnection (String user, String password)
    throws SQLException {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
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
