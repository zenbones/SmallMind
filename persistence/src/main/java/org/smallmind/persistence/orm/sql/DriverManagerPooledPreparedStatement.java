package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

public class DriverManagerPooledPreparedStatement implements InvocationHandler {

   private DriverManagerPooledConnection pooledConnection;
   private PreparedStatement actualStatement;
   private PreparedStatement proxyStatement;
   private String statementId;
   private AtomicBoolean closed = new AtomicBoolean(false);

   public DriverManagerPooledPreparedStatement (DriverManagerPooledConnection pooledConnection, PreparedStatement actualStatement) {

      this.pooledConnection = pooledConnection;
      this.actualStatement = actualStatement;

      statementId = UUID.randomUUID().toString();
      proxyStatement = (PreparedStatement)(Proxy.newProxyInstance(DriverManagerPooledPreparedStatement.class.getClassLoader(), new Class[] {PreparedStatement.class}, this));
   }

   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      if (method.getName().equals("close")) {

         StatementEvent event = new DriverManagerStatementEvent(pooledConnection, actualStatement, statementId);

         for (StatementEventListener listener : pooledConnection.getStatementEventListeners()) {
            listener.statementClosed(event);
         }

         return null;
      }
      else {
         try {
            return method.invoke(actualStatement, args);
         }
         catch (Throwable throwable) {
            if (throwable instanceof SQLException) {

               StatementEvent event = new DriverManagerStatementEvent(pooledConnection, actualStatement, (SQLException)throwable, statementId);

               for (StatementEventListener listener : pooledConnection.getStatementEventListeners()) {
                  listener.statementErrorOccurred(event);
               }
            }

            throw throwable;
         }
      }
   }

   public String getStatementId () {

      return statementId;
   }

   public PreparedStatement getPreparedStatement () {

      return proxyStatement;
   }

   public PrintWriter getLogWriter ()
      throws SQLException {

      return pooledConnection.getLogWriter();
   }

   public void close ()
      throws SQLException {

      if (closed.compareAndSet(false, true)) {
         actualStatement.close();
      }
   }

   public void finalize ()
      throws SQLException {

      try {
         close();
      }
      catch (SQLException sqlExecption) {

         PrintWriter logWriter;

         if ((logWriter = getLogWriter()) != null) {
            sqlExecption.printStackTrace(logWriter);
         }
      }
   }
}
