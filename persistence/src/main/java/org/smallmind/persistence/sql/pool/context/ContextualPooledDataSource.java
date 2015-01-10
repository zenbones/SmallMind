/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.sql.pool.context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class ContextualPooledDataSource extends AbstractPooledDataSource<DataSource, PooledConnection> implements DataSource {

  private final HashMap<String, ComponentPool<? extends PooledConnection>> componentPoolMap = new HashMap<>();
  private final String baseName;

  public ContextualPooledDataSource (ContextualPoolNameTranslator poolNameTranslator, ComponentPool<? extends PooledConnection>... componentPools)
    throws ComponentPoolException {

    super(DataSource.class, PooledConnection.class);

    baseName = poolNameTranslator.getBaseName();
    for (ComponentPool<? extends PooledConnection> componentPool : componentPools) {
      componentPoolMap.put(poolNameTranslator.getContextualPartFromPoolName(componentPool.getPoolName()), componentPool);
    }
  }

  @Override
  public Connection getConnection ()
    throws SQLException {

    try {

      PooledDataSourceContext pooledDataSourceContext = ContextFactory.getContext(PooledDataSourceContext.class);
      ComponentPool<? extends PooledConnection> componentPool;
      String contextualPart;

      if ((componentPool = componentPoolMap.get(contextualPart = (pooledDataSourceContext == null) ? null : pooledDataSourceContext.getContextualPart())) == null) {
        throw new ComponentPoolException("Unable to locate component pool for base name(%s) and context(%s)", baseName, contextualPart == null ? "null" : contextualPart);
      }

      return componentPool.getComponent().getConnection();
    }
    catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  public Connection getConnection (String username, String password) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  @Override
  public void startup ()
    throws ComponentPoolException {

    for (ComponentPool<? extends PooledConnection> componentPool : componentPoolMap.values()) {
      componentPool.startup();
    }
  }

  @Override
  public void shutdown ()
    throws ComponentPoolException {

    for (ComponentPool<? extends PooledConnection> componentPool : componentPoolMap.values()) {
      componentPool.shutdown();
    }
  }
}
