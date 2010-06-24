package org.smallmind.persistence.orm.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

public class DriverManagerDataSource implements DataSource {

   private PrintWriter logWriter;
   private String jdbcUrl;
   private String user;
   private String password;

   public DriverManagerDataSource (String driverClassName, String jdbcUrl, String user, String password)
      throws SQLException {

      this.jdbcUrl = jdbcUrl;
      this.user = user;
      this.password = password;

      try {
         Class.forName(driverClassName);
      }
      catch (ClassNotFoundException classNotFoundException) {
         throw new SQLException(classNotFoundException);
      }
   }

   public Connection getConnection ()
      throws SQLException {

      return DriverManager.getConnection(jdbcUrl, user, password);
   }

   public Connection getConnection (String user, String password)
      throws SQLException {

      return DriverManager.getConnection(jdbcUrl, user, password);
   }

   public PrintWriter getLogWriter () {

      return logWriter;
   }

   public void setLogWriter (PrintWriter logWriter) {

      this.logWriter = logWriter;
   }

   public void setLoginTimeout (int timeoutSeconds) {

      throw new UnsupportedOperationException();
   }

   public int getLoginTimeout () {

      return 0;
   }

   public boolean isWrapperFor (Class<?> iface) {

      return false;
   }

   public <T> T unwrap (Class<T> iface) {

      return null;
   }
}
