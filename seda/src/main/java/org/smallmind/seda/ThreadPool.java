package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ThreadPool<I extends Event, O extends Event> {

   private final LinkedList<EventProcessor<I, O>> processorList;

   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private SedaConfiguration sedaConfiguration;
   private EventQueue<I> eventQueue;
   private DurationMonitor durationMonitor;
   private HomeostaticRegulator<I, O> homeostaticRegulator;

   public ThreadPool (EventQueue<I> eventQueue, SedaConfiguration sedaConfiguration) {

      Thread regulatorThread;

      this.sedaConfiguration = sedaConfiguration;
      this.eventQueue = eventQueue;

      durationMonitor = new DurationMonitor(sedaConfiguration.getMaxTrackedInvocations());
      processorList = new LinkedList<EventProcessor<I, O>>();

      regulatorThread = new Thread(homeostaticRegulator = new HomeostaticRegulator<I, O>(this, durationMonitor, processorList, sedaConfiguration));
      regulatorThread.start();

      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected synchronized void increase () {

      synchronized (processorList) {
         if (!stopped.get()) {
            if ((sedaConfiguration.getMaxThreadPoolSize() == 0) || (processorList.size() < sedaConfiguration.getMaxThreadPoolSize())) {

               Thread processorThread;
               EventProcessor<I, O> eventProcessor;

               eventProcessor = new EventProcessor<I, O>(eventQueue, durationMonitor, sedaConfiguration);
               processorThread = new Thread(eventProcessor);
               processorThread.start();

               processorList.add(eventProcessor);
            }
         }
      }
   }

   protected void decrease () {

      synchronized (processorList) {
         if (!stopped.get()) {
            if ((sedaConfiguration.getMinThreadPoolSize() == 0) || (processorList.size() > sedaConfiguration.getMinThreadPoolSize())) {
               if (!processorList.isEmpty()) {
                  try {
                     processorList.removeFirst().stop();
                  }
                  catch (InterruptedException interruptedException) {
                     LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
                  }
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
