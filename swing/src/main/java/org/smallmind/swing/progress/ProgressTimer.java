package org.smallmind.swing.progress;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ProgressTimer implements Runnable {

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private ProgressPanel progressPanel;
   private long pulseTime;

   public ProgressTimer (ProgressPanel progressPanel, long pulseTime) {

      this.progressPanel = progressPanel;
      this.pulseTime = pulseTime;

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
         progressPanel.setProgress();

         try {
            pulseLatch.await(pulseTime, TimeUnit.MILLISECONDS);
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ProgressTimer.class).error(interruptedException);
         }
      }

      exitLatch.countDown();
   }
}
