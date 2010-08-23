package org.smallmind.seda;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StageThreadPool<I extends Event, O extends Event> {

   private CountDownLatch exitLatch;
   private AtomicBoolean stopped = new AtomicBoolean(false);
   private ReentrantReadWriteLock shutdownLock;
   private ConcurrentLinkedQueue<EventProcessor<I, O>> processorQueue;
   private StageController<I, O> stageController;

   public StageThreadPool (StageController<I, O> stageController) {

      this.stageController = stageController;

      shutdownLock = new ReentrantReadWriteLock();
      processorQueue = new ConcurrentLinkedQueue<EventProcessor<I, O>>();
      exitLatch = new CountDownLatch(1);
   }

   public boolean isRunning () {

      return !stopped.get();
   }

   protected boolean increase () {

      shutdownLock.readLock().lock();
      try {
         if (stopped.get()) {

            return false;
         }

         Thread processorThread;
         EventProcessor<I, O> eventProcessor;

         eventProcessor = stageController.createEventProcessor();
         processorThread = new Thread(eventProcessor);
         processorThread.start();

         return processorQueue.add(eventProcessor);
      }
      finally {
         shutdownLock.readLock().unlock();
      }
   }

   protected void remove (EventProcessor<I, O> eventProcessor) {

      processorQueue.remove(eventProcessor);
   }

   protected void stop ()
      throws InterruptedException {

      if (stopped.compareAndSet(false, true)) {
         shutdownLock.writeLock().lock();
         try {

            EventProcessor<I, O> eventProcessor;

            while ((eventProcessor = processorQueue.poll()) != null) {
               eventProcessor.stop();
            }

            exitLatch.countDown();
         }
         finally {
            shutdownLock.writeLock().unlock();
         }
      }
      else {
         exitLatch.await();
      }
   }
}
