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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.StatementEvent;
import javax.sql.StatementEventListener;

/**
 * Proxy wrapper around a JDBC {@link PreparedStatement} that notifies the owning pooled connection
 * when the statement is closed or errors occur, enabling reuse via {@link PooledPreparedStatementCache}.
 */
public class PooledPreparedStatement implements InvocationHandler {

  private final AbstractPooledConnection<?> pooledConnection;
  private final PreparedStatement actualStatement;
  private final PreparedStatement proxyStatement;
  private final String statementId;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Wraps a prepared statement originating from a pooled connection.
   *
   * @param pooledConnection connection that owns the statement
   * @param actualStatement  underlying JDBC prepared statement
   */
  public PooledPreparedStatement (AbstractPooledConnection<?> pooledConnection, PreparedStatement actualStatement) {

    this.pooledConnection = pooledConnection;
    this.actualStatement = actualStatement;

    statementId = UUID.randomUUID().toString();
    proxyStatement = (PreparedStatement)(Proxy.newProxyInstance(PooledPreparedStatement.class.getClassLoader(), new Class[] {PreparedStatement.class}, this));
  }

  /**
   * Intercepts {@code close()} to fire a {@link StatementEvent} instead of closing the underlying
   * statement, allowing it to be recycled. Other methods delegate to the wrapped statement and
   * propagate statement error events on {@link SQLException}s.
   *
   * @param proxy  the proxy instance
   * @param method invoked method
   * @param args   method arguments
   * @return result of the underlying method call
   * @throws Throwable the wrapped exception from the target method
   */
  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    if (method.getName().equals("close")) {

      StatementEvent event = new PooledPreparedStatementEvent(pooledConnection, actualStatement, statementId);

      for (StatementEventListener listener : pooledConnection.getStatementEventListeners()) {
        listener.statementClosed(event);
      }

      return null;
    } else {
      try {
        return method.invoke(actualStatement, args);
      } catch (Throwable throwable) {
        if (throwable instanceof SQLException) {

          StatementEvent event = new PooledPreparedStatementEvent(pooledConnection, actualStatement, (SQLException)throwable, statementId);

          for (StatementEventListener listener : pooledConnection.getStatementEventListeners()) {
            listener.statementErrorOccurred(event);
          }
        }

        throw throwable;
      }
    }
  }

  /**
   * Unique identifier used by the cache and statement events.
   *
   * @return generated statement id
   */
  public String getStatementId () {

    return statementId;
  }

  /**
   * @return proxied statement that will publish events on close and error
   */
  public PreparedStatement getPreparedStatement () {

    return proxyStatement;
  }

  /**
   * Delegates to the pooled connection's log writer.
   *
   * @return configured {@link PrintWriter} or {@code null}
   * @throws SQLException if the pooled connection cannot supply a writer
   */
  public PrintWriter getLogWriter ()
    throws SQLException {

    return pooledConnection.getLogWriter();
  }

  /**
   * Permanently closes the underlying statement exactly once, bypassing the reuse proxy.
   *
   * @throws SQLException if the underlying statement fails to close
   */
  public void close ()
    throws SQLException {

    if (closed.compareAndSet(false, true)) {
      actualStatement.close();
    }
  }
}
