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
package org.smallmind.persistence.sql;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.smallmind.nutsnbolts.lang.StaticManager;
import org.smallmind.persistence.orm.ORMInitializationException;

public class DataSourceManager implements StaticManager {

  private static final InheritableThreadLocal<Map<String, DataSource>> SOURCE_MAP_LOCAL = new InheritableThreadLocal<Map<String, DataSource>>() {

    @Override
    protected Map<String, DataSource> initialValue () {

      return new ConcurrentHashMap<String, DataSource>();
    }
  };

  public static void register (String key, DataSource dataSource) {

    SOURCE_MAP_LOCAL.get().put(key, dataSource);
  }

  public static DataSource getDataSource (String key) {

    DataSource dataSource;

    if ((dataSource = SOURCE_MAP_LOCAL.get().get(key)) == null) {
      throw new ORMInitializationException("No DataSource was mapped to the key(%s)", key);
    }

    return dataSource;
  }
}
