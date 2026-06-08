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
import java.sql.SQLException;
import javax.sql.ConnectionEvent;
import javax.sql.PooledConnection;
import org.mockito.Mockito;
import org.smallmind.quorum.pool.complex.ComplexPoolConfig;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link PooledConnectionComponentInstance}, the bridge between a {@link PooledConnection} and the
 * generic component pool. The pool, the pooled connection, and the JDBC validation collaborators are Mockito
 * mocks, so no database is involved; the tests verify listener registration, validation (no-query, success,
 * failure), {@code serve()} existential-stack capture, the connection-closed return-to-pool and
 * connection-error report/terminate paths, and idempotent {@code close()} cleanup.
 */
@Test(groups = "unit")
public class PooledConnectionComponentInstanceTest {

  private ComponentPool<PooledConnection> componentPool;
  private PooledConnection pooledConnection;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void setUp () {

    componentPool = Mockito.mock(ComponentPool.class);
    pooledConnection = Mockito.mock(PooledConnection.class);
  }

  public void testConstructorRegistersItselfAsConnectionEventListener () {

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);

    Mockito.verify(pooledConnection).addConnectionEventListener(instance);
  }

  public void testValidateWithoutQueryReturnsTrue () {

    Assert.assertTrue(new PooledConnectionComponentInstance<>(componentPool, pooledConnection).validate());
  }

  public void testValidateExecutesQueryAndReturnsTrueOnSuccess ()
    throws SQLException {

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement validationStatement = Mockito.mock(PreparedStatement.class);

    Mockito.when(pooledConnection.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement("select 1")).thenReturn(validationStatement);

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection, "select 1");

    Assert.assertTrue(instance.validate());
    Mockito.verify(validationStatement).execute();
  }

  public void testValidateReturnsFalseWhenQueryThrows ()
    throws SQLException {

    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement validationStatement = Mockito.mock(PreparedStatement.class);

    Mockito.when(pooledConnection.getConnection()).thenReturn(connection);
    Mockito.when(connection.prepareStatement("select 1")).thenReturn(validationStatement);
    Mockito.when(validationStatement.execute()).thenThrow(new SQLException("invalid"));

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection, "select 1");

    Assert.assertFalse(instance.validate(), "a failing validation query should mark the connection invalid");
  }

  public void testServeRecordsStackTraceWhenExistentiallyAware () {

    ComplexPoolConfig complexPoolConfig = Mockito.mock(ComplexPoolConfig.class);

    Mockito.when(componentPool.getComplexPoolConfig()).thenReturn(complexPoolConfig);
    Mockito.when(complexPoolConfig.isExistentiallyAware()).thenReturn(true);

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);

    Assert.assertSame(instance.serve(), pooledConnection);
    Assert.assertNotNull(instance.getExistentialStackTrace(), "an existentially aware pool should capture the serving stack trace");
  }

  public void testServeDoesNotRecordStackTraceWhenNotAware () {

    ComplexPoolConfig complexPoolConfig = Mockito.mock(ComplexPoolConfig.class);

    Mockito.when(componentPool.getComplexPoolConfig()).thenReturn(complexPoolConfig);
    Mockito.when(complexPoolConfig.isExistentiallyAware()).thenReturn(false);

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);

    Assert.assertSame(instance.serve(), pooledConnection);
    Assert.assertNull(instance.getExistentialStackTrace());
  }

  public void testConnectionClosedReturnsInstanceToPool ()
    throws Exception {

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);

    instance.connectionClosed(new ConnectionEvent(pooledConnection));

    Mockito.verify(componentPool).returnInstance(instance);
  }

  public void testConnectionErrorReportsAndTerminates ()
    throws Exception {

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);
    SQLException reportedException = new SQLException("boom");

    instance.connectionErrorOccurred(new ConnectionEvent(pooledConnection, reportedException));

    Mockito.verify(componentPool).reportErrorOccurred(reportedException);
    Mockito.verify(componentPool).terminateInstance(instance);
  }

  public void testCloseTerminatesClosesAndDeregistersExactlyOnce ()
    throws Exception {

    PooledConnectionComponentInstance<PooledConnection> instance = new PooledConnectionComponentInstance<>(componentPool, pooledConnection);

    instance.close();
    instance.close();

    Mockito.verify(componentPool, Mockito.times(1)).terminateInstance(instance);
    Mockito.verify(pooledConnection, Mockito.times(1)).close();
    Mockito.verify(pooledConnection).removeConnectionEventListener(instance);
  }
}
