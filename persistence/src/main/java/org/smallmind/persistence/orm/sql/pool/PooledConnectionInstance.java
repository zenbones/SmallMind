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
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import org.smallmind.nutsnbolts.lang.Existential;
import org.smallmind.quorum.pool.AbstractConnectionInstance;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolManager;

public class PooledConnectionInstance extends AbstractConnectionInstance<PooledConnection> implements ConnectionEventListener, Existential {

  private ConnectionPool connectionPool;
  private PooledConnection pooledConnection;
  private PreparedStatement validationStatement;

  public PooledConnectionInstance (ConnectionPool connectionPool, Integer originatingIndex, PooledConnection pooledConnection)
    throws SQLException {

    this(connectionPool, originatingIndex, pooledConnection, null);
  }

  public PooledConnectionInstance (ConnectionPool connectionPool, Integer originatingIndex, PooledConnection pooledConnection, String validationQuery)
    throws SQLException {

    super(originatingIndex);

    this.connectionPool = connectionPool;
    this.pooledConnection = pooledConnection;

    if ((validationQuery != null) && (validationQuery.length() > 0)) {
      validationStatement = pooledConnection.getConnection().prepareStatement(validationQuery);
    }

    pooledConnection.addConnectionEventListener(this);
  }

  public StackTraceElement[] getExistentialStackTrace () {

    return ((Existential)pooledConnection).getExistentialStackTrace();
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
      ConnectionPoolManager.logError(exception);
    }
  }

  public void connectionErrorOccurred (ConnectionEvent connectionEvent) {

    Exception reportedException = connectionEvent.getSQLException();

    try {
      if (reportedException != null) {
        fireConnectionErrorOccurred(reportedException);
      }
    }
    catch (Exception exception) {
      ConnectionPoolManager.logError(exception);
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
          ConnectionPoolManager.logError(reportedException);
        }
      }
    }
  }

  public PooledConnection serve () {

    return pooledConnection;
  }

  public void close ()
    throws SQLException {

    SQLException validationCloseException = null;

    if (validationStatement != null) {
      try {
        validationStatement.close();
      }
      catch (SQLException sqlException) {
        validationCloseException = sqlException;
      }
    }

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

    if (validationCloseException != null) {
      throw validationCloseException;
    }
  }
}