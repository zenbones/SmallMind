/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.sql.pool;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool.ConnectionInstance;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.scribe.pen.LoggerManager;

public class PooledConnectionInstance implements ConnectionInstance<PooledConnection>, ConnectionEventListener {

  private final ConnectionPool<PooledConnection> connectionPool;
  private final PooledConnection pooledConnection;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private PreparedStatement validationStatement;

  public PooledConnectionInstance (ConnectionPool<PooledConnection> connectionPool, PooledConnection pooledConnection)
    throws SQLException {

    this.connectionPool = connectionPool;
    this.pooledConnection = pooledConnection;

    pooledConnection.addConnectionEventListener(this);
  }

  public PooledConnectionInstance (ConnectionPool<PooledConnection> connectionPool, PooledConnection pooledConnection, String validationQuery)
    throws SQLException {

    this(connectionPool, pooledConnection);

    if ((validationQuery != null) && (validationQuery.length() > 0)) {
      validationStatement = pooledConnection.getConnection().prepareStatement(validationQuery);
    }
    else {
      validationStatement = null;
    }
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return stackTraceReference.get();
  }

  public boolean validate () {

    if (validationStatement != null) {
      try {
        validationStatement.execute();
      }
      catch (SQLException sqlException) {
        return false;
      }
    }

    return true;
  }

  public void connectionClosed (ConnectionEvent connectionEvent) {

    try {
      connectionPool.returnInstance(this);
    }
    catch (Exception exception) {
      LoggerManager.getLogger(PooledConnectionInstance.class).error(exception);
    }
  }

  public void connectionErrorOccurred (ConnectionEvent connectionEvent) {

    Exception reportedException = connectionEvent.getSQLException();

    try {
      if (reportedException != null) {
        connectionPool.reportConnectionErrorOccurred(reportedException);
      }
    }
    catch (Exception exception) {
      LoggerManager.getLogger(PooledConnectionInstance.class).error(exception);
    }
    finally {
      try {
        connectionPool.terminateInstance(this);
      }
      catch (Exception exception) {
        if (reportedException != null) {
          exception.initCause(reportedException);
        }

        reportedException = exception;
      }
      finally {
        if (reportedException != null) {
          LoggerManager.getLogger(PooledConnectionInstance.class).error(reportedException);
        }
      }
    }
  }

  public PooledConnection serve () {

    if (connectionPool.getConnectionPoolConfig().isExistentiallyAware()) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return pooledConnection;
  }

  public void close ()
    throws SQLException {

    if (closed.compareAndSet(false, true)) {

      SQLException validationCloseException = null;

      if (connectionPool.getConnectionPoolConfig().isExistentiallyAware()) {
        stackTraceReference.set(null);
      }

      if (validationStatement != null) {
        try {
          validationStatement.close();
        }
        catch (SQLException sqlException) {
          validationCloseException = sqlException;
        }
      }

      if (pooledConnection != null) {
        try {
          pooledConnection.close();
        }
        catch (SQLException sqlException) {

          if (validationCloseException != null) {
            sqlException.initCause(validationCloseException);
          }

          throw sqlException;
        }
        finally {
          pooledConnection.removeConnectionEventListener(this);
        }
      }

      if (validationCloseException != null) {
        throw validationCloseException;
      }
    }
  }
}