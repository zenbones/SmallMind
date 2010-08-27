package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private EventQueue<I> eventQueue;
   private WorkMonitor monitor;
   private TimeUnit pollTimeUnit;
   private boolean stopped = false;
   private long pollTimeout;

   public EventProcessor (EventQueue<I> eventQueue, long pollTimeout, TimeUnit pollTimeUnit, long trackingTime, TimeUnit trackingTimeUnit, int maxTracked) {

      this.eventQueue = eventQueue;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;

      monitor = new WorkMonitor(trackingTime, trackingTimeUnit, maxTracked);
      exitLatch = new CountDownLatch(1);
   }

   protected WorkMonitor getMonitor () {

      return monitor;
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
            if ((inputEvent = eventQueue.poll(pollTimeout, pollTimeUnit)) != null) {
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
