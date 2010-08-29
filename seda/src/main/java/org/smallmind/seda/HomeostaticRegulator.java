package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class HomeostaticRegulator<I extends Event, O extends Event> implements Runnable {

   private final LinkedList<EventProcessor<I, O>> processorList;

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private SedaConfiguration sedaConfiguration;
   private ThreadPool<I, O> threadPool;

   public HomeostaticRegulator (ThreadPool<I, O> threadPool, DurationMonitor durationMonitor, LinkedList<EventProcessor<I, O>> processorList, SedaConfiguration sedaConfiguration) {

      this.threadPool = threadPool;
      this.processorList = processorList;
      this.sedaConfiguration = sedaConfiguration;

      exitLatch = new CountDownLatch(1);
      pulseLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected void stop ()
      throws InterruptedException {

      if (stopped.compareAndSet(false, true)) {
         pulseLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      while (!stopped.get()) {
         try {
            pulseLatch.await(sedaConfiguration.getRegulatorPulseTime(), sedaConfiguration.getRegulatorPulseTimeUnit());
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(HomeostaticRegulator.class).error(interruptedException);
         }

         if (!stopped.get()) {

            double idlePercentage = 0;
            double activePercentage = 0;

            synchronized (processorList) {
               for (EventProcessor<I, O> eventProcessor : processorList) {
                  idlePercentage += eventProcessor.getIdlePercentage();
                  activePercentage += eventProcessor.getActivePercentage();
               }

               idlePercentage /= processorList.size();
               activePercentage /= processorList.size();
            }
            if (sedaConfiguration.getActiveUpShiftPercentage() >= activePercentage) {
               threadPool.increase();
            }
            if (sedaConfiguration.getInactiveDownShiftPercentage() >= idlePercentage) {
               threadPool.decrease();
            }
         }
      }

      exitLatch.countDown();
   }
}
