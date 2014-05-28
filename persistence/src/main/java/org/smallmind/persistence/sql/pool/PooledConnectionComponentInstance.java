/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.sql.pool;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.scribe.pen.LoggerManager;

public class PooledConnectionComponentInstance implements ComponentInstance<PooledConnection>, ConnectionEventListener {

  private final ComponentPool<PooledConnection> componentPool;
  private final PooledConnection pooledConnection;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private PreparedStatement validationStatement;

  public PooledConnectionComponentInstance (ComponentPool<PooledConnection> componentPool, PooledConnection pooledConnection)
    throws SQLException {

    this.componentPool = componentPool;
    this.pooledConnection = pooledConnection;

    pooledConnection.addConnectionEventListener(this);
  }

  public PooledConnectionComponentInstance (ComponentPool<PooledConnection> componentPool, PooledConnection pooledConnection, String validationQuery)
    throws SQLException {

    this(componentPool, pooledConnection);

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
      componentPool.returnInstance(this);
    }
    catch (Exception exception) {
      LoggerManager.getLogger(PooledConnectionComponentInstance.class).error(exception);
    }
  }

  public void connectionErrorOccurred (ConnectionEvent connectionEvent) {

    Exception reportedException = connectionEvent.getSQLException();

    try {
      if (reportedException != null) {
        componentPool.reportErrorOccurred(reportedException);
      }
    }
    catch (Exception exception) {
      LoggerManager.getLogger(PooledConnectionComponentInstance.class).error(exception);
    }
    finally {
      try {
        componentPool.terminateInstance(this);
      }
      catch (Exception exception) {
        if ((reportedException != null) && (exception.getCause() == exception)) {
          exception.initCause(reportedException);
        }

        LoggerManager.getLogger(PooledConnectionComponentInstance.class).error(exception);
      }
    }
  }

  public PooledConnection serve () {

    if (componentPool.getComplexPoolConfig().isExistentiallyAware()) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return pooledConnection;
  }

  public void close ()
    throws SQLException {

    if (closed.compareAndSet(false, true)) {

      SQLException validationCloseException = null;

      if (componentPool.getComplexPoolConfig().isExistentiallyAware()) {
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
          if ((validationCloseException != null) && (sqlException.getCause() != sqlException)) {
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