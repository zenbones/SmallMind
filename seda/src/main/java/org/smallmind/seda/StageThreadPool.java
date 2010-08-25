package org.smallmind.seda;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class StageThreadPool<I extends Event, O extends Event> {

   private final LinkedList<EventProcessor<I, O>> processorList;

   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private StageController<I, O> stageController;
   private int minPoolSize;
   private int maxPoolSize;

   public StageThreadPool (StageController<I, O> stageController, int minPoolSize, int maxPoolSize) {

      this.stageController = stageController;
      this.minPoolSize = minPoolSize;
      this.maxPoolSize = maxPoolSize;

      processorList = new LinkedList<EventProcessor<I, O>>();
      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected synchronized boolean increase () {

      if (stopped.get()) {

         return false;
      }

      synchronized (processorList) {
         if (processorList.size() >= maxPoolSize) {

            return false;
         }

         Thread processorThread;
         EventProcessor<I, O> eventProcessor;

         eventProcessor = stageController.createEventProcessor();
         processorThread = new Thread(eventProcessor);
         processorThread.start();

         processorList.add(eventProcessor);

         return true;
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
         synchronized (this) {

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
      }
      else {
         exitLatch.await();
      }
   }
}
