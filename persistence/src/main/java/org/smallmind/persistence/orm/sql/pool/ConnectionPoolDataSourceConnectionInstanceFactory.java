package org.smallmind.persistence.orm.sql.pool;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import org.smallmind.quorum.pool.ConnectionInstance;
import org.smallmind.quorum.pool.ConnectionInstanceFactory;
import org.smallmind.quorum.pool.ConnectionPool;

public class ConnectionPoolDataSourceConnectionInstanceFactory implements ConnectionInstanceFactory {

   private DataSource dataSource;
   private ConnectionPoolDataSource pooledDataSource;
   private String validationQuery = "select 1";

   public ConnectionPoolDataSourceConnectionInstanceFactory (ConnectionPoolDataSource pooledDataSource) {

      this(null, pooledDataSource);
   }

   public ConnectionPoolDataSourceConnectionInstanceFactory (DataSource dataSource, ConnectionPoolDataSource pooledDataSource) {

      this.dataSource = dataSource;
      this.pooledDataSource = pooledDataSource;
   }

   public String getValidationQuery () {

      return validationQuery;
   }

   public void setValidationQuery (String validationQuery) {

      this.validationQuery = validationQuery;
   }

   public Object rawInstance ()
      throws SQLException {

      if (dataSource == null) {
         throw new UnsupportedOperationException("No standard (unpooled) data source is available");
      }

      return dataSource.getConnection();
   }

   public ConnectionInstance createInstance (ConnectionPool connectionPool)
      throws SQLException {

      return new PooledConnectionInstance(connectionPool, pooledDataSource.getPooledConnection(), validationQuery);
   }
}
