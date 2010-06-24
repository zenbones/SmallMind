package org.smallmind.persistence.orm.sql.pool;

import java.sql.SQLException;
import org.smallmind.persistence.orm.sql.DriverManagerConnectionPoolDataSource;
import org.smallmind.persistence.orm.sql.DriverManagerDataSource;
import org.smallmind.quorum.pool.ConnectionInstance;
import org.smallmind.quorum.pool.ConnectionInstanceFactory;
import org.smallmind.quorum.pool.ConnectionPool;

public class DriverManagerConnectionInstanceFactory implements ConnectionInstanceFactory {

   private DriverManagerDataSource dataSource;
   private DriverManagerConnectionPoolDataSource pooledDataSource;

   public DriverManagerConnectionInstanceFactory (String driverClassName, String jdbcUrl, String user, String password)
      throws SQLException {

      dataSource = new DriverManagerDataSource(driverClassName, jdbcUrl, user, password);
      pooledDataSource = new DriverManagerConnectionPoolDataSource(dataSource);
   }

   public int getMaxStatements () {

      return pooledDataSource.getMaxStatements();
   }

   public void setMaxStatements (int maxStatements) {

      pooledDataSource.setMaxStatements(maxStatements);
   }

   public Object rawInstance ()
      throws SQLException {

      return dataSource.getConnection();
   }

   public ConnectionInstance createInstance (ConnectionPool connectionPool)
      throws SQLException {

      return new PooledConnectionInstance(connectionPool, pooledDataSource.getPooledConnection());
   }
}