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
package org.smallmind.persistence.orm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.StaticManager;

public class SessionManager implements StaticManager {

  private static final InheritableThreadLocal<Map<String, ProxySession>> SESSION_MAP_LOCAL = new InheritableThreadLocal<Map<String, ProxySession>>() {

    @Override
    protected Map<String, ProxySession> initialValue () {

      return new ConcurrentHashMap<String, ProxySession>();
    }
  };

  public static void register (String dataSourceKey, ProxySession proxySession) {

    SESSION_MAP_LOCAL.get().put(dataSourceKey, proxySession);
  }

  public static ProxySession getSession () {

    return getSession(null);
  }

  public static ProxySession getSession (String dataSourceKey) {

    ProxySession proxySession;

    if ((proxySession = SESSION_MAP_LOCAL.get().get(dataSourceKey)) == null) {
      throw new ORMInitializationException("No ProxySession was mapped to the data source value(%s)", dataSourceKey);
    }

    return proxySession;
  }

  public static void closeSession () {

    closeSession(null);
  }

  public static void closeSession (String dataSourceKey) {

    getSession(dataSourceKey).close();
  }
}
