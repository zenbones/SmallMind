package org.smallmind.cloud.cluster;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ClusterUpdateTimer implements Runnable {

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private ClusterHub clusterHub;
   private long pulseTime;

   public ClusterUpdateTimer (ClusterHub clusterHub, int updateInterval) {

      this.clusterHub = clusterHub;

      pulseTime = updateInterval * 1000;

      pulseLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         pulseLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      while (!finished.get()) {

         clusterHub.fireStatusUpdate(clusterHub.getClientClusterInstances());

         try {
            pulseLatch.await(pulseTime, TimeUnit.MILLISECONDS);
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ClusterUpdateTimer.class).error(interruptedException);
         }
      }

      exitLatch.countDown();
   }

}
