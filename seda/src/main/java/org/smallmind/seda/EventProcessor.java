package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private Stage<I, O> stage;
   private EventQueue<I> eventQueue;
   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private TimeUnit unit;
   private long timeout;

   public EventProcessor (Stage<I, O> stage, EventQueue<I> eventQueue, long timeout, TimeUnit unit) {

      this.stage = stage;
      this.eventQueue = eventQueue;
      this.timeout = timeout;
      this.unit = unit;

      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   public void stop ()
      throws InterruptedException {

      stopped.compareAndSet(false, true);
      exitLatch.await();
   }

   public void run () {

      I inputEvent;

      try {
         while (!stopped.get()) {
            if ((inputEvent = eventQueue.poll(timeout, unit)) != null) {

            }
         }
      }
      catch (InterruptedException interruptedException) {
         stopped.set(true);
         LoggerManager.getLogger(EventProcessor.class).error(interruptedException);
      }

      exitLatch.countDown();
   }
}
