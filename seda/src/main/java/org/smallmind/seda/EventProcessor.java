package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private StageController<I, O> stageController;
   private boolean stopped = false;
   private long maxIdleTimeMillis;
   private long lastRunTimeMillis;

   public EventProcessor (StageController<I, O> stageController, long maxIdleTime, TimeUnit maxIdleTimeUnit) {

      this.stageController = stageController;

      maxIdleTimeMillis = maxIdleTimeUnit.toMillis(maxIdleTime);
      lastRunTimeMillis = System.currentTimeMillis();
      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped;
   }

   protected void stop ()
      throws InterruptedException {

      stopped = true;
      exitLatch.await();
   }

   public void run () {

      I inputEvent;

      try {
         while (!stopped) {
            if ((inputEvent = stageController.poll()) != null) {

               lastRunTimeMillis = System.currentTimeMillis();
            }
            else if ((System.currentTimeMillis() - lastRunTimeMillis) > maxIdleTimeMillis) {
               stopped = true;
            }
         }
      }
      catch (InterruptedException interruptedException) {
         stopped = true;
         LoggerManager.getLogger(EventProcessor.class).error(interruptedException);
      }

      stageController.remove(this);
      exitLatch.countDown();
   }
}
