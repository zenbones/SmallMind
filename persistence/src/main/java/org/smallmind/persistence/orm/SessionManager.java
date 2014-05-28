/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.persistence.orm;

import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;

public class SessionManager implements PerApplicationDataManager {

  static {

    PerApplicationContext.setPerApplicationData(SessionManager.class, new ConcurrentHashMap<String, ProxySession>());
  }

  public static void register (String dataSourceKey, ProxySession proxySession) {

    PerApplicationContext.getPerApplicationData(SessionManager.class, ConcurrentHashMap.class).put(dataSourceKey, proxySession);
  }

  public static ProxySession getSession () {

    return getSession(null);
  }

  public static ProxySession getSession (String dataSourceKey) {

    ProxySession proxySession;

    if ((proxySession = (ProxySession)PerApplicationContext.getPerApplicationData(SessionManager.class, ConcurrentHashMap.class).get(dataSourceKey)) == null) {
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
