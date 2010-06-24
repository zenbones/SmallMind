package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class DriverManagerConnectionPoolDataSource implements ConnectionPoolDataSource {

   private DriverManagerDataSource dataSource;
   private int maxStatements = 0;

   public DriverManagerConnectionPoolDataSource (String driverClassName, String jdbcUrl, String user, String password)
      throws SQLException {

      this(new DriverManagerDataSource(driverClassName, jdbcUrl, user, password));
   }

   public DriverManagerConnectionPoolDataSource (DriverManagerDataSource dataSource) {

      this.dataSource = dataSource;
   }

   public PooledConnection getPooledConnection ()
      throws SQLException {

      return new DriverManagerPooledConnection(dataSource, maxStatements);
   }

   public PooledConnection getPooledConnection (String user, String password)
      throws SQLException {

      return new DriverManagerPooledConnection(dataSource, user, password, maxStatements);
   }

   public int getMaxStatements () {

      return maxStatements;
   }

   public void setMaxStatements (int maxStatements) {

      this.maxStatements = maxStatements;
   }

   public PrintWriter getLogWriter ()
      throws SQLException {

      return dataSource.getLogWriter();
   }

   public void setLogWriter (PrintWriter out)
      throws SQLException {

      dataSource.setLogWriter(out);
   }

   public void setLoginTimeout (int seconds)
      throws SQLException {

      dataSource.setLoginTimeout(seconds);
   }

   public int getLoginTimeout ()
      throws SQLException {

      return dataSource.getLoginTimeout();
   }
}
