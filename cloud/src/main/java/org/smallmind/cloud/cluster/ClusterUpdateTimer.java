package org.smallmind.cloud.cluster;

public class ClusterUpdateTimer implements Runnable {

   private Thread runnableThread = null;
   private ClusterHub clusterHub;
   private boolean finished = false;
   private boolean exited = false;
   private long pulseTime;

   public ClusterUpdateTimer (ClusterHub clusterHub, int updateInterval) {

      this.clusterHub = clusterHub;

      pulseTime = updateInterval * 1000;
   }

   public void finish () {

      finished = true;

      while (!exited) {
         runnableThread.interrupt();

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException i) {
         }
      }
   }

   public void run () {

      runnableThread = Thread.currentThread();

      while (!finished) {

         clusterHub.fireStatusUpdate(clusterHub.getClientClusterInstances());

         try {
            Thread.sleep(pulseTime);
         }
         catch (InterruptedException i) {
         }
      }

      exited = true;
   }

}
