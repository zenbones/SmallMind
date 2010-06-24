package org.smallmind.cloud.namespace.java.pool;

import javax.naming.NamingException;
import org.smallmind.cloud.namespace.java.PooledJavaContext;
import org.smallmind.cloud.namespace.java.event.JavaContextEvent;
import org.smallmind.cloud.namespace.java.event.JavaContextListener;
import org.smallmind.quorum.pool.ConnectionInstance;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolManager;

public class JavaContextConnectionInstance implements ConnectionInstance, JavaContextListener {

   private ConnectionPool connectionPool;
   private PooledJavaContext pooledJavaContext;

   public JavaContextConnectionInstance (ConnectionPool connectionPool, PooledJavaContext pooledJavaContext)
      throws NamingException {

      this.connectionPool = connectionPool;
      this.pooledJavaContext = pooledJavaContext;

      pooledJavaContext.addJavaContextListener(this);
   }

   public boolean validate () {

      try {
         pooledJavaContext.lookup("");
      }
      catch (NamingException namingException) {

         return false;
      }

      return true;
   }

   public void contextClosed (JavaContextEvent javaContextEvent) {

      try {
         connectionPool.returnInstance(this);
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
   }

   public void contextAborted (JavaContextEvent javaContextEvent) {

      try {
         connectionPool.terminateInstance(this);
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
   }

   public Object serve ()
      throws Exception {

      return pooledJavaContext;
   }

   public void close ()
      throws Exception {

      pooledJavaContext.close(true);
   }
}
