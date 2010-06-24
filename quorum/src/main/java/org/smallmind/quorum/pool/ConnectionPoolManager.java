package org.smallmind.quorum.pool;

import java.util.HashMap;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionPoolManager {

   private static final HashMap<String, ConnectionPool> POOL_MAP = new HashMap<String, ConnectionPool>();

   public static void addConnectionPool (ConnectionPool connectionPool) {

      synchronized (POOL_MAP) {
         POOL_MAP.put(connectionPool.getPoolName(), connectionPool);
      }
   }

   public static Object rawConnection (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).rawConnection();
   }

   public static Object getConnection (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).getConnection();
   }

   public static int poolSize (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).poolSize();
   }

   public static int freeSize (String poolName)
      throws ConnectionPoolException {

      return getConnectionPool(poolName).freeSize();
   }

   private static ConnectionPool getConnectionPool (String poolName)
      throws ConnectionPoolException {

      synchronized (POOL_MAP) {
         if (POOL_MAP.containsKey(poolName)) {
            return POOL_MAP.get(poolName);
         }
      }

      throw new ConnectionPoolException("No ConnectionPool defined for name (%s)", poolName);
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
