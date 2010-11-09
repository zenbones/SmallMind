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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
   private ConcurrentLinkedQueue<ConnectionPin<C>> freeConnectionPinQueue;
   private ConcurrentLinkedQueue<ConnectionPin<C>> processingConnectionPinQueue;
   private ConcurrentLinkedQueue<ConnectionPoolEventListener> connectionPoolEventListenerQueue;
   private String poolName;
   private PoolMode poolMode = PoolMode.BLOCKING_POOL;
   private AtomicInteger poolCount = new AtomicInteger(0);
   private AtomicInteger processingCount = new AtomicInteger(0);
   private AtomicBoolean startupFlag = new AtomicBoolean(false);
   private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
   private boolean testOnConnect = false;
   private boolean testOnAcquire = false;
   private boolean reportLeaseTimeNanos = true;
   private long connectionTimeoutMillis = 0;
   private int initialPoolSize = 0;
   private int minPoolSize = 1;
   private int maxPoolSize = 10;
   private int acquireRetryAttempts = 0;
   private int acquireRetryDelayMillis = 0;
   private int maxLeaseTimeSeconds = 0;
   private int maxIdleTimeSeconds = 0;
   private int unreturnedConnectionTimeoutSeconds = 0;

   public ConnectionPool (String poolName, ConnectionInstanceFactory<C> connectionFactory)
      throws ConnectionPoolException {

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

      if (startupFlag.compareAndSet(false, true)) {
         freeConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();
         processingConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();

         for (int count = 0; count < initialPoolSize; count++) {
            freeConnectionPinQueue.add(createConnectionPin());
         }
      }
   }

   public synchronized void shutdown () {

      if (shutdownFlag.compareAndSet(false, true)) {
         for (ConnectionPin<C> connectionPin : processingConnectionPinQueue) {
            destroyConnectionPin(connectionPin);
         }
         for (ConnectionPin<C> connectionPin : freeConnectionPinQueue) {
            destroyConnectionPin(connectionPin);
         }
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

      fireErrorReportingConnectionPoolEvent((instanceEvent.getException() instanceof ConnectionPoolException) ? (ConnectionCreationException)instanceEvent.getException() : new ConnectionCreationException(instanceEvent.getException()));
   }

   private ConnectionPoolException fireErrorReportingConnectionPoolEvent (ConnectionPoolException exception) {

      ErrorReportingConnectionPoolEvent poolEvent = new ErrorReportingConnectionPoolEvent(this, exception);

      for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
         listener.connectionErrorOccurred(poolEvent);
      }

      return exception;
   }

   public void reportConnectionLeaseTimeNanos (long leaseTimeNanos) {

      LeaseTimeReportingConnectionPoolEvent poolEvent = new LeaseTimeReportingConnectionPoolEvent(this, leaseTimeNanos);

      for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
         listener.connectionLeaseTime(poolEvent);
      }
   }

   private ConnectionPin<C> createConnectionPin ()
      throws ConnectionPoolException {

      ConnectionInstance<C> connectionInstance;

      if (connectionTimeoutMillis > 0) {

         ConnectionWorker<C> connectionWorker;
         Thread workerThread;
         CountDownLatch workerInitLatch = new CountDownLatch(1);

         connectionWorker = new ConnectionWorker<C>(this, connectionFactory, workerInitLatch);
         workerThread = new Thread(connectionWorker);
         workerThread.start();

         try {
            workerInitLatch.await();
            workerThread.join(connectionTimeoutMillis);
            connectionWorker.abort();
         }
         catch (InterruptedException interruptedException) {
            throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(interruptedException));
         }

         if (connectionWorker.hasBeenAborted()) {
            throw fireErrorReportingConnectionPoolEvent(new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionTimeoutMillis));
         }
         else if (connectionWorker.hasException()) {
            throw fireErrorReportingConnectionPoolEvent(connectionWorker.getException());
         }
         else {
            connectionInstance = connectionWorker.getConnectionInstance();
         }
      }
      else {
         try {
            connectionInstance = connectionFactory.createInstance(this);
         }
         catch (ConnectionPoolException connectionPoolException) {
            throw fireErrorReportingConnectionPoolEvent(connectionPoolException);
         }
         catch (Exception otherException) {
            throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(otherException));
         }
      }

      if (testOnConnect && (!connectionInstance.validate())) {
         throw fireErrorReportingConnectionPoolEvent(new InvalidConnectionException("A new connection was required, but failed to validate"));
      }
      else {
         poolCount.incrementAndGet();

         return new ConnectionPin<C>(this, connectionInstance, reportLeaseTimeNanos, maxIdleTimeSeconds, maxLeaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
      }
   }

   private void destroyConnectionPin (ConnectionPin<C> connectionPin) {

      poolCount.decrementAndGet();

      try {
         connectionPin.close();
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
      finally {
         connectionPin.abort();
      }
   }

   private C useConnectionPin ()
      throws ConnectionPoolException {

      ConnectionPin<C> connectionPin;
      int blockedAttempts = 0;

      do {
         while ((connectionPin = freeConnectionPinQueue.poll()) != null) {
            synchronized (connectionPin) {
               if (connectionPin.isComissioned() && connectionPin.isFree()) {
                  if (testOnAcquire && (!connectionPin.validate())) {
                     destroyConnectionPin(connectionPin);
                  }
                  else {
                     try {
                        return connectionPin.serve();
                     }
                     catch (ConnectionPoolException connectionPoolException) {
                        throw fireErrorReportingConnectionPoolEvent(connectionPoolException);
                     }
                     catch (Exception otherException) {
                        throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(otherException));
                     }
                     finally {
                        processingConnectionPinQueue.add(connectionPin);
                        processingCount.incrementAndGet();
                     }
                  }
               }
               else {
                  poolCount.decrementAndGet();
               }
            }
         }

         if (poolMode.equals(PoolMode.EXPANDING_POOL) || (poolCount.get() < maxPoolSize)) {
            synchronized (connectionPin = createConnectionPin()) {
               try {
                  return connectionPin.serve();
               }
               catch (ConnectionPoolException connectionPoolException) {
                  throw fireErrorReportingConnectionPoolEvent(connectionPoolException);
               }
               catch (Exception otherException) {
                  throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(otherException));
               }
               finally {
                  processingConnectionPinQueue.add(connectionPin);
                  processingCount.incrementAndGet();
               }
            }
         }

         if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            if ((acquireRetryAttempts == 0) || (++blockedAttempts < acquireRetryAttempts)) {
               try {
                  Thread.sleep(acquireRetryDelayMillis);
               }
               catch (InterruptedException interruptedException) {
                  throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(interruptedException));
               }
            }
            else {
               throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException("Blocking ConnectionPool (%s) has exceeded its maximum attempts (%d)", poolName, acquireRetryAttempts));
            }
         }
      } while (poolMode.equals(PoolMode.BLOCKING_POOL));

      throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException("Fixed ConnectionPool (%s) is completely booked", poolName));
   }

   public C getConnection ()
      throws ConnectionPoolException {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      startup();

      try {
         return useConnectionPin();
      }
      catch (Exception exception) {
         throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException(exception));
      }
   }

   public void returnInstance (ConnectionInstance connectionInstance)
      throws Exception {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      releaseInstance(connectionInstance, false);
   }

   public void terminateInstance (ConnectionInstance connectionInstance)
      throws Exception {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      releaseInstance(connectionInstance, true);
   }

   private void releaseInstance (ConnectionInstance connectionInstance, boolean terminate)
      throws ConnectionPoolException {

      for (ConnectionPin<C> connectionPin : processingConnectionPinQueue) {
         if (connectionPin.contains(connectionInstance)) {
            if (processingConnectionPinQueue.remove(connectionPin)) {
               processingCount.decrementAndGet();

               synchronized (connectionPin) {
                  if (terminate || (poolCount.get() > minPoolSize)) {
                     destroyConnectionPin(connectionPin);

                     if (poolCount.get() < minPoolSize) {
                        freeConnectionPinQueue.add(createConnectionPin());
                     }
                  }
                  else if (connectionPin.isServed()) {
                     connectionPin.free();
                     freeConnectionPinQueue.add(connectionPin);
                  }
                  else {
                     poolCount.decrementAndGet();
                  }
               }
            }

            return;
         }
      }

      throw fireErrorReportingConnectionPoolEvent(new ConnectionPoolException("Could not find connection (%s) within ConnectionPool (%s)", connectionInstance, poolName));
   }

   protected void removePin (ConnectionPin connectionPin) {

      if (processingConnectionPinQueue.remove(connectionPin)) {
         processingCount.decrementAndGet();
         poolCount.decrementAndGet();
      }
      else if (freeConnectionPinQueue.remove(connectionPin)) {
         poolCount.decrementAndGet();
      }

      if (poolCount.get() < minPoolSize) {
         try {
            freeConnectionPinQueue.add(createConnectionPin());
         }
         catch (ConnectionPoolException connectionPoolException) {
            ConnectionPoolManager.logError(connectionPoolException);
         }
      }
   }

   public int getPoolSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return poolCount.get();
   }

   public int getFreeSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return poolCount.get() - processingCount.get();
   }

   public int getProcessingSize () {

      if (shutdownFlag.get()) {
         throw new IllegalStateException("ConnectionPool has been shut down");
      }

      if (!startupFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return processingCount.get();
   }
}