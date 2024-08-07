/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.persistence.sql.pool.context;

import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.persistence.sql.pool.AbstractPooledDataSource;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;

public class ContextualPooledXADataSource extends AbstractPooledDataSource<XADataSource, XAConnection> implements XADataSource {

  private final HashMap<String, ComponentPool<XAConnection>> componentPoolMap = new HashMap<>();
  private final String baseName;

  public ContextualPooledXADataSource (ContextualPoolNameTranslator poolNameTranslator, ComponentPool<XAConnection>... componentPools)
    throws ComponentPoolException {

    super(XADataSource.class, XAConnection.class);

    baseName = poolNameTranslator.getBaseName();
    for (ComponentPool<XAConnection> componentPool : componentPools) {
      componentPoolMap.put(poolNameTranslator.getContextualPartFromPoolName(componentPool.getPoolName()), componentPool);
    }
  }

  @Override
  public XAConnection getXAConnection ()
    throws SQLException {

    try {

      PooledDataSourceContext pooledDataSourceContext = ContextFactory.getContext(PooledDataSourceContext.class);
      ComponentPool<XAConnection> componentPool;
      String contextualPart;

      if ((componentPool = componentPoolMap.get(contextualPart = (pooledDataSourceContext == null) ? null : pooledDataSourceContext.getContextualPart())) == null) {
        throw new ComponentPoolException("Unable to locate component pool for base name(%s) and context(%s)", baseName, contextualPart == null ? "null" : contextualPart);
      }

      return componentPool.getComponent();
    } catch (ComponentPoolException componentPoolException) {
      throw new SQLException(componentPoolException);
    }
  }

  @Override
  public XAConnection getXAConnection (String user, String password)
    throws SQLException {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  @Override
  public void startup ()
    throws ComponentPoolException {

    for (ComponentPool<XAConnection> componentPool : componentPoolMap.values()) {
      componentPool.startup();
    }
  }

  @Override
  public void shutdown ()
    throws ComponentPoolException {

    for (ComponentPool<XAConnection> componentPool : componentPoolMap.values()) {
      componentPool.shutdown();
    }
  }
}
