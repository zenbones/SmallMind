package org.smallmind.quorum.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool<C> {

   private ConnectionInstanceFactory<C> connectionFactory;
   private ConcurrentLinkedQueue<ConnectionPin<C>> freeConnectionPinQueue;
   private ConcurrentLinkedQueue<ConnectionPin<C>> processingConnectionPinQueue;
   private String poolName;
   private PoolMode poolMode = PoolMode.BLOCKING_POOL;
   private AtomicInteger poolCount = new AtomicInteger(0);
   private AtomicBoolean initializationFlag = new AtomicBoolean(false);
   private boolean testOnConnect = false;
   private boolean testOnAcquire = false;
   private long connectionTimeoutMillis = 0;
   private int initialPoolSize = 0;
   private int minPoolSize = 1;
   private int maxPoolSize = 10;
   private int acquireRetryAttempts = 0;
   private int acquireRetryDelayMillis = 0;
   private int leaseTimeSeconds = 0;
   private int maxIdleTimeSeconds = 0;
   private int unreturnedConnectionTimeoutSeconds = 0;

   public ConnectionPool (String poolName, ConnectionInstanceFactory<C> connectionFactory)
      throws ConnectionPoolException {

      this.poolName = poolName;
      this.connectionFactory = connectionFactory;

      ConnectionPoolManager.addConnectionPool(this);
   }

   public void initialize ()
      throws ConnectionPoolException {

      if (initializationFlag.compareAndSet(false, true)) {
         freeConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();
         processingConnectionPinQueue = new ConcurrentLinkedQueue<ConnectionPin<C>>();

         for (int count = 0; count < initialPoolSize; count++) {
            freeConnectionPinQueue.add(createConnectionPin());
         }
      }
   }

   public void setPoolMode (PoolMode poolMode) {

      this.poolMode = poolMode;
   }

   public void setTestOnConnect (boolean testOnConnect) {

      this.testOnConnect = testOnConnect;
   }

   public void setTestOnAcquire (boolean testOnAcquire) {

      this.testOnAcquire = testOnAcquire;
   }

   public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

      this.connectionTimeoutMillis = connectionTimeoutMillis;
   }

   public void setInitialPoolSize (int initialPoolSize) {

      this.initialPoolSize = initialPoolSize;
   }

   public void setMinPoolSize (int minPoolSize) {

      this.minPoolSize = minPoolSize;
   }

   public void setMaxPoolSize (int maxPoolSize) {

      this.maxPoolSize = maxPoolSize;
   }

   public void setAcquireRetryAttempts (int acquireRetryAttempts) {

      this.acquireRetryAttempts = acquireRetryAttempts;
   }

   public void setAcquireRetryDelayMillis (int acquireRetryDelayMillis) {

      this.acquireRetryDelayMillis = acquireRetryDelayMillis;
   }

   public void setLeaseTimeSeconds (int leaseTimeSeconds) {

      this.leaseTimeSeconds = leaseTimeSeconds;
   }

   public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

      this.maxIdleTimeSeconds = maxIdleTimeSeconds;
   }

   public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

      this.unreturnedConnectionTimeoutSeconds = unreturnedConnectionTimeoutSeconds;
   }

   public String getPoolName () {

      return poolName;
   }

   public Object rawConnection ()
      throws ConnectionCreationException {

      try {
         return connectionFactory.rawInstance();
      }
      catch (Exception exception) {
         throw new ConnectionCreationException(exception);
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
            throw new ConnectionPoolException(interruptedException);
         }

         if (connectionWorker.hasBeenAborted()) {
            throw new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionTimeoutMillis);
         }
         else if (connectionWorker.hasException()) {
            throw connectionWorker.getException();
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
            throw connectionPoolException;
         }
         catch (Exception otherException) {
            throw new ConnectionPoolException(otherException);
         }
      }

      if (testOnConnect && (!connectionInstance.validate())) {
         throw new InvalidConnectionException("A new connection was required by failed to validate");
      }
      else {
         poolCount.incrementAndGet();

         return new ConnectionPin<C>(this, connectionInstance, maxIdleTimeSeconds, leaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
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
      throws Exception {

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
                     finally {
                        processingConnectionPinQueue.add(connectionPin);
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
               finally {
                  processingConnectionPinQueue.add(connectionPin);
               }
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

      initialize();

      try {
         return useConnectionPin();
      }
      catch (Exception exception) {
         throw new ConnectionPoolException(exception);
      }
   }

   public void returnInstance (ConnectionInstance connectionInstance)
      throws Exception {

      releaseInstance(connectionInstance, false);
   }

   public void terminateInstance (ConnectionInstance connectionInstance)
      throws Exception {

      releaseInstance(connectionInstance, true);
   }

   private void releaseInstance (ConnectionInstance connectionInstance, boolean terminate)
      throws Exception {

      for (ConnectionPin<C> connectionPin : processingConnectionPinQueue) {
         if (connectionPin.contains(connectionInstance)) {
            if (processingConnectionPinQueue.remove(connectionPin)) {
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

      throw new ConnectionPoolException("Could not find connection (%s) within ConnectionPool (%s)", connectionInstance, poolName);
   }

   protected void removePin (ConnectionPin connectionPin) {

      if (processingConnectionPinQueue.remove(connectionPin) || freeConnectionPinQueue.remove(connectionPin)) {
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

   public int poolSize () {

      if (!initializationFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return poolCount.get();
   }

   public int freeSize () {

      if (!initializationFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return freeConnectionPinQueue.size();
   }

   public int processingSize () {

      if (!initializationFlag.get()) {
         throw new IllegalStateException("ConnectionPool has not yet been initialized");
      }

      return processingConnectionPinQueue.size();
   }
}