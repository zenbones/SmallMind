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
package org.smallmind.persistence.sql.pool.context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.nutsnbolts.context.ExpectedContexts;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

@ExpectedContexts(PooledDataSourceContext.class)
public class ContextualPooledDataSource extends AbstractPooledDataSource {

  private final HashMap<String, ComponentPool<PooledConnection>> componentPoolMap = new HashMap<String, ComponentPool<PooledConnection>>();
  private final String baseName;

  public ContextualPooledDataSource (ContextualPoolNameTranslator poolNameTranslator, ComponentPool<PooledConnection>... componentPools)
    throws ComponentPoolException {

    baseName = poolNameTranslator.getBaseName();
    for (ComponentPool<PooledConnection> componentPool : componentPools) {
      componentPoolMap.put(poolNameTranslator.getContextualPartFromPoolName(componentPool.getPoolName()), componentPool);
    }
  }

  @Override
  public Connection getConnection ()
    throws SQLException {

    try {

      PooledDataSourceContext pooledDataSourceContext = ContextFactory.getContext(PooledDataSourceContext.class);
      ComponentPool<PooledConnection> componentPool;
      String contextualPart;

      if ((componentPool = componentPoolMap.get(contextualPart = (pooledDataSourceContext == null) ? null : pooledDataSourceContext.getContextualPart())) == null) {
        throw new ComponentPoolException("Unable to locate component pool for base name(%s) and context(%s)", baseName, contextualPart);
      }

      return componentPool.getComponent().getConnection();
    }
    catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  @Override
  public void startup ()
    throws ComponentPoolException {

    for (ComponentPool<PooledConnection> componentPool : componentPoolMap.values()) {
      componentPool.startup();
    }
  }

  @Override
  public void shutdown ()
    throws ComponentPoolException {

    for (ComponentPool<PooledConnection> componentPool : componentPoolMap.values()) {
      componentPool.shutdown();
    }
  }
}
