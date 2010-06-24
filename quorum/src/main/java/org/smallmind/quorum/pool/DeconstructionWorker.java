package org.smallmind.quorum.pool;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeconstructionWorker implements java.lang.Runnable {

   private final ConnectionPin connectionPin;

   private ConnectionPool connectionPool;
   private CountDownLatch deconstructionLatch;
   private CountDownLatch exitLatch;
   private List<DeconstructionFuse> fuseList;
   private HashMap<DeconstructionFuse, Thread> fuseThreadMap;
   private AtomicBoolean terminated = new AtomicBoolean(false);
   private boolean forced = false;
   private boolean aborted = false;

   public DeconstructionWorker (ConnectionPool connectionPool, ConnectionPin connectionPin, List<DeconstructionFuse> fuseList) {

      Thread fuseThread;

      this.connectionPool = connectionPool;
      this.connectionPin = connectionPin;
      this.fuseList = fuseList;

      deconstructionLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);

      fuseThreadMap = new HashMap<DeconstructionFuse, Thread>();
      for (DeconstructionFuse deconstructionFuse : fuseList) {
         deconstructionFuse.setDeconstructionLatch(this);
         fuseThread = new Thread(deconstructionFuse);
         fuseThread.setDaemon(true);
         fuseThreadMap.put(deconstructionFuse, fuseThread);

         fuseThread.start();
      }
   }

   public void free () {

      for (DeconstructionFuse fuse : fuseList) {
         fuse.free();
      }
   }

   public void serve () {

      for (DeconstructionFuse fuse : fuseList) {
         fuse.serve();
      }
   }

   public void ignite (boolean forced) {

      if (terminated.compareAndSet(false, true)) {
         if (forced) {
            this.forced = true;
         }

         deconstructionLatch.countDown();
      }

      try {
         exitLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }
   }

   public void abort () {

      if (terminated.compareAndSet(false, true)) {
         aborted = true;
         deconstructionLatch.countDown();
      }

      try {
         exitLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }
   }

   public void run () {

      boolean deconstructed = false;

      try {
         deconstructionLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }

      for (DeconstructionFuse deconstructionFuse : fuseThreadMap.keySet()) {
         deconstructionFuse.abort();
      }

      if (!aborted) {
         synchronized (connectionPin) {
            connectionPin.decommission();
         }

         while (!deconstructed) {
            synchronized (connectionPin) {
               if (forced || connectionPin.isFree()) {
                  try {
                     connectionPin.close();
                  }
                  catch (Exception exception) {
                     ConnectionPoolManager.logError(exception);
                  }
               }

               if (connectionPin.isClosed()) {
                  deconstructed = true;
               }
            }

            if (deconstructed) {
               connectionPool.removePin(connectionPin);
            }
            else {
               try {
                  Thread.sleep(100);
               }
               catch (InterruptedException interruptedException) {
                  ConnectionPoolManager.logError(interruptedException);
               }
            }
         }
      }

      exitLatch.countDown();
   }
}
