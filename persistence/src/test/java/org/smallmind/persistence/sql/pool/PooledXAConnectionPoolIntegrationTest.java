/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.sql.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import com.mysql.cj.jdbc.MysqlXADataSource;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.sql.testbench.DataSourceAvailableTestCondition;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.testbench.condition.TestConditions;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test for the XA branch of the JDBC connection-pooling stack against a real MySQL container.
 * It mirrors {@link PooledConnectionPoolIntegrationTest} but drives the distributed-transaction path: a
 * {@link DataSourceFactory} producing MySQL {@link MysqlXADataSource}s feeds a
 * {@link DataSourceComponentInstanceFactory}, whose {@link OmnivorousConnectionPoolDataSource} wrapper is
 * auto-detected by {@code PooledConnectionFactory} as XA and served as an {@link XADataSourcePooledConnection}.
 * The pool is exposed through a {@link PooledXADataSource}, proving XA connections are validated, served,
 * expose an {@link XAResource}, returned to the pool when their logical connection closes, and released on shutdown.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class PooledXAConnectionPoolIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";
  private static final String VALIDATION_QUERY = "select 1";

  public PooledXAConnectionPoolIntegrationTest () {

    super(DockerApplication.MYSQL);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    TestConditions.serial(120, new DataSourceAvailableTestCondition(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD));
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @BeforeMethod
  public void ensurePerApplicationContext () {

    // The complex pool resolves its metric registry through the per-application context, which must exist on the test thread.
    new PerApplicationContext();
  }

  private DataSourceFactory<XADataSource, XAConnection> xaDataSourceFactory () {

    return new DataSourceFactory<>() {

      @Override
      public Class<XAConnection> getPooledConnectionClass () {

        return XAConnection.class;
      }

      @Override
      public Class<XADataSource> getDataSourceClass () {

        return XADataSource.class;
      }

      @Override
      public XADataSource constructDataSource (String jdbcUrl, String user, String password)
        throws SQLException {

        MysqlXADataSource dataSource = new MysqlXADataSource();

        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);

        return dataSource;
      }
    };
  }

  private ComplexPoolConfig poolConfig (int maxPoolSize) {

    return new ComplexPoolConfig()
             .setInitialPoolSize(1)
             .setMinPoolSize(1)
             .setMaxPoolSize(maxPoolSize)
             .setTestOnCreate(true)
             .setTestOnAcquire(true)
             .setAcquireWaitTimeMillis(5000);
  }

  private int selectOne (Connection connection)
    throws SQLException {

    try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(VALIDATION_QUERY)) {
      Assert.assertTrue(resultSet.next(), "the validation query should yield a row");

      return resultSet.getInt(1);
    }
  }

  public void testXaPooledStackServesReusesAndShutsDown ()
    throws Exception {

    DataSourceComponentInstanceFactory<XADataSource, XAConnection> factory = new DataSourceComponentInstanceFactory<>(xaDataSourceFactory(), JDBC_URL, USER_NAME, PASSWORD);

    factory.setValidationQuery(VALIDATION_QUERY);

    ComponentPool<XAConnection> componentPool = new ComponentPool<>("xa-pool", factory, poolConfig(4));
    PooledXADataSource pooledXADataSource = new PooledXADataSource("xa-ds", componentPool);

    try {
      pooledXADataSource.startup();

      XAConnection xaConnection = pooledXADataSource.getXAConnection();

      Assert.assertNotNull(xaConnection.getXAResource(), "a pooled XA connection should expose its XAResource for transaction enlistment");

      // Closing the logical connection returns the pooled XA connection to the pool.
      try (Connection connection = xaConnection.getConnection()) {
        Assert.assertEquals(selectOne(connection), 1, "a borrowed pooled XA connection should execute the validation query");
        Assert.assertTrue(componentPool.getProcessingSize() >= 1, "the pool should report the XA connection as checked out while it is open");
      }

      // Sequential borrow/return cycles must reuse XA connections rather than grow the pool unbounded.
      for (int index = 0; index < 8; index++) {
        XAConnection reusedXAConnection = pooledXADataSource.getXAConnection();
        try (Connection connection = reusedXAConnection.getConnection()) {
          Assert.assertEquals(selectOne(connection), 1);
        }
      }

      Assert.assertEquals(componentPool.getProcessingSize(), 0, "no XA connection should remain checked out after every borrow has been closed");
      Assert.assertTrue(componentPool.getPoolSize() <= 4, "reused XA connections must keep the pool within its configured maximum, proving they are not leaked");
    } finally {
      pooledXADataSource.shutdown();
    }

    Assert.assertEquals(componentPool.getPoolSize(), 0, "shutting the pool down should release every pooled XA connection");
  }
}
