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

import java.util.concurrent.ConcurrentHashMap;
import javax.sql.CommonDataSource;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.orm.ORMInitializationException;

/**
 * Application-scoped registry mapping string keys to {@link CommonDataSource} instances, resolved through a
 * {@link PerApplicationScope}. This is the scope flavor of {@link DataSourceContextManager}: the calling thread must
 * be inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, or a thread created by
 * {@code PerApplicationScope.wrapThreadFactory} or the scope's own {@code boundThreadFactory} / {@code boundExecutor},
 * otherwise {@code MissingPerApplicationScopeException} is thrown.
 * Library code in this module (the pooled data sources' {@code register()}) resolves through {@link DataSourceManager},
 * which writes here whenever a scope is bound on the calling thread and reads here first.
 */
public class DataSourceScopeManager implements PerApplicationDataManager {

  /**
   * Registers a data source under the given key in the per-application scope bound to the current thread.
   *
   * @param key        lookup key
   * @param dataSource data source to store
   */
  public static void register (String key, CommonDataSource dataSource) {

    ConcurrentHashMap<String, CommonDataSource> dataSourceMap;

    if ((dataSourceMap = getDataSourceMap()) == null) {
      dataSourceMap = PerApplicationScope.setPerApplicationDataIfAbsent(DataSourceScopeManager.class, new ConcurrentHashMap<>());
    }
    dataSourceMap.put(key, dataSource);
  }

  /**
   * Returns the data source registered under the given key.
   *
   * @param key lookup key
   * @return the registered data source
   * @throws ORMInitializationException if no data source is registered under the key
   */
  public static CommonDataSource getDataSource (String key) {

    CommonDataSource dataSource;

    if ((dataSource = findDataSource(key)) == null) {
      throw new ORMInitializationException("No DataSource was mapped to the key(%s)", key);
    }

    return dataSource;
  }

  /**
   * Looks up a data source by key without failing when none is registered.
   *
   * @param key lookup key
   * @return the registered {@link CommonDataSource}, or {@code null} if none is registered for the key
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if no per-application scope is bound on the current thread; only an unregistered key yields {@code null}
   */
  public static CommonDataSource findDataSource (String key) {

    ConcurrentHashMap<String, CommonDataSource> dataSourceMap;

    return ((dataSourceMap = getDataSourceMap()) == null) ? null : dataSourceMap.get(key);
  }

  private static ConcurrentHashMap<String, CommonDataSource> getDataSourceMap () {

    return PerApplicationScope.getPerApplicationData(DataSourceScopeManager.class, ConcurrentHashMap.class);
  }
}
