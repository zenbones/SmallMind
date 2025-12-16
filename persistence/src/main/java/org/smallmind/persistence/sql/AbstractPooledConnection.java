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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.CommonDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;
import org.smallmind.nutsnbolts.lang.Existential;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

/**
 * Base {@link PooledConnection} implementation that wraps a real JDBC {@link Connection} with a
 * proxy to intercept close events, manage statement caching, and dispatch connection/statement
 * events to listeners.
 *
 * @param <D> the concrete {@link CommonDataSource} type that owns this connection
 */
public abstract class AbstractPooledConnection<D extends CommonDataSource> implements PooledConnection, InvocationHandler {

  private static final Method CLOSE_METHOD;

  private final PooledPreparedStatementCache statementCache;

  private final D dataSource;
  private final Connection actualConnection;
  private final Connection proxyConnection;
  private final ConcurrentLinkedQueue<ConnectionEventListener> connectionEventListenerQueue;
  private final ConcurrentLinkedQueue<StatementEventListener> statementEventListenerQueue;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final long creationMilliseconds;

  static {

    try {
      CLOSE_METHOD = Connection.class.getMethod("close");
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new StaticInitializationError(noSuchMethodException);
    }
  }

  /**
   * Wraps a physical JDBC connection with pooling behavior and optional prepared statement cache.
   *
   * @param dataSource       owning data source
   * @param actualConnection physical JDBC connection being wrapped
   * @param maxStatements    maximum number of prepared statements to cache (0 to disable)
   * @throws SQLException if the max statements value is negative
   */
  public AbstractPooledConnection (D dataSource, Connection actualConnection, int maxStatements)
    throws SQLException {

    this.dataSource = dataSource;
    this.actualConnection = actualConnection;

    if (maxStatements < 0) {
      throw new SQLException("The maximum number of cached statements for this connection must be >= 0");
    }

    creationMilliseconds = System.currentTimeMillis();
    proxyConnection = (Connection)Proxy.newProxyInstance(dataSource.getClass().getClassLoader(), new Class[] {Connection.class, Existential.class}, this);

    connectionEventListenerQueue = new ConcurrentLinkedQueue<>();
    statementEventListenerQueue = new ConcurrentLinkedQueue<>();

    if (maxStatements == 0) {
      statementCache = null;
    } else {
      addStatementEventListener(statementCache = new PooledPreparedStatementCache(maxStatements));
    }
  }

  /**
   * Creates a {@link ConnectionEvent} representing either a normal close or error condition for
   * this connection.
   *
   * @param sqlException optional SQL error (null for normal close)
   * @return constructed connection event
   */
  public abstract ConnectionEvent getConnectionEvent (SQLException sqlException);

  /**
   * Delegates JDBC calls to the underlying connection while intercepting close calls and wrapping
   * prepared statements in the cache if enabled. Errors trigger a {@link PooledConnectionException}
   * and fire {@link ConnectionEventListener#connectionErrorOccurred(ConnectionEvent)}.
   *
   * @param proxy  the proxy instance
   * @param method invoked method
   * @param args   arguments to the method
   * @return result of the underlying call or cached prepared statement
   * @throws Throwable propagated underlying exception wrapped when needed
   */
  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    if (CLOSE_METHOD.equals(method)) {

      ConnectionEvent event = getConnectionEvent(null);

      for (ConnectionEventListener listener : connectionEventListenerQueue) {
        listener.connectionClosed(event);
      }

      return null;
    } else {
      try {
        if ((statementCache != null) && PreparedStatement.class.isAssignableFrom(method.getReturnType())) {

          PreparedStatement preparedStatement;

          synchronized (statementCache) {
            if ((preparedStatement = statementCache.getPreparedStatement(args)) == null) {
              preparedStatement = statementCache.cachePreparedStatement(args, new PooledPreparedStatement(this, (PreparedStatement)method.invoke(actualConnection, args)));
            }
          }

          return preparedStatement;
        } else {

          return method.invoke(actualConnection, args);
        }
      } catch (Throwable throwable) {

        Throwable closestCause;

        closestCause = ((throwable instanceof InvocationTargetException) && (throwable.getCause() != null)) ? throwable.getCause() : throwable;

        if (closestCause instanceof SQLException) {

          ConnectionEvent event = getConnectionEvent((SQLException)closestCause);

          for (ConnectionEventListener listener : connectionEventListenerQueue) {
            listener.connectionErrorOccurred(event);
          }
        }

        throw new PooledConnectionException(closestCause, "Connection encountered an exception after operation for %d milliseconds", System.currentTimeMillis() - creationMilliseconds);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public PrintWriter getLogWriter ()
    throws SQLException {

    return dataSource.getLogWriter();
  }

  /**
   * Returns a proxy connection that defers {@code close()} and optionally caches prepared
   * statements.
   *
   * @return connection proxy exposed to clients
   */
  public Connection getConnection () {

    return proxyConnection;
  }

  /**
   * Closes the pooled connection once, cleaning up cached statements and the underlying connection.
   *
   * @throws SQLException if closing the real connection or statements fails
   */
  public void close ()
    throws SQLException {

    if (closed.compareAndSet(false, true)) {
      try {
        if (statementCache != null) {
          try {
            statementCache.close();
          } finally {
            removeStatementEventListener(statementCache);
          }
        }
      } finally {
        actualConnection.close();
      }
    }
  }

  /**
   * Registers a listener to receive connection lifecycle events.
   *
   * @param listener listener to add
   */
  @Override
  public void addConnectionEventListener (ConnectionEventListener listener) {

    connectionEventListenerQueue.add(listener);
  }

  /**
   * Removes the given connection event listener.
   *
   * @param listener listener to remove
   */
  @Override
  public void removeConnectionEventListener (ConnectionEventListener listener) {

    connectionEventListenerQueue.remove(listener);
  }

  /**
   * Provides access to the registered statement event listeners.
   *
   * @return iterable of statement listeners currently registered
   */
  protected Iterable<StatementEventListener> getStatementEventListeners () {

    return statementEventListenerQueue;
  }

  /**
   * Registers a statement event listener (often the statement cache).
   *
   * @param listener listener to add
   */
  public void addStatementEventListener (StatementEventListener listener) {

    statementEventListenerQueue.add(listener);
  }

  /**
   * Removes a previously added statement event listener.
   *
   * @param listener listener to remove
   */
  public void removeStatementEventListener (StatementEventListener listener) {

    statementEventListenerQueue.remove(listener);
  }
}
