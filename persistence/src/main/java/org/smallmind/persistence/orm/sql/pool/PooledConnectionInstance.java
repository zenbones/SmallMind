package org.smallmind.persistence.orm.sql.pool;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool.ConnectionInstance;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolManager;

public class PooledConnectionInstance implements ConnectionInstance, ConnectionEventListener {

   private ConnectionPool connectionPool;
   private PooledConnection pooledConnection;
   private PreparedStatement validationStatement;

   public PooledConnectionInstance (ConnectionPool connectionPool, PooledConnection pooledConnection)
      throws SQLException {

      this(connectionPool, pooledConnection, "Select 1");
   }

   public PooledConnectionInstance (ConnectionPool connectionPool, PooledConnection pooledConnection, String validationQuery)
      throws SQLException {

      this.connectionPool = connectionPool;
      this.pooledConnection = pooledConnection;

      if (validationQuery != null) {
         validationStatement = pooledConnection.getConnection().prepareStatement(validationQuery);
      }

      pooledConnection.addConnectionEventListener(this);
   }

   public boolean validate () {

      if (validationStatement != null) {
         try {
            validationStatement.execute();
         }
         catch (SQLException sqlException) {
            return false;
         }
      }

      return true;
   }

   public void connectionClosed (ConnectionEvent connectionEvent) {

      try {
         connectionPool.returnInstance(this);
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
   }

   public void connectionErrorOccurred (ConnectionEvent connectionEvent) {

      Exception reportedException = connectionEvent.getSQLException();

      try {
         connectionPool.terminateInstance(this);
      }
      catch (Exception exception) {
         if (reportedException != null) {
            exception.initCause(reportedException);
         }

         reportedException = exception;
      }
      finally {
         if (reportedException != null) {
            ConnectionPoolManager.logError(reportedException);
         }
      }
   }

   public Object serve () {

      return pooledConnection;
   }

   public void close ()
      throws SQLException {

      SQLException validationCloseException = null;

      if (validationStatement != null) {
         try {
            validationStatement.close();
         }
         catch (SQLException sqlException) {
            validationCloseException = sqlException;
         }
      }

      try {
         pooledConnection.close();
      }
      catch (SQLException sqlException) {

         if (validationCloseException != null) {
            sqlException.initCause(validationCloseException);
         }

         throw sqlException;
      }

      if (validationCloseException != null) {
         throw validationCloseException;
      }
   }
}