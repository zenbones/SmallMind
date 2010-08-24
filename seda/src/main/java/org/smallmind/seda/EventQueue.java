package org.smallmind.seda;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventQueue<I extends Event, O extends Event> {

   private StageController<I, O> stageController;
   private LinkedBlockingQueue<I> internalQueue;
   double expansionFactor;
   int maxQueueCapacity;

   public EventQueue (StageController<I, O> stageController, int maxQueueCapacity, double expansionFactor) {

      this.stageController = stageController;
      this.maxQueueCapacity = maxQueueCapacity;
      this.expansionFactor = expansionFactor;

      internalQueue = new LinkedBlockingQueue<I>(maxQueueCapacity);
   }

   protected void put (I event)
      throws InterruptedException {

      internalQueue.put(event);
      //TODO: work this out for real
      if (internalQueue.size() > (maxQueueCapacity * expansionFactor)) {
         stageController.increasePool();
      }
   }

   protected I poll (long timeout, TimeUnit unit)
      throws InterruptedException {

      return internalQueue.poll(timeout, unit);
   }
}
