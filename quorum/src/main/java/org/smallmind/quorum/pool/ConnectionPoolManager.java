/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.StaticManager;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionPoolManager implements StaticManager {

   private static final InheritableThreadLocal<Map<String, ConnectionPool>> POOL_MAP_LOCAL = new InheritableThreadLocal<Map<String, ConnectionPool>>() {

      @Override
      protected Map<String, ConnectionPool> initialValue () {

         return new ConcurrentHashMap<String, ConnectionPool>();
      }
   };

   public static void register (ConnectionPool connectionPool) {

      POOL_MAP_LOCAL.get().put(connectionPool.getPoolName(), connectionPool);
   }

   public static Object getRawConnection (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).rawConnection();
   }

   public static Object getConnection (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).getConnection();
   }

   public static int getPoolSize (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).getPoolSize();
   }

   public static int getFreeSize (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).getFreeSize();
   }

   private static ConnectionPool getConnectionPool (String poolName)
      throws ConnectionPoolException {

      ConnectionPool pool;

      if ((pool = POOL_MAP_LOCAL.get().get(poolName)) == null) {
         throw new ConnectionPoolException("No ConnectionPool defined for name (%s)", poolName);
      }

      return pool;
   }

   public static void logInfo (String message) {

      log(Level.INFO, message);
   }

   public static void logError (String message, Throwable throwable) {

      log(Level.ERROR, message, throwable);
   }

   public static void logError (Throwable throwable) {

      log(Level.ERROR, throwable);
   }

   public static void log (Level level, String message) {

      LoggerManager.getLogger(ConnectionPoolManager.class).log(level, message);
   }

   public static void log (Level level, Throwable throwable) {

      LoggerManager.getLogger(ConnectionPoolManager.class).log(level, throwable);
   }

   public static void log (Level level, String message, Throwable throwable) {

      LoggerManager.getLogger(ConnectionPoolManager.class).log(level, message, throwable);
   }
}
