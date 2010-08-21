package org.smallmind.swing.progress;

public class ProgressTimer implements Runnable {

   private Thread runnableThread = null;
   private ProgressPanel progressPanel;
   private boolean finished = false;
   private boolean exited = false;
   private long pulseTime;

   public ProgressTimer (ProgressPanel progressPanel, long pulseTime) {

      this.progressPanel = progressPanel;
      this.pulseTime = pulseTime;
   }

   public void finish () {

      finished = true;

      while (!exited) {
         runnableThread.interrupt();

         try {
            Thread.currentThread().sleep(100);
         }
         catch (InterruptedException i) {
         }
      }
   }

   public void run () {

      runnableThread = Thread.currentThread();

      while (!finished) {
         progressPanel.setProgress();

         try {
            runnableThread.sleep(pulseTime);
         }
         catch (InterruptedException i) {
         }
      }
      exited = true;
   }

}
