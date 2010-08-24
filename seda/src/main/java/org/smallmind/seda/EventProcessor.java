package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private StageController<I, O> stageController;
   private ProcessorHistory history;
   private boolean stopped = false;

   public EventProcessor (StageController<I, O> stageController, long trackingTime, TimeUnit trackingTimeUnit) {

      this.stageController = stageController;

      history = new ProcessorHistory(trackingTime, trackingTimeUnit);
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
      long startTime;

      try {
         while (!stopped) {
            startTime = System.currentTimeMillis();
            if ((inputEvent = stageController.pollQueue()) != null) {
               history.addIdleTime(startTime, startTime = System.currentTimeMillis());
               //TODO: HandleEvent
               history.addActiveTime(startTime, System.currentTimeMillis());
            }
            else {
               history.addIdleTime(startTime, System.currentTimeMillis());
            }
         }
      }
      catch (InterruptedException interruptedException) {
         stopped = true;
         LoggerManager.getLogger(EventProcessor.class).error(interruptedException);
      }

      stageController.decreasePool(this, true);
      exitLatch.countDown();
   }
}
