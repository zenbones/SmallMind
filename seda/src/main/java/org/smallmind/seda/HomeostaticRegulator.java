package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class HomeostaticRegulator<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private ThreadPool<I, O> threadPool;
   private TimeUnit monitorPulseTimeUnit;
   private long monitorPulseTime;

   public HomeostaticRegulator (ThreadPool<I, O> threadPool, long monitorPulseTime, TimeUnit monitorPulseTimeUnit) {

      this.threadPool = threadPool;
      this.monitorPulseTime = monitorPulseTime;
      this.monitorPulseTimeUnit = monitorPulseTimeUnit;

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
            pulseLatch.await(monitorPulseTime, monitorPulseTimeUnit);
         }
         catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(HomeostaticRegulator.class).error(interruptedException);
         }

         if (!stopped.get()) {

         }
      }

      exitLatch.countDown();
   }
}
