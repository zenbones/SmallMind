/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.quorum.pool.event.ConnectionInstanceEvent;
import org.smallmind.quorum.pool.event.ConnectionInstanceEventListener;
import org.smallmind.quorum.pool.event.ConnectionPoolEventListener;
import org.smallmind.quorum.pool.event.ErrorReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.event.LeaseTimeReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolSurface;
import org.smallmind.quorum.transport.remote.RemoteEndpoint;

public class ConnectionPool<C> implements ConnectionInstanceEventListener, RemoteConnectionPoolSurface, RemoteEndpoint {

   private static final Class[] REMOTE_INTERFACES = new Class[] {RemoteConnectionPoolSurface.class};

   private ConnectionInstanceFactory<C> connectionFactory;
   private ConnectionPinManager<C> connectionPinManager;
   private ConcurrentLinkedQueue<ConnectionPoolEventListener> connectionPoolEventListenerQueue;
   private String poolName;
   private PoolMode poolMode = PoolMode.BLOCKING_POOL;
   private AtomicBoolean startupFlag = new AtomicBoolean(false);
   private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
   private boolean testOnConnect = false;
   private boolean testOnAcquire = false;
   private boolean reportLeaseTimeNanos = true;
   private boolean allowSoftMinSize = false;
   private long connectionTimeoutMillis = 0;
   private int initialPoolSize = 0;
   private int minPoolSize = 1;
   private int maxPoolSize = 10;
   private int acquireRetryAttempts = 0;
   private int acquireRetryDelayMillis = 0;
   private int maxLeaseTimeSeconds = 0;
   private int maxIdleTimeSeconds = 0;
   private int unreturnedConnectionTimeoutSeconds = 0;

   public ConnectionPool (String poolName, ConnectionInstanceFactory<C> connectionFactory) {

      this.poolName = poolName;
      this.connectionFactory = connectionFactory;

      connectionPoolEventListenerQueue = new ConcurrentLinkedQueue<ConnectionPoolEventListener>();

      ConnectionPoolManager.register(this);
   }

   public Class[] getProxyInterfaces () {

      return REMOTE_INTERFACES;
   }

   public synchronized void startup ()
      throws ConnectionPoolException {

      try {
         if (startupFlag.compareAndSet(false, true)) {
            connectionPinManager = new ConnectionPinManager<C>(this, connectionFactory, maxPoolSize, poolMode.equals(PoolMode.EXPANDING_POOL));

            for (int count = 0; count < initialPoolSize; count++) {
               connectionPinManager.initialize(createConnectionPin());
            }
         }
      }
      catch (ConnectionPoolException connectionPoolException) {
         throw connectionPoolException;
      }
      catch (Exception exception) {
         throw new ConnectionPoolException(exception);
      }
   }

   public synchronized void shutdown ()
      throws ConnectionPoolException {

      try {
         if (shutdownFlag.compareAndSet(false, true)) {
            connectionPinManager.shutdown();
         }
      }
      catch (ConnectionPoolException connectionPoolException) {
         throw connectionPoolException;
      }
      catch (Exception exception) {
         throw new ConnectionPoolException(exception);
      }
   }

   public String getPoolName () {

      return poolName;
   }

   public synchronized PoolMode getPoolMode () {

      return poolMode;
   }

   public synchronized void setPoolMode (PoolMode poolMode) {

      this.poolMode = poolMode;
   }

   public synchronized boolean isTestOnConnect () {

      return testOnConnect;
   }

   public synchronized void setTestOnConnect (boolean testOnConnect) {

      this.testOnConnect = testOnConnect;
   }

   public synchronized boolean isTestOnAcquire () {

      return testOnAcquire;
   }

   public synchronized void setTestOnAcquire (boolean testOnAcquire) {

      this.testOnAcquire = testOnAcquire;
   }

   public synchronized boolean isReportLeaseTimeNanos () {

      return reportLeaseTimeNanos;
   }

   public synchronized void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

      this.reportLeaseTimeNanos = reportLeaseTimeNanos;
   }

   public synchronized long getConnectionTimeoutMillis () {

      return connectionTimeoutMillis;
   }

   public synchronized void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

      this.connectionTimeoutMillis = connectionTimeoutMillis;
   }

   public synchronized int getInitialPoolSize () {

      return initialPoolSize;
   }

   public synchronized void setInitialPoolSize (int initialPoolSize) {

      if (startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has already been initialized");
      }

      this.initialPoolSize = initialPoolSize;
   }

   public synchronized int getMinPoolSize () {

      return minPoolSize;
   }

   public synchronized void setMinPoolSize (int minPoolSize) {

      this.minPoolSize = minPoolSize;
   }

   public boolean isAllowSoftMinSize () {

      return allowSoftMinSize;
   }

   public void setAllowSoftMinSize (boolean allowSoftMinSize) {

      this.allowSoftMinSize = allowSoftMinSize;
   }

   public synchronized int getMaxPoolSize () {

      return maxPoolSize;
   }

   public synchronized void setMaxPoolSize (int maxPoolSize) {

      this.maxPoolSize = maxPoolSize;
   }

   public synchronized int getAcquireRetryAttempts () {

      return acquireRetryAttempts;
   }

   public synchronized void setAcquireRetryAttempts (int acquireRetryAttempts) {

      this.acquireRetryAttempts = acquireRetryAttempts;
   }

   public synchronized int getAcquireRetryDelayMillis () {

      return acquireRetryDelayMillis;
   }

   public synchronized void setAcquireRetryDelayMillis (int acquireRetryDelayMillis) {

      this.acquireRetryDelayMillis = acquireRetryDelayMillis;
   }

   public synchronized int getMaxLeaseTimeSeconds () {

      return maxLeaseTimeSeconds;
   }

   public synchronized void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

      this.maxLeaseTimeSeconds = maxLeaseTimeSeconds;
   }

   public synchronized int getMaxIdleTimeSeconds () {

      return maxIdleTimeSeconds;
   }

   public synchronized void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

      this.maxIdleTimeSeconds = maxIdleTimeSeconds;
   }

   public synchronized int getUnreturnedConnectionTimeoutSeconds () {

      return unreturnedConnectionTimeoutSeconds;
   }

   public synchronized void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

      this.unreturnedConnectionTimeoutSeconds = unreturnedConnectionTimeoutSeconds;
   }

   public Object rawConnection ()
      throws ConnectionCreationException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      try {
         return connectionFactory.rawInstance();
      }
      catch (Exception exception) {
         throw new ConnectionCreationException(exception);
      }
   }

   public void addConnectionPoolEventListener (ConnectionPoolEventListener listener) {

      connectionPoolEventListenerQueue.add(listener);
   }

   public void removeConnectionPoolEventListener (ConnectionPoolEventListener listener) {

      connectionPoolEventListenerQueue.remove(listener);
   }

   public void connectionErrorOccurred (ConnectionInstanceEvent instanceEvent) {

      fireErrorReportingConnectionPoolEvent(instanceEvent.getException());
   }

   private ConnectionPoolException fireErrorReportingConnectionPoolEvent (Exception exception) {

      ErrorReportingConnectionPoolEvent poolEvent = new ErrorReportingConnectionPoolEvent(this, exception);

      for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
         listener.connectionErrorOccurred(poolEvent);
      }

      return (exception instanceof ConnectionPoolException) ? (ConnectionPoolException)exception : new ConnectionPoolException(exception);
   }

   public void reportConnectionLeaseTimeNanos (long leaseTimeNanos) {

      LeaseTimeReportingConnectionPoolEvent poolEvent = new LeaseTimeReportingConnectionPoolEvent(this, leaseTimeNanos);

      for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
         listener.connectionLeaseTime(poolEvent);
      }
   }

   protected ConnectionPin<C> createConnectionPin ()
      throws Exception {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      return connectionPinManager.create(connectionTimeoutMillis, testOnConnect, reportLeaseTimeNanos, maxIdleTimeSeconds, maxLeaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
   }

   private C useConnectionPin ()
      throws Exception {

      ConnectionPin<C> connectionPin;
      int blockedAttempts = 0;

      do {
         while ((connectionPin = connectionPinManager.serve()) != null) {
            synchronized (connectionPin) {
               if (connectionPin.isCommissioned() && connectionPin.isFree()) {
                  if (testOnAcquire && (!connectionPin.validate())) {
                     connectionPinManager.remove(connectionPin, true);
                  }
                  else {
                     return connectionPin.serve();
                  }
               }
               else {
                  connectionPinManager.remove(connectionPin, true);
               }
            }
         }

         if ((connectionPin = createConnectionPin()) != null) {
            synchronized (connectionPin) {

               return connectionPin.serve();
            }
         }

         if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            if ((acquireRetryAttempts == 0) || (++blockedAttempts < acquireRetryAttempts)) {
               Thread.sleep(acquireRetryDelayMillis);
            }
            else {
               throw new ConnectionPoolException("Blocking ConnectionPool (%s) has exceeded its maximum attempts (%d)", poolName, acquireRetryAttempts);
            }
         }
      } while (poolMode.equals(PoolMode.BLOCKING_POOL));

      throw new ConnectionPoolException("Fixed ConnectionPool (%s) is completely booked", poolName);
   }

   public C getConnection ()
      throws ConnectionPoolException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      try {
         startup();

         return useConnectionPin();
      }
      catch (Exception exception) {
         throw fireErrorReportingConnectionPoolEvent(exception);
      }
   }

   public void returnInstance (ConnectionInstance connectionInstance)
      throws ConnectionPoolException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      try {
         releaseInstance(connectionInstance, false);
      }
      catch (Exception exception) {
         throw fireErrorReportingConnectionPoolEvent(exception);
      }
   }

   public void terminateInstance (ConnectionInstance connectionInstance)
      throws ConnectionPoolException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      try {
         releaseInstance(connectionInstance, true);
      }
      catch (ConnectionPoolException connectionPoolException) {
         throw connectionPoolException;
      }
      catch (Exception exception) {
         throw new ConnectionPoolException(exception);
      }
   }

   private void releaseInstance (ConnectionInstance<C> connectionInstance, boolean terminate)
      throws Exception {

      connectionPinManager.release(connectionInstance, terminate);
   }

   protected void removePin (ConnectionPin<C> connectionPin)
      throws Exception {

      connectionPinManager.remove(connectionPin, !allowSoftMinSize);
   }

   public int getPoolSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return connectionPinManager.getPoolSize();
   }

   public int getFreeSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return connectionPinManager.getFreeSize();
   }

   public int getProcessingSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return connectionPinManager.getProcessingSize();
   }
}