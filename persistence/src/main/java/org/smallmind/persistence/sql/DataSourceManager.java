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
package org.smallmind.persistence.sql;

import javax.sql.CommonDataSource;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.orm.ORMInitializationException;

/**
 * Registry of {@link CommonDataSource} instances that bridges {@link DataSourceScopeManager} and
 * {@link DataSourceContextManager}.
 * <p>
 * {@code register} writes to every per-application registry bound on the calling thread: the scope flavor when a
 * {@link PerApplicationScope} is bound, and the context flavor when a {@link PerApplicationContext} is attached, so a
 * registration made from a thread that has both is visible to threads that later carry only one of them. Lookups read
 * the scope flavor first and fall back to the context flavor when the scope holds nothing and a context is attached,
 * so a thread bound to a scope that was never written still finds what was registered through the context. A thread
 * with neither bound delegates to the context flavor and fails with {@code MissingPerApplicationContextException}.
 * Nothing is ever written to a registry whose binding is absent.
 */
public class DataSourceManager {

  /**
   * Registers a data source under a key in every per-application registry bound on the current thread.
   *
   * @param key        lookup key
   * @param dataSource the data source to register
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void register (String key, CommonDataSource dataSource) {

    if (PerApplicationScope.isBound()) {
      DataSourceScopeManager.register(key, dataSource);
      if (PerApplicationContext.isBound()) {
        DataSourceContextManager.register(key, dataSource);
      }
    } else {
      DataSourceContextManager.register(key, dataSource);
    }
  }

  /**
   * Retrieves a data source for the provided key, scope flavor first, then context flavor.
   *
   * @param key lookup key
   * @return registered {@link CommonDataSource}
   * @throws ORMInitializationException                                          if no data source is registered for the key in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static CommonDataSource getDataSource (String key) {

    CommonDataSource dataSource;

    if ((dataSource = findDataSource(key)) == null) {
      throw new ORMInitializationException("No DataSource was mapped to the key(%s)", key);
    }

    return dataSource;
  }

  /**
   * Looks up a data source by key, scope flavor first, then context flavor, without failing when none is registered.
   *
   * @param key lookup key
   * @return the registered {@link CommonDataSource}, or {@code null} if none is registered for the key in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the current thread; only an unregistered key yields {@code null}
   */
  public static CommonDataSource findDataSource (String key) {

    if (PerApplicationScope.isBound()) {

      CommonDataSource dataSource;

      if ((dataSource = DataSourceScopeManager.findDataSource(key)) != null) {

        return dataSource;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return DataSourceContextManager.findDataSource(key);
  }
}
