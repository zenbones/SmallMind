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
package org.smallmind.quorum.pool.connection;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.connection.event.ConnectionPoolEventListener;
import org.smallmind.quorum.pool.connection.event.ErrorReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.connection.event.LeaseTimeReportingConnectionPoolEvent;

public class ConnectionPool<C> {

  private final ConcurrentLinkedQueue<ConnectionPoolEventListener> connectionPoolEventListenerQueue = new ConcurrentLinkedQueue<ConnectionPoolEventListener>();
  private final ConnectionInstanceFactory<?, C> connectionInstanceFactory;
  private final ConnectionPinManager<C> connectionPinManager;
  private final String name;

  private ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();

  public ConnectionPool (String name, ConnectionInstanceFactory<?, C> connectionInstanceFactory) {

    this.name = name;
    this.connectionInstanceFactory = connectionInstanceFactory;

    connectionPinManager = new ConnectionPinManager<C>(this);
  }

  public ConnectionPool (String name, ConnectionInstanceFactory<?, C> connectionInstanceFactory, ConnectionPoolConfig connectionPoolConfig) {

    this(name, connectionInstanceFactory);

    this.connectionPoolConfig = connectionPoolConfig;
  }

  public String getPoolName () {

    return name;
  }

  public ConnectionInstanceFactory<?, C> getConnectionInstanceFactory () {

    return connectionInstanceFactory;
  }

  public ConnectionPoolConfig getConnectionPoolConfig () {

    return connectionPoolConfig;
  }

  public ConnectionPool<C> setConnectionPoolConfig (ConnectionPoolConfig connectionPoolConfig) {

    this.connectionPoolConfig = connectionPoolConfig;

    return this;
  }

  public StackTrace[] getExistentialStackTraces () {

    return connectionPinManager.getExistentialStackTraces();
  }

  public void addConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPoolEventListenerQueue.add(listener);
  }

  public void removeConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPoolEventListenerQueue.remove(listener);
  }

  public void reportConnectionErrorOccurred (Exception exception) {

    ErrorReportingConnectionPoolEvent poolEvent = new ErrorReportingConnectionPoolEvent<C>(this, exception);

    for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
      listener.reportConnectionErrorOccurred(poolEvent);
    }
  }

  public void reportConnectionLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingConnectionPoolEvent poolEvent = new LeaseTimeReportingConnectionPoolEvent<C>(this, leaseTimeNanos);

    for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
      listener.reportConnectionLeaseTime(poolEvent);
    }
  }

  public void startup ()
    throws ConnectionPoolException {

    try {
      connectionInstanceFactory.initialize();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }

    connectionPinManager.startup();

    try {
      connectionInstanceFactory.startup();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }
  }

  public void shutdown ()
    throws ConnectionPoolException {

    try {
      connectionInstanceFactory.shutdown();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }

    connectionPinManager.shutdown();

    try {
      connectionInstanceFactory.deconstruct();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }
  }

  public C getConnection ()
    throws ConnectionPoolException {

    try {

      return connectionPinManager.serve().serve();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }
  }

  public void returnInstance (ConnectionInstance<C> connectionInstance) {

    connectionPinManager.process(connectionInstance);
  }

  public void terminateInstance (ConnectionInstance<C> connectionInstance) {

    connectionPinManager.terminate(connectionInstance);
  }

  public void removePin (ConnectionPin<C> connectionPin, boolean withPrejudice) {

    connectionPinManager.remove(connectionPin, withPrejudice);
  }

  public int getPoolSize () {

    return connectionPinManager.getPoolSize();
  }

  public int getFreeSize () {

    return connectionPinManager.getFreeSize();
  }

  public int getProcessingSize () {

    return connectionPinManager.getProcessingSize();
  }
}
