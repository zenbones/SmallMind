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
package org.smallmind.persistence.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import javax.sql.CommonDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AbstractPooledConnection}, which proxies a real JDBC {@link Connection} to intercept
 * {@code close()} and to surface connection errors as pool events. The data source and the wrapped connection
 * are Mockito mocks, so no database is involved; the tests drive the proxy's close interception, plain
 * delegation, the error path (errors rethrown unchanged, with the error event fired only when the connection
 * is unusable), idempotent {@code close()}, log-writer delegation, and listener registration.
 */
@Test(groups = "unit")
public class AbstractPooledConnectionTest {

  private static final int VALIDITY_TIMEOUT_SECONDS = 2;

  private CommonDataSource dataSource;
  private Connection actualConnection;

  @BeforeMethod
  public void setUp () {

    dataSource = Mockito.mock(CommonDataSource.class);
    actualConnection = Mockito.mock(Connection.class);
  }

  private TestPooledConnection pooledConnection (int maxStatements)
    throws SQLException {

    return new TestPooledConnection(dataSource, actualConnection, maxStatements, VALIDITY_TIMEOUT_SECONDS);
  }

  @Test(groups = "unit", expectedExceptions = SQLException.class)
  public void testNegativeMaxStatementsIsRejected ()
    throws SQLException {

    new TestPooledConnection(dataSource, actualConnection, -1, VALIDITY_TIMEOUT_SECONDS);
  }

  @Test(groups = "unit", expectedExceptions = SQLException.class)
  public void testNonPositiveValidityTimeoutIsRejected ()
    throws SQLException {

    new TestPooledConnection(dataSource, actualConnection, 0, 0);
  }

  public void testProxyCloseFiresConnectionClosedWithoutClosingTheActualConnection ()
    throws Exception {

    TestPooledConnection pooledConnection = pooledConnection(0);
    ConnectionEventListener listener = Mockito.mock(ConnectionEventListener.class);

    pooledConnection.addConnectionEventListener(listener);
    pooledConnection.getConnection().close();

    Mockito.verify(listener).connectionClosed(Mockito.any(ConnectionEvent.class));
    Mockito.verify(actualConnection, Mockito.never()).close();
  }

  public void testNonCloseMethodDelegatesToTheActualConnection ()
    throws Exception {

    Mockito.when(actualConnection.getCatalog()).thenReturn("inventory");

    Assert.assertEquals(pooledConnection(0).getConnection().getCatalog(), "inventory");
  }

  private ConnectionEventListener assertRethrown (TestPooledConnection pooledConnection, SQLException sqlException)
    throws Exception {

    ConnectionEventListener listener = Mockito.mock(ConnectionEventListener.class);

    Mockito.when(actualConnection.getCatalog()).thenThrow(sqlException);
    pooledConnection.addConnectionEventListener(listener);

    try {
      pooledConnection.getConnection().getCatalog();
      Assert.fail("a SQL error on a delegated call should be rethrown");
    } catch (SQLException thrownException) {
      Assert.assertSame(thrownException, sqlException, "the driver's exception should reach the caller unchanged");
    }

    return listener;
  }

  public void testStatementLevelErrorIsRethrownWithoutDiscardingTheConnection ()
    throws Exception {

    Mockito.when(actualConnection.isValid(Mockito.anyInt())).thenReturn(true);

    ConnectionEventListener listener = assertRethrown(pooledConnection(0), new SQLException("Table not found", "42S02"));

    Mockito.verify(listener, Mockito.never()).connectionErrorOccurred(Mockito.any(ConnectionEvent.class));
  }

  public void testConnectionLevelSqlStateFiresConnectionErrorWithoutAskingTheConnection ()
    throws Exception {

    ConnectionEventListener listener = assertRethrown(pooledConnection(0), new SQLException("Communications link failure", "08S01"));

    Mockito.verify(listener).connectionErrorOccurred(Mockito.any(ConnectionEvent.class));
    Mockito.verify(actualConnection, Mockito.never()).isValid(Mockito.anyInt());
  }

  public void testConnectionExceptionTypeFiresConnectionError ()
    throws Exception {

    ConnectionEventListener listener = assertRethrown(pooledConnection(0), new SQLNonTransientConnectionException("connection lost"));

    Mockito.verify(listener).connectionErrorOccurred(Mockito.any(ConnectionEvent.class));
  }

  public void testErrorOnAnInvalidConnectionFiresConnectionError ()
    throws Exception {

    Mockito.when(actualConnection.isValid(Mockito.anyInt())).thenReturn(false);

    ConnectionEventListener listener = assertRethrown(pooledConnection(0), new SQLException("vendor failure", "HY000"));

    Mockito.verify(listener).connectionErrorOccurred(Mockito.any(ConnectionEvent.class));
  }

  public void testFailedValidityCheckFiresConnectionError ()
    throws Exception {

    Mockito.when(actualConnection.isValid(Mockito.anyInt())).thenThrow(new SQLException("isValid unsupported"));

    ConnectionEventListener listener = assertRethrown(pooledConnection(0), new SQLException("vendor failure", "HY000"));

    Mockito.verify(listener).connectionErrorOccurred(Mockito.any(ConnectionEvent.class));
  }

  public void testValidityTimeoutIsPassedToIsValid ()
    throws Exception {

    Mockito.when(actualConnection.isValid(Mockito.anyInt())).thenReturn(true);

    assertRethrown(new TestPooledConnection(dataSource, actualConnection, 0, 3), new SQLException("Table not found", "42S02"));

    Mockito.verify(actualConnection).isValid(3);
  }

  public void testCloseClosesTheActualConnectionExactlyOnce ()
    throws Exception {

    TestPooledConnection pooledConnection = pooledConnection(0);

    pooledConnection.close();
    pooledConnection.close();

    Mockito.verify(actualConnection, Mockito.times(1)).close();
  }

  public void testGetLogWriterDelegatesToTheDataSource ()
    throws Exception {

    PrintWriter writer = new PrintWriter(new StringWriter());

    Mockito.when(dataSource.getLogWriter()).thenReturn(writer);

    Assert.assertSame(pooledConnection(0).getLogWriter(), writer);
  }

  public void testRemovedListenerNoLongerReceivesCloseEvents ()
    throws Exception {

    TestPooledConnection pooledConnection = pooledConnection(0);
    ConnectionEventListener listener = Mockito.mock(ConnectionEventListener.class);

    pooledConnection.addConnectionEventListener(listener);
    pooledConnection.removeConnectionEventListener(listener);
    pooledConnection.getConnection().close();

    Mockito.verify(listener, Mockito.never()).connectionClosed(Mockito.any(ConnectionEvent.class));
  }

  public void testSameArgsPrepareStatementReusesTheCachedWrapper ()
    throws Exception {

    PreparedStatement actualStatement = Mockito.mock(PreparedStatement.class);

    Mockito.when(actualConnection.prepareStatement("select 1")).thenReturn(actualStatement);

    Connection connection = pooledConnection(4).getConnection();

    PreparedStatement first = connection.prepareStatement("select 1");

    // Returning the statement (close on the reuse proxy) frees the cache entry for the same args.
    first.close();

    PreparedStatement second = connection.prepareStatement("select 1");

    Assert.assertSame(second, first, "the same args should hand back the cached statement wrapper once it is freed");
    Mockito.verify(actualConnection, Mockito.times(1)).prepareStatement("select 1");
  }

  public void testDistinctArgsPrepareStatementProduceDistinctStatements ()
    throws Exception {

    Mockito.when(actualConnection.prepareStatement("select 1")).thenReturn(Mockito.mock(PreparedStatement.class));
    Mockito.when(actualConnection.prepareStatement("select 2")).thenReturn(Mockito.mock(PreparedStatement.class));

    Connection connection = pooledConnection(4).getConnection();

    PreparedStatement first = connection.prepareStatement("select 1");
    PreparedStatement second = connection.prepareStatement("select 2");

    Assert.assertNotSame(second, first, "differing args must not collide on a single cached statement");
    Mockito.verify(actualConnection).prepareStatement("select 1");
    Mockito.verify(actualConnection).prepareStatement("select 2");
  }

  public void testCloseClosesCachedStatementsAndTheActualConnection ()
    throws Exception {

    PreparedStatement actualStatement = Mockito.mock(PreparedStatement.class);

    Mockito.when(actualConnection.prepareStatement("select 1")).thenReturn(actualStatement);

    TestPooledConnection pooledConnection = pooledConnection(4);

    pooledConnection.getConnection().prepareStatement("select 1");
    pooledConnection.close();

    Mockito.verify(actualStatement).close();
    Mockito.verify(actualConnection).close();
  }

  private static class TestPooledConnection extends AbstractPooledConnection<CommonDataSource> {

    private TestPooledConnection (CommonDataSource dataSource, Connection actualConnection, int maxStatements, int validityTimeoutSeconds)
      throws SQLException {

      super(dataSource, actualConnection, maxStatements, validityTimeoutSeconds);
    }

    @Override
    public ConnectionEvent getConnectionEvent (SQLException sqlException) {

      return (sqlException == null) ? new ConnectionEvent(this) : new ConnectionEvent(this, sqlException);
    }
  }
}
