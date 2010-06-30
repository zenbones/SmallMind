package org.smallmind.persistence.orm;

import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

   private static final ConcurrentHashMap<String, ProxySession> SESSION_MAP = new ConcurrentHashMap<String, ProxySession>();

   public static void registerSession (String dataSourceKey, ProxySession proxySession) {

      SESSION_MAP.put(dataSourceKey, proxySession);
   }

   public static ProxySession getSession () {

      return getSession(null);
   }

   public static ProxySession getSession (String dataSourceKey) {

      ProxySession proxySession;

      if ((proxySession = SESSION_MAP.get(dataSourceKey)) == null) {
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
