package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private SedaConfiguration sedaConfiguration;
   private EventQueue<I> eventQueue;
   private WorkMonitor monitor;
   private boolean stopped = false;

   public EventProcessor (EventQueue<I> eventQueue, DurationMonitor durationMonitor, SedaConfiguration sedaConfiguration) {

      this.eventQueue = eventQueue;

      monitor = new WorkMonitor(durationMonitor, sedaConfiguration.getWorkTrackingTime(), sedaConfiguration.getWorkTrackingTimeUnit());
      exitLatch = new CountDownLatch(1);
   }

   protected WorkMonitor getMonitor () {

      return monitor;
   }

   public double getIdlePercentage () {

      return monitor.getIdlePercentage();
   }

   public double getActivePercentage () {

      return monitor.getActivePercentage();
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
      StopWatch stopWatch = new StopWatch();

      try {
         stopWatch.click();
         while (!stopped) {
            if ((inputEvent = eventQueue.poll(sedaConfiguration.getQueuePollTimeout(), sedaConfiguration.getQueuePollTimeUnit())) != null) {
               monitor.addIdleTime(stopWatch.click());
               //TODO: HandleEvent
               monitor.addActiveTime(stopWatch.click());
            }
            else {
               monitor.addIdleTime(stopWatch.click());
            }
         }
      }
      catch (InterruptedException interruptedException) {
         stopped = true;
         LoggerManager.getLogger(EventProcessor.class).error(interruptedException);
      }

      exitLatch.countDown();
   }
}
