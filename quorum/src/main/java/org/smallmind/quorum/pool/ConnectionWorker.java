package org.smallmind.quorum.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionWorker<C> implements Runnable {

   private ConnectionPool<C> connectionPool;
   private ConnectionInstanceFactory<C> connectionFactory;
   private ConnectionInstance<C> connectionInstance;
   private ConnectionPoolException exception;
   private Thread runnableThread;
   private CountDownLatch exitLatch;
   private CountDownLatch workerInitLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private boolean aborted = false;

   public ConnectionWorker (ConnectionPool<C> connectionPool, ConnectionInstanceFactory<C> connectionFactory, CountDownLatch workerInitLatch) {

      this.connectionPool = connectionPool;
      this.connectionFactory = connectionFactory;
      this.workerInitLatch = workerInitLatch;

      exitLatch = new CountDownLatch(1);
   }

   public boolean hasBeenAborted () {

      return aborted;
   }

   public boolean hasException () {

      return exception != null;
   }

   public ConnectionPoolException getException () {

      return exception;
   }

   public ConnectionInstance<C> getConnectionInstance () {

      return connectionInstance;
   }

   public void abort ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         runnableThread.interrupt();
      }

      exitLatch.await();
   }

   public void run () {

      runnableThread = Thread.currentThread();
      workerInitLatch.countDown();

      try {
         connectionInstance = connectionFactory.createInstance(connectionPool);
      }
      catch (InterruptedException interruptedException) {
         aborted = true;
      }
      catch (ConnectionPoolException connectionPoolException) {
         exception = connectionPoolException;
      }
      catch (Exception otherException) {
         exception = new ConnectionPoolException(otherException);
      }

      exitLatch.countDown();
   }
}