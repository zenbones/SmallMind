package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadPool<I extends Event, O extends Event> {

   private final LinkedList<EventProcessor<I, O>> processorList;

   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private EventQueue<I> eventQueue;
   private TimeUnit pollTimeUnit;
   private TimeUnit trackingTimeUnit;
   private long pollTimeout;
   private long trackingTime;
   private int maxTracked;
   private int minPoolSize;
   private int maxPoolSize;

   public ThreadPool (EventQueue<I> eventQueue, int minPoolSize, int maxPoolSize, long pollTimeout, TimeUnit pollTimeUnit, long trackingTime, TimeUnit trackingTimeUnit, int maxTracked, long monitorPulseTime, TimeUnit monitorPulseTimeUnit) {

      this.eventQueue = eventQueue;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;
      this.trackingTime = trackingTime;
      this.trackingTimeUnit = trackingTimeUnit;
      this.maxTracked = maxTracked;
      this.minPoolSize = minPoolSize;
      this.maxPoolSize = maxPoolSize;

      processorList = new LinkedList<EventProcessor<I, O>>();
      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected synchronized void increase () {

      if (!stopped.get()) {
         synchronized (processorList) {
            if (processorList.size() < maxPoolSize) {

               Thread processorThread;
               EventProcessor<I, O> eventProcessor;

               eventProcessor = new EventProcessor<I, O>(eventQueue, pollTimeout, pollTimeUnit, trackingTime, trackingTimeUnit, maxTracked);
               processorThread = new Thread(eventProcessor);
               processorThread.start();

               processorList.add(eventProcessor);
            }
         }
      }
   }

   protected void decrease (EventProcessor<I, O> eventProcessor) {

      synchronized (processorList) {
         processorList.remove(eventProcessor);
      }
   }

   protected void stop ()
      throws InterruptedException {

      if (stopped.compareAndSet(false, true)) {

         EventProcessor<I, O> eventProcessor;

         do {
            synchronized (processorList) {
               eventProcessor = processorList.peek();
            }

            if (eventProcessor != null) {
               eventProcessor.stop();
            }
         } while (eventProcessor != null);

         exitLatch.countDown();
      }
      else {
         exitLatch.await();
      }
   }
}
