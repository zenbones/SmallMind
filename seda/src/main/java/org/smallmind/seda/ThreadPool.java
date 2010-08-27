package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ThreadPool<I extends Event, O extends Event> {

   private final LinkedList<EventProcessor<I, O>> processorList;

   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private EventQueue<I> eventQueue;
   private DurationMonitor durationMonitor;
   private HomeostaticRegulator<I, O> homeostaticRegulator;
   private TimeUnit pollTimeUnit;
   private TimeUnit trackingTimeUnit;
   private long pollTimeout;
   private long trackingTime;
   private int minPoolSize;
   private int maxPoolSize;

   public ThreadPool (EventQueue<I> eventQueue, int minPoolSize, int maxPoolSize, long pollTimeout, TimeUnit pollTimeUnit, long trackingTime, TimeUnit trackingTimeUnit, int maxTracked, long monitorPulseTime, TimeUnit monitorPulseTimeUnit) {

      Thread regulatorThread;

      this.eventQueue = eventQueue;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;
      this.trackingTime = trackingTime;
      this.trackingTimeUnit = trackingTimeUnit;
      this.minPoolSize = minPoolSize;
      this.maxPoolSize = maxPoolSize;

      durationMonitor = new DurationMonitor(maxTracked);
      processorList = new LinkedList<EventProcessor<I, O>>();

      regulatorThread = new Thread(homeostaticRegulator = new HomeostaticRegulator<I, O>(this, durationMonitor, processorList, monitorPulseTime, monitorPulseTimeUnit));
      regulatorThread.start();

      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected synchronized void increase () {

      synchronized (processorList) {
         if (!stopped.get()) {
            if (processorList.size() < maxPoolSize) {

               Thread processorThread;
               EventProcessor<I, O> eventProcessor;

               eventProcessor = new EventProcessor<I, O>(eventQueue, durationMonitor, pollTimeout, pollTimeUnit, trackingTime, trackingTimeUnit);
               processorThread = new Thread(eventProcessor);
               processorThread.start();

               processorList.add(eventProcessor);
            }
         }
      }
   }

   protected void decrease (EventProcessor<I, O> eventProcessor) {

      synchronized (processorList) {
         if (!stopped.get()) {
            if (processorList.size() > minPoolSize) {
               processorList.remove(eventProcessor);

               try {
                  eventProcessor.stop();
               }
               catch (InterruptedException interruptedException) {
                  LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
               }
            }
         }
      }
   }

   protected void stop ()
      throws InterruptedException {

      if (stopped.compareAndSet(false, true)) {
         try {
            homeostaticRegulator.stop();
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
         }

         synchronized (processorList) {
            while (!processorList.isEmpty()) {
               try {
                  processorList.removeFirst().stop();
               }
               catch (InterruptedException interruptedException) {
                  LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
               }
            }
         }

         exitLatch.countDown();
      }
      else {
         exitLatch.await();
      }
   }
}
