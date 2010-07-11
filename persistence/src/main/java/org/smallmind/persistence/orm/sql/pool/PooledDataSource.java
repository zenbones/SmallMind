package org.smallmind.persistence.orm.sql.pool;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.orm.sql.DataSourceManager;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolException;

public class PooledDataSource implements DataSource {

   private ConnectionPool connectionPool;
   private PrintWriter logWriter;
   private String key;

   public PooledDataSource (String key, ConnectionPool connectionPool) {

      this.key = key;
      this.connectionPool = connectionPool;

      logWriter = new PrintWriter(new PooledLogWriter());
   }

   public void register () {

      DataSourceManager.registerDataSource(key, this);
   }

   public String getKey () {

      return key;
   }

   public Connection getConnection ()
      throws SQLException {

      try {
         return ((PooledConnection)connectionPool.getConnection()).getConnection();
      }
      catch (ConnectionPoolException connectionPoolException) {
         throw new SQLException(connectionPoolException);
      }
   }

   public Connection getConnection (String username, String password) {

      throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
   }

   public PrintWriter getLogWriter () {

      return logWriter;
   }

   public void setLogWriter (PrintWriter out) {

      throw new UnsupportedOperationException("Please properly configure the underlying pool which is represented by this DataSource");
   }

   public void setLoginTimeout (int seconds) {

      throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
   }

   public int getLoginTimeout () {

      throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
   }

   public boolean isWrapperFor (Class<?> iface) {

      return false;
   }

   public <T> T unwrap (Class<T> iface) {

      throw new UnsupportedOperationException("This DataSource represents a connection pool");
   }
}
