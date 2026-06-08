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
import javax.sql.CommonDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link PooledPreparedStatement}, the dynamic-proxy wrapper that lets a pooled connection recycle
 * a JDBC {@link PreparedStatement}. The owning connection is a minimal real {@link AbstractPooledConnection}
 * subclass (with statement caching disabled) carrying a Mockito {@link StatementEventListener}, and the wrapped
 * statement is a Mockito mock, so no database is involved. The tests drive the proxy's {@code close()} interception
 * (which fires {@code statementClosed} for reuse rather than closing the statement), plain delegation, the SQL-error
 * path (which must fire {@code statementErrorOccurred} on the owning connection and surface the failure), the
 * stable statement id carried on those events, log-writer delegation, and the idempotent permanent {@code close()}.
 */
@Test(groups = "unit")
public class PooledPreparedStatementTest {

  private CommonDataSource dataSource;
  private Connection actualConnection;
  private PreparedStatement actualStatement;

  @BeforeMethod
  public void setUp () {

    dataSource = Mockito.mock(CommonDataSource.class);
    actualConnection = Mockito.mock(Connection.class);
    actualStatement = Mockito.mock(PreparedStatement.class);
  }

  private TestPooledConnection pooledConnection ()
    throws SQLException {

    return new TestPooledConnection(dataSource, actualConnection);
  }

  private boolean sqlExceptionInCauseChain (Throwable throwable) {

    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException) {

        return true;
      }
    }

    return false;
  }

  public void testCloseFiresStatementClosedWithoutClosingTheActualStatement ()
    throws Exception {

    TestPooledConnection pooledConnection = pooledConnection();
    StatementEventListener listener = Mockito.mock(StatementEventListener.class);

    pooledConnection.addStatementEventListener(listener);

    PooledPreparedStatement pooledStatement = new PooledPreparedStatement(pooledConnection, actualStatement);

    pooledStatement.getPreparedStatement().close();

    Mockito.verify(listener).statementClosed(Mockito.any(StatementEvent.class));
    Mockito.verify(actualStatement, Mockito.never()).close();
  }

  public void testCloseEventCarriesTheStableStatementId ()
    throws Exception {

    TestPooledConnection pooledConnection = pooledConnection();
    StatementEventListener listener = Mockito.mock(StatementEventListener.class);

    pooledConnection.addStatementEventListener(listener);

    PooledPreparedStatement pooledStatement = new PooledPreparedStatement(pooledConnection, actualStatement);

    Assert.assertEquals(pooledStatement.getStatementId(), pooledStatement.getStatementId(), "the statement id must be stable across calls");

    pooledStatement.getPreparedStatement().close();

    ArgumentCaptor<StatementEvent> captor = ArgumentCaptor.forClass(StatementEvent.class);

    Mockito.verify(listener).statementClosed(captor.capture());
    Assert.assertEquals(((PooledPreparedStatementEvent)captor.getValue()).getStatementId(), pooledStatement.getStatementId(), "the close event must carry the pooled statement's id");
  }

  public void testNonCloseMethodDelegatesToTheActualStatement ()
    throws Exception {

    Mockito.when(actualStatement.execute()).thenReturn(true);

    PooledPreparedStatement pooledStatement = new PooledPreparedStatement(pooledConnection(), actualStatement);

    Assert.assertTrue(pooledStatement.getPreparedStatement().execute());
    Mockito.verify(actualStatement).execute();
  }

  public void testSqlErrorFiresStatementErrorOccurredAndSurfacesTheFailure ()
    throws Exception {

    Mockito.when(actualStatement.execute()).thenThrow(new SQLException("boom"));

    TestPooledConnection pooledConnection = pooledConnection();
    StatementEventListener listener = Mockito.mock(StatementEventListener.class);

    pooledConnection.addStatementEventListener(listener);

    PooledPreparedStatement pooledStatement = new PooledPreparedStatement(pooledConnection, actualStatement);

    try {
      pooledStatement.getPreparedStatement().execute();
      Assert.fail("a SQL error on a delegated call should surface to the caller");
    } catch (Throwable throwable) {
      Assert.assertTrue(sqlExceptionInCauseChain(throwable), "the delegated SQL error should reach the caller");
    }

    Mockito.verify(listener).statementErrorOccurred(Mockito.any(StatementEvent.class));
  }

  public void testGetLogWriterDelegatesToTheOwningConnection ()
    throws Exception {

    PrintWriter writer = new PrintWriter(new StringWriter());

    Mockito.when(dataSource.getLogWriter()).thenReturn(writer);

    Assert.assertSame(new PooledPreparedStatement(pooledConnection(), actualStatement).getLogWriter(), writer);
  }

  public void testPermanentCloseClosesTheActualStatementExactlyOnce ()
    throws Exception {

    PooledPreparedStatement pooledStatement = new PooledPreparedStatement(pooledConnection(), actualStatement);

    pooledStatement.close();
    pooledStatement.close();

    Mockito.verify(actualStatement, Mockito.times(1)).close();
  }

  private static class TestPooledConnection extends AbstractPooledConnection<CommonDataSource> {

    private TestPooledConnection (CommonDataSource dataSource, Connection actualConnection)
      throws SQLException {

      super(dataSource, actualConnection, 0);
    }

    @Override
    public ConnectionEvent getConnectionEvent (SQLException sqlException) {

      return (sqlException == null) ? new ConnectionEvent(this) : new ConnectionEvent(this, sqlException);
    }
  }
}
