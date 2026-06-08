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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import org.smallmind.persistence.sql.DataSourceManager;
import org.smallmind.persistence.sql.DriverManagerDataSourceFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.persistence.sql.testbench.DataSourceAvailableTestCondition;
import org.smallmind.quorum.pool.ComponentPoolException;
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
 * Integration test for the JDBC connection-pooling stack against a real MySQL container started through
 * the docker testbench. It drives the whole stack end to end — {@link DriverManagerComponentInstanceFactory}
 * and {@link DataSourceComponentInstanceFactory} feeding a {@link ComponentPool}, exposed as a
 * {@link PooledDataSource} — proving that connections are validated, served, reused across borrow/return
 * cycles without leaking, registered in the {@link DataSourceManager}, and released cleanly on shutdown.
 *
 * <p>The prepared-statement path is exercised against a single-connection pool so a physical connection is
 * borrowed, returned, and borrowed again, covering {@link org.smallmind.persistence.sql.AbstractPooledConnection}'s
 * statement cache and the {@link org.smallmind.persistence.sql.PooledPreparedStatement} proxy with a live driver.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class PooledConnectionPoolIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";
  private static final String VALIDATION_QUERY = "select 1";

  public PooledConnectionPoolIntegrationTest () {

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

  @SuppressWarnings("unchecked")
  public void testDriverManagerPooledStackServesReusesAndShutsDown ()
    throws Exception {

    DriverManagerComponentInstanceFactory factory = new DriverManagerComponentInstanceFactory(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD);

    factory.setValidationQuery(VALIDATION_QUERY);

    ComponentPool<PooledConnection> componentPool = new ComponentPool<>("driver-manager-pool", factory, poolConfig(4));
    PooledDataSource pooledDataSource = new PooledDataSource("driver-manager-ds", componentPool);

    pooledDataSource.register();

    try {
      pooledDataSource.startup();

      Assert.assertSame(DataSourceManager.getDataSource("driver-manager-ds"), pooledDataSource, "the pooled data source should be retrievable from the DataSourceManager under its key");

      try (Connection connection = pooledDataSource.getConnection()) {
        Assert.assertEquals(selectOne(connection), 1, "a borrowed pooled connection should execute the validation query");
        Assert.assertTrue(componentPool.getProcessingSize() >= 1, "the pool should report the connection as checked out while it is open");
      }

      // Sequential borrow/return cycles must reuse connections rather than grow the pool unbounded.
      for (int index = 0; index < 8; index++) {
        try (Connection connection = pooledDataSource.getConnection()) {
          Assert.assertEquals(selectOne(connection), 1);
        }
      }

      Assert.assertEquals(componentPool.getProcessingSize(), 0, "no connection should remain checked out after every borrow has been closed");
      Assert.assertTrue(componentPool.getPoolSize() <= 4, "reused connections must keep the pool within its configured maximum, proving they are not leaked");
    } finally {
      pooledDataSource.shutdown();
    }

    Assert.assertEquals(componentPool.getPoolSize(), 0, "shutting the pool down should release every pooled connection");
  }

  @SuppressWarnings("unchecked")
  public void testDataSourceFactoryPooledStackServesConnections ()
    throws Exception {

    DriverManagerDataSourceFactory dataSourceFactory = new DriverManagerDataSourceFactory();

    dataSourceFactory.setDriverClassName(DRIVER_CLASS_NAME);

    DataSourceComponentInstanceFactory<DataSource, PooledConnection> factory = new DataSourceComponentInstanceFactory<>(dataSourceFactory, JDBC_URL, USER_NAME, PASSWORD);

    factory.setValidationQuery(VALIDATION_QUERY);

    ComponentPool<PooledConnection> componentPool = new ComponentPool<>("data-source-pool", factory, poolConfig(2));
    PooledDataSource pooledDataSource = new PooledDataSource("data-source-ds", componentPool);

    try {
      pooledDataSource.startup();

      try (Connection connection = pooledDataSource.getConnection()) {
        Assert.assertEquals(selectOne(connection), 1, "a connection served through the DataSource-backed factory should execute the validation query");
      }
    } finally {
      pooledDataSource.shutdown();
    }

    Assert.assertEquals(componentPool.getPoolSize(), 0, "the DataSource-backed pool should release its connections on shutdown");
  }

  @SuppressWarnings("unchecked")
  public void testPreparedStatementCachingAcrossReuse ()
    throws Exception {

    // A single-connection pool guarantees the same physical connection is borrowed again, exercising the
    // per-connection prepared-statement cache (maxStatements > 0) and the PooledPreparedStatement proxy.
    DriverManagerComponentInstanceFactory factory = new DriverManagerComponentInstanceFactory(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD, 16);

    factory.setValidationQuery(VALIDATION_QUERY);

    ComponentPool<PooledConnection> componentPool = new ComponentPool<>("statement-cache-pool", factory, poolConfig(1));
    PooledDataSource pooledDataSource = new PooledDataSource("statement-cache-ds", componentPool);

    try {
      pooledDataSource.startup();

      try (Connection connection = pooledDataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select ?")) {
        preparedStatement.setInt(1, 7);
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          Assert.assertTrue(resultSet.next());
          Assert.assertEquals(resultSet.getInt(1), 7, "the first prepared statement should round-trip its parameter");
        }
      }

      // The connection (and its cached statement) returns to the pool; re-borrowing must serve a working statement.
      try (Connection connection = pooledDataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select ?")) {
        preparedStatement.setInt(1, 9);
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          Assert.assertTrue(resultSet.next());
          Assert.assertEquals(resultSet.getInt(1), 9, "a prepared statement served from the reused connection should round-trip its parameter");
        }
      }
    } finally {
      pooledDataSource.shutdown();
    }
  }
}
