/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.quorum.pool.connection.remote;

import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;
import org.smallmind.quorum.pool.connection.event.ConnectionPoolEventListener;
import org.smallmind.quorum.transport.remote.RemoteEndpoint;

public class RemoteConnectionPoolSurfaceImpl implements RemoteConnectionPoolSurface, RemoteEndpoint {

  private static final Class[] REMOTE_INTERFACES = new Class[] {RemoteConnectionPoolSurface.class};

  private final ConnectionPool<?> connectionPool;

  public RemoteConnectionPoolSurfaceImpl (ConnectionPool<?> connectionPool) {

    this.connectionPool = connectionPool;
  }

  public Class[] getProxyInterfaces () {

    return REMOTE_INTERFACES;
  }

  @Override
  public void addConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPool.addConnectionPoolEventListener(listener);
  }

  @Override
  public void removeConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPool.removeConnectionPoolEventListener(listener);
  }

  @Override
  public String getPoolName () {

    return connectionPool.getPoolName();
  }

  @Override
  public void startup ()
    throws ConnectionPoolException {

    connectionPool.startup();
  }

  @Override
  public void shutdown ()
    throws ConnectionPoolException {

    connectionPool.shutdown();
  }

  @Override
  public int getPoolSize () {

    return connectionPool.getPoolSize();
  }

  @Override
  public int getFreeSize () {

    return connectionPool.getFreeSize();
  }

  @Override
  public int getProcessingSize () {

    return connectionPool.getProcessingSize();
  }

  @Override
  public boolean isTestOnConnect () {

    return connectionPool.getConnectionPoolConfig().isTestOnConnect();
  }

  @Override
  public void setTestOnConnect (boolean testOnConnect) {

    connectionPool.getConnectionPoolConfig().setTestOnConnect(testOnConnect);
  }

  @Override
  public boolean isTestOnAcquire () {

    return connectionPool.getConnectionPoolConfig().isTestOnAcquire();
  }

  @Override
  public void setTestOnAcquire (boolean testOnAcquire) {

    connectionPool.getConnectionPoolConfig().setTestOnAcquire(testOnAcquire);
  }

  @Override
  public boolean isReportLeaseTimeNanos () {

    return connectionPool.getConnectionPoolConfig().isReportLeaseTimeNanos();
  }

  @Override
  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    connectionPool.getConnectionPoolConfig().setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  @Override
  public boolean isExistentiallyAware () {

    return connectionPool.getConnectionPoolConfig().isExistentiallyAware();
  }

  @Override
  public void setExistentiallyAware (boolean existentiallyAware) {

    connectionPool.getConnectionPoolConfig().setExistentiallyAware(existentiallyAware);
  }

  @Override
  public long getConnectionTimeoutMillis () {

    return connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis();
  }

  @Override
  public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    connectionPool.getConnectionPoolConfig().setConnectionTimeoutMillis(connectionTimeoutMillis);
  }

  @Override
  public long getAcquireWaitTimeMillis () {

    return connectionPool.getConnectionPoolConfig().getAcquireWaitTimeMillis();
  }

  @Override
  public void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    connectionPool.getConnectionPoolConfig().setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  @Override
  public int getInitialPoolSize () {

    return connectionPool.getConnectionPoolConfig().getInitialPoolSize();
  }

  @Override
  public int getMinPoolSize () {

    return connectionPool.getConnectionPoolConfig().getMinPoolSize();
  }

  @Override
  public void setMinPoolSize (int minPoolSize) {

    connectionPool.getConnectionPoolConfig().setMinPoolSize(minPoolSize);
  }

  @Override
  public int getMaxPoolSize () {

    return connectionPool.getConnectionPoolConfig().getMaxPoolSize();
  }

  @Override
  public void setMaxPoolSize (int maxPoolSize) {

    connectionPool.getConnectionPoolConfig().setMaxPoolSize(maxPoolSize);
  }

  @Override
  public int getMaxLeaseTimeSeconds () {

    return connectionPool.getConnectionPoolConfig().getMaxLeaseTimeSeconds();
  }

  @Override
  public void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    connectionPool.getConnectionPoolConfig().setMaxLeaseTimeSeconds(maxLeaseTimeSeconds);
  }

  @Override
  public int getMaxIdleTimeSeconds () {

    return connectionPool.getConnectionPoolConfig().getMaxIdleTimeSeconds();
  }

  @Override
  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    connectionPool.getConnectionPoolConfig().setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  @Override
  public int getUnreturnedConnectionTimeoutSeconds () {

    return connectionPool.getConnectionPoolConfig().getUnreturnedConnectionTimeoutSeconds();
  }

  @Override
  public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

    connectionPool.getConnectionPoolConfig().setUnreturnedConnectionTimeoutSeconds(unreturnedConnectionTimeoutSeconds);
  }
}
